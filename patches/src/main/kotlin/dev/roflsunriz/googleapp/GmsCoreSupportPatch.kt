package dev.roflsunriz.googleapp

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instructionsOrNull
import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.firstMethod
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ThreeRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val GMS_COMPAT = "Lapp/revanced/extension/googleapp/GmsCoreCompat;"
private const val FIREBASE_MESSAGING = "Lcom/google/firebase/messaging/FirebaseMessaging;"
private const val FIREBASE_NOT_INITIALIZED_ERROR =
    "Default FirebaseApp is not initialized in this process "
private const val FIREBASE_NEW_TOKEN_LOG = "Invoking onNewToken for app: "
private const val PLAY_SERVICES_CRONET_PROVIDER =
    "Lcom/google/android/gms/net/PlayServicesCronetProvider;"

private val processExitMethods = setOf(
    "Landroid/os/Process;->killProcess(I)V",
    "Ljava/lang/System;->exit(I)V",
)

private val unsupportedPhenotypeFeatures = setOf(
    "get_storage_info_api",
    "set_runtime_properties_api",
)

private const val SYSTEM_EXIT_RETURNED_ERROR =
    "System.exit returned normally, while it was supposed to halt JVM."

private const val SPATULA_HEADER_ERROR = "Could not retrieve spatula header"
private const val GOOGLE_CERTIFICATES_ERROR = "Failed to get Google certificates from remote"
private const val PIXEL_LAUNCHER_QSB_PERMISSION =
    "com.google.android.apps.nexuslauncher.permission.QSB"

internal val gmsCoreResourcesPatch = resourcePatch {
    compatibleWith(ORIGINAL_PACKAGE)
    dependsOn(googleAppResourcesPatch)

    apply {
        document("AndroidManifest.xml").use(::configureGmsCoreManifest)
    }
}

@Suppress("unused")
val gmsCoreSupportPatch = bytecodePatch(
    name = "GmsCore support",
    description = "標準Googleアプリを無効化し、ReVanced GmsCoreを使う別パッケージとして非root端末へ導入します。",
) {
    compatibleWith(ORIGINAL_PACKAGE)
    dependsOn(gmsCoreResourcesPatch, googleAppReVancedPatch)

    apply {
        patchCloudMessagingTokenNotification()

        val playServicesCronetProvider = classDefs.firstOrNull {
            it.type == PLAY_SERVICES_CRONET_PROVIDER
        } ?: throw PatchException("Google Play services Cronet provider was not found")
        val isPlayServicesCronetEnabled = playServicesCronetProvider.methods.singleOrNull { method ->
            method.name == "isEnabled" && method.parameterTypes.isEmpty() && method.returnType == "Z"
        } ?: throw PatchException("Google Play services Cronet availability method was not found")
        firstMethod(isPlayServicesCronetEnabled).addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """.trimIndent(),
        )

        val processSelector = classDefs.asSequence()
            .flatMap { it.methods.asSequence() }
            .singleOrNull { method ->
                method.containsString("com.google.android.googlequicksearchbox:googleapp") &&
                    method.containsString("com.google.android.googlequicksearchbox:ar_runtime_loader")
            } ?: throw PatchException("Google app Dagger process selector was not found")
        val mutableProcessSelector = firstMethod(processSelector)
        val processNameIndex = mutableProcessSelector.instructionsOrNull
            ?.indexOfFirst { instruction ->
                val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
                    ?: return@indexOfFirst false
                reference.returnType == "Ljava/lang/String;" && reference.parameterTypes.isEmpty()
            }
            ?.takeIf { it >= 0 }
            ?: throw PatchException("Google app Dagger process name lookup was not found")
        mutableProcessSelector.replaceInstruction(
            processNameIndex,
            "invoke-static {}, $GMS_COMPAT->getProcessName()Ljava/lang/String;",
        )

        transformInstructions(
            match = { classDef, _, instruction, index ->
                if (classDef.type.startsWith("Lapp/revanced/extension/googleapp/")) {
                    return@transformInstructions null
                }
                val value = ((instruction as? ReferenceInstruction)?.reference as? StringReference)?.string
                    ?: return@transformInstructions null
                val replacement = transformGmsString(value) ?: return@transformInstructions null
                val register = (instruction as? OneRegisterInstruction)?.registerA
                    ?: return@transformInstructions null
                Triple(index, register, replacement)
            },
            transform = { method, (index, register, replacement) ->
                method.replaceInstruction(
                    index,
                    "const-string v$register, \"${escapeSmaliString(replacement)}\"",
                )
            },
        )

        patchApiClientHeaders()

        val processExitMatches = classDefs.asSequence()
            .flatMap { it.methods.asSequence() }
            .mapNotNull { method ->
                val exitIndexes = method.instructionsOrNull
                    ?.toList()
                    .orEmpty()
                    .mapIndexedNotNull { index, instruction ->
                        val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
                            ?: return@mapIndexedNotNull null
                        index.takeIf { reference.toString() in processExitMethods }
                    }
                exitIndexes.takeIf { it.isNotEmpty() }?.let { method to it }
            }
            .toList()
        processExitMatches.forEach { (method, exitIndexes) ->
            val mutableMethod = firstMethod(method)
            exitIndexes.forEach { index -> mutableMethod.replaceInstruction(index, "nop") }
        }
        val disabledProcessExits = processExitMatches.sumOf { (_, exitIndexes) -> exitIndexes.size }
        if (disabledProcessExits == 0) {
            throw PatchException("Google app process reapers were not found")
        }

        val systemExitWrappers = findMethodsContaining(SYSTEM_EXIT_RETURNED_ERROR)
            .filter { method -> method.returnType == "V" }
        if (systemExitWrappers.size < 2) {
            throw PatchException("Google app System.exit wrappers were not found")
        }
        systemExitWrappers.forEach { firstMethod(it).addInstructions(0, "return-void") }

        val unsupportedFeatureFields = classDefs.asSequence()
            .flatMap { it.methods.asSequence() }
            .flatMap { method ->
                val instructions = method.instructionsOrNull?.toList().orEmpty()
                instructions.mapIndexedNotNull { index, instruction ->
                    val featureName = ((instruction as? ReferenceInstruction)?.reference as? StringReference)?.string
                        ?.takeIf { it in unsupportedPhenotypeFeatures }
                        ?: return@mapIndexedNotNull null
                    instructions.drop(index + 1).take(16).firstNotNullOfOrNull { candidate ->
                        if (candidate.opcode != Opcode.SPUT_OBJECT) return@firstNotNullOfOrNull null
                        (candidate as? ReferenceInstruction)?.reference as? FieldReference
                    } ?: throw PatchException("Phenotype feature field was not found for $featureName")
                }.asSequence()
            }
            .map(FieldReference::toString)
            .toSet()
        if (unsupportedFeatureFields.size != unsupportedPhenotypeFeatures.size) {
            throw PatchException("Phenotype feature fields were not found")
        }

        val featureRequirementMatches = classDefs.asSequence()
            .flatMap { it.methods.asSequence() }
            .mapNotNull { method ->
                val instructions = method.instructionsOrNull?.toList().orEmpty()
                val replacements = instructions.mapIndexedNotNull { index, instruction ->
                    if (instruction.opcode != Opcode.SGET_OBJECT) return@mapIndexedNotNull null
                    val field = (instruction as? ReferenceInstruction)?.reference as? FieldReference
                        ?: return@mapIndexedNotNull null
                    val fieldIndex = index.takeIf { field.toString() in unsupportedFeatureFields }
                        ?: return@mapIndexedNotNull null
                    val aputIndex = ((fieldIndex + 1)..minOf(fieldIndex + 6, instructions.lastIndex))
                        .firstOrNull { instructions[it].opcode == Opcode.APUT_OBJECT }
                        ?: throw PatchException("Phenotype feature requirement array was not found")
                    val arrayRegister = (instructions[aputIndex] as ThreeRegisterInstruction).registerB
                    aputIndex to arrayRegister
                }
                replacements.takeIf { it.isNotEmpty() }?.let { method to it }
            }
            .toList()
        featureRequirementMatches.forEach { (method, replacements) ->
            val mutableMethod = firstMethod(method)
            replacements.forEach { (aputIndex, arrayRegister) ->
                mutableMethod.replaceInstruction(aputIndex, "const/4 v$arrayRegister, 0x0")
            }
        }
        val removedFeatureRequirements = featureRequirementMatches.sumOf { (_, replacements) -> replacements.size }
        if (removedFeatureRequirements < unsupportedPhenotypeFeatures.size) {
            throw PatchException("Phenotype feature requirements were not removed")
        }

        val serviceChecks = findMethodsContaining("Google Play Services not available")
            .filter { it.returnType == "V" && it.parameterTypes.map { type -> type.toString() } == listOf(
                "Landroid/content/Context;",
                "I",
            ) }
        if (serviceChecks.isEmpty()) {
            throw PatchException("Google Play Services availability check was not found")
        }
        serviceChecks.forEach { firstMethod(it).addInstructions(0, "return-void") }

        val googleCertificateClasses = findMethodsContaining(GOOGLE_CERTIFICATES_ERROR)
            .map { method -> method.definingClass }
            .toSet()
        val pixelLauncherTrustCandidates = findMethodsContaining(PIXEL_LAUNCHER_QSB_PERMISSION)
        val pixelLauncherTrustMatches = pixelLauncherTrustCandidates.mapNotNull { method ->
            val instructions = method.instructionsOrNull?.toList().orEmpty()
            val permissionIndex = instructions.indexOfFirst { instruction ->
                ((instruction as? ReferenceInstruction)?.reference as? StringReference)?.string ==
                    PIXEL_LAUNCHER_QSB_PERMISSION
            }.takeIf { it >= 0 } ?: return@mapNotNull null
            val certificateCheckIndex = ((permissionIndex + 1)..minOf(permissionIndex + 96, instructions.lastIndex))
                .firstOrNull { index ->
                    val instruction = instructions[index]
                    val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
                        ?: return@firstOrNull false
                    instruction.opcode in setOf(Opcode.INVOKE_VIRTUAL, Opcode.INVOKE_VIRTUAL_RANGE) &&
                        reference.definingClass in googleCertificateClasses &&
                        reference.parameterTypes.map { it.toString() } == listOf("Ljava/lang/String;") &&
                        reference.returnType == "Z" &&
                        instructions.getOrNull(index + 1)?.opcode == Opcode.MOVE_RESULT
                } ?: return@mapNotNull null
            val resultRegister = (instructions[certificateCheckIndex + 1] as? OneRegisterInstruction)?.registerA
                ?: return@mapNotNull null

            Triple(method, certificateCheckIndex, resultRegister)
        }
        if (pixelLauncherTrustMatches.isEmpty()) {
            throw PatchException(
                "Pixel Launcher certificate check was not found " +
                    "(${pixelLauncherTrustCandidates.size} candidates)",
            )
        }
        pixelLauncherTrustMatches.forEach { (method, certificateCheckIndex, resultRegister) ->
            val mutableMethod = firstMethod(method)

            // The package is already required to be a system app declaring the Pixel Launcher QSB permission.
            // Avoid the Dynamite GoogleCertificates module, which cannot initialize under ReVanced GmsCore.
            mutableMethod.replaceInstruction(certificateCheckIndex, "const/16 v$resultRegister, 0x1")
            mutableMethod.replaceInstruction(certificateCheckIndex + 1, "nop")
        }

        findMethodsContaining("MetadataValueReader")
            .filter { method ->
                method.returnType == "I" &&
                    method.parameterTypes.map { type -> type.toString() } == listOf("Landroid/content/Context;", "I") &&
                    method.containsString("This should never happen.")
            }
            .forEach { method ->
                firstMethod(method).addInstructions(
                    0,
                    """
                        const/4 v0, 0x0
                        return v0
                    """.trimIndent(),
                )
            }

        val spatulaInterceptors = findMethodsContaining(SPATULA_HEADER_ERROR)
        if (spatulaInterceptors.isEmpty()) {
            throw PatchException("Google app Spatula interceptor was not found")
        }
        spatulaInterceptors.forEach { method ->
            val mutableMethod = firstMethod(method)
            val instructions = mutableMethod.instructionsOrNull?.toList().orEmpty()
            val errorIndex = instructions.indexOfFirst { instruction ->
                ((instruction as? ReferenceInstruction)?.reference as? StringReference)?.string ==
                    SPATULA_HEADER_ERROR
            }.takeIf { it >= 0 }
                ?: throw PatchException("Google app Spatula failure branch was not found")
            val catchIndex = (errorIndex downTo 0).firstOrNull { index ->
                instructions[index].opcode == Opcode.MOVE_EXCEPTION
            } ?: throw PatchException("Google app Spatula exception handler was not found")
            val resultRegister = (instructions[catchIndex] as? OneRegisterInstruction)?.registerA
                ?: throw PatchException("Google app Spatula exception register was not found")
            val successField = instructions.take(errorIndex).firstNotNullOfOrNull { instruction ->
                if (instruction.opcode != Opcode.SGET_OBJECT) return@firstNotNullOfOrNull null
                ((instruction as? ReferenceInstruction)?.reference as? FieldReference)
                    ?.takeIf { field -> field.type == method.returnType }
            } ?: throw PatchException("Google app Spatula success result was not found")

            // ReVanced GmsCore can return null when Google's proprietary device key is unavailable.
            // Continue without the optional fallback header so public/API-key requests can proceed.
            mutableMethod.addInstructions(
                catchIndex + 1,
                """
                    sget-object v$resultRegister, $successField
                    return-object v$resultRegister
                """.trimIndent(),
            )
        }
    }
}

private fun app.revanced.patcher.patch.BytecodePatchContext.patchCloudMessagingTokenNotification() {
    val firebaseMessagingClass = classDefs.single { it.type == FIREBASE_MESSAGING }
    val getFirebaseMessaging = firebaseMessagingClass.methods.singleOrNull { method ->
        method.name == "getInstance" &&
            method.parameterTypes.size == 1 &&
            method.returnType == FIREBASE_MESSAGING
    } ?: throw PatchException("Firebase Messaging instance method was not found")
    val firebaseAppType = getFirebaseMessaging.parameterTypes.single().toString()
    val getDefaultFirebaseApp = classDefs.singleOrNull { it.type == firebaseAppType }
        ?.methods
        ?.singleOrNull { method ->
            method.parameterTypes.isEmpty() &&
                method.returnType == firebaseAppType &&
                method.containsString(FIREBASE_NOT_INITIALIZED_ERROR)
        } ?: throw PatchException("Default Firebase app method was not found")
    val notifyNewToken = firebaseMessagingClass.methods.singleOrNull { method ->
        method.parameterTypes.map { it.toString() } == listOf("Ljava/lang/String;") &&
            method.returnType == "V" &&
            method.containsString(FIREBASE_NEW_TOKEN_LOG)
    } ?: throw PatchException("Firebase Cloud Messaging token notification method was not found")

    val tokenBridge = classDefs.singleOrNull { it.type == GMS_COMPAT }
        ?.methods
        ?.singleOrNull { method ->
            method.name == "notifyCloudMessagingToken" &&
                method.parameterTypes.map { it.toString() } ==
                listOf("Ljava/lang/String;", "Ljava/lang/Object;") &&
                method.returnType == "V"
        } ?: throw PatchException("Cloud Messaging token notification bridge was not found")

    firstMethod(tokenBridge).addInstructions(
        0,
        """
            invoke-static {}, $getDefaultFirebaseApp
            move-result-object p1
            invoke-static {p1}, $getFirebaseMessaging
            move-result-object p1
            invoke-virtual {p1, p0}, $notifyNewToken
            return-void
        """.trimIndent(),
    )
}

private fun app.revanced.patcher.patch.BytecodePatchContext.findMethodsContaining(value: String): List<Method> =
    classDefs.asSequence()
        .flatMap { it.methods.asSequence() }
        .filter { it.containsString(value) }
        .toList()

private fun Method.containsString(value: String) = instructionsOrNull?.any { instruction ->
    ((instruction as? ReferenceInstruction)?.reference as? StringReference)?.string == value
} == true

internal fun escapeSmaliString(value: String): String = buildString(value.length) {
    value.forEach { character ->
        when (character) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(character)
        }
    }
}
