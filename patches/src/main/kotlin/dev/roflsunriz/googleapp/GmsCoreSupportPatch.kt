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

        val processSelector = classDefs.flatMap { it.methods }.singleOrNull { method ->
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

        val disabledProcessExits = classDefs.flatMap { it.methods }
            .sumOf { method ->
                val mutableMethod = firstMethod(method)
                val exitIndexes = mutableMethod.instructionsOrNull
                    .orEmpty()
                    .mapIndexedNotNull { index, instruction ->
                        val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
                            ?: return@mapIndexedNotNull null
                        index.takeIf { reference.toString() in processExitMethods }
                    }
                exitIndexes.forEach { index -> mutableMethod.replaceInstruction(index, "nop") }
                exitIndexes.size
            }
        if (disabledProcessExits == 0) {
            throw PatchException("Google app process reapers were not found")
        }

        val systemExitWrappers = findMethodsContaining(SYSTEM_EXIT_RETURNED_ERROR)
            .filter { method -> method.returnType == "V" }
        if (systemExitWrappers.size < 2) {
            throw PatchException("Google app System.exit wrappers were not found")
        }
        systemExitWrappers.forEach { firstMethod(it).addInstructions(0, "return-void") }

        val unsupportedFeatureFields = classDefs.flatMap { it.methods }.flatMap { method ->
            val instructions = method.instructionsOrNull?.toList().orEmpty()
            instructions.mapIndexedNotNull { index, instruction ->
                val featureName = ((instruction as? ReferenceInstruction)?.reference as? StringReference)?.string
                    ?.takeIf { it in unsupportedPhenotypeFeatures }
                    ?: return@mapIndexedNotNull null
                instructions.drop(index + 1).take(16).firstNotNullOfOrNull { candidate ->
                    if (candidate.opcode != Opcode.SPUT_OBJECT) return@firstNotNullOfOrNull null
                    (candidate as? ReferenceInstruction)?.reference as? FieldReference
                } ?: throw PatchException("Phenotype feature field was not found for $featureName")
            }
        }.map(FieldReference::toString).toSet()
        if (unsupportedFeatureFields.size != unsupportedPhenotypeFeatures.size) {
            throw PatchException("Phenotype feature fields were not found")
        }

        val removedFeatureRequirements = classDefs.flatMap { it.methods }.sumOf { method ->
            val mutableMethod = firstMethod(method)
            val instructions = mutableMethod.instructionsOrNull?.toList().orEmpty()
            val requirementIndexes = instructions.mapIndexedNotNull { index, instruction ->
                if (instruction.opcode != Opcode.SGET_OBJECT) return@mapIndexedNotNull null
                val field = (instruction as? ReferenceInstruction)?.reference as? FieldReference
                    ?: return@mapIndexedNotNull null
                index.takeIf { field.toString() in unsupportedFeatureFields }
            }
            requirementIndexes.sumOf { fieldIndex ->
                val aputIndex = ((fieldIndex + 1)..minOf(fieldIndex + 6, instructions.lastIndex))
                    .firstOrNull { instructions[it].opcode == Opcode.APUT_OBJECT }
                    ?: throw PatchException("Phenotype feature requirement array was not found")
                val arrayRegister = (instructions[aputIndex] as ThreeRegisterInstruction).registerB
                mutableMethod.replaceInstruction(aputIndex, "const/4 v$arrayRegister, 0x0")
                1
            }
        }
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

private fun app.revanced.patcher.patch.BytecodePatchContext.findMethodsContaining(value: String): List<Method> =
    classDefs.flatMap { it.methods }.filter { it.containsString(value) }

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
