package dev.roflsunriz.googleapp

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instructionsOrNull
import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.firstMethod
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

private const val EXTENSION_PREFIX = "Lapp/revanced/extension/googleapp/"
private const val AD_BLOCKER = "${EXTENSION_PREFIX}AdBlocker;"
private const val WEB_BLOCKER = "${EXTENSION_PREFIX}WebAdBlocker;"

private sealed class Rewrite(open val index: Int) {
    data class Replace(override val index: Int, val smali: String) : Rewrite(index)
    data class Insert(override val index: Int, val smali: String) : Rewrite(index)
}

internal fun BytecodePatchContext.patchAdNetworkBoundaries() {
    transformInstructions(
        match = { classDef, _, instruction, index ->
            if (classDef.type.startsWith(EXTENSION_PREFIX)) return@transformInstructions null

            val stringReference = (instruction as? ReferenceInstruction)?.reference as? StringReference
            if (stringReference != null && AdNetworkClassifier.isAdNetworkLiteral(stringReference.string)) {
                val register = (instruction as? OneRegisterInstruction)?.registerA
                    ?: return@transformInstructions null
                return@transformInstructions Rewrite.Replace(
                    index,
                    "const-string v$register, \"${AdNetworkClassifier.replacement(stringReference.string)}\"",
                )
            }

            val methodReference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
                ?: return@transformInstructions null
            rewriteBoundary(index, instruction, methodReference)
        },
        transform = { method, rewrite ->
            when (rewrite) {
                is Rewrite.Replace -> method.replaceInstruction(rewrite.index, rewrite.smali)
                is Rewrite.Insert -> method.addInstructions(rewrite.index, rewrite.smali)
            }
        },
    )
}

internal fun BytecodePatchContext.patchComposeAdContainers() {
    classDefs.flatMap { classDef -> classDef.methods }
        .filter { method ->
            method.instructionsOrNull?.any { instruction ->
                val value = ((instruction as? ReferenceInstruction)?.reference as? StringReference)?.string
                value != null && value.contains("_ads_container_test_tag")
            } == true
        }
        .forEach { method ->
            val instructions: List<Instruction> = method.instructionsOrNull?.toList() ?: emptyList()
            val markerIndex = instructions.indexOfFirst { instruction ->
                val value = ((instruction as? ReferenceInstruction)?.reference as? StringReference)?.string
                value != null && value.contains("_ads_container_test_tag")
            }
            val renderIndex = (markerIndex + 1 until minOf(markerIndex + 8, instructions.size))
                .firstOrNull { index ->
                    val instruction = instructions[index]
                    val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
                    instruction.opcode in setOf(Opcode.INVOKE_STATIC, Opcode.INVOKE_STATIC_RANGE) &&
                        reference?.returnType == "V"
                }
                ?: return@forEach
            firstMethod(method).replaceInstruction(renderIndex, "nop")
        }
}

private fun rewriteBoundary(
    index: Int,
    instruction: Instruction,
    reference: MethodReference,
): Rewrite? {
    val registers = instruction.argumentRegisters() ?: return null
    val signature = reference.toString()

    return when (signature) {
        "Ljava/net/InetAddress;->getAllByName(Ljava/lang/String;)[Ljava/net/InetAddress;" ->
            Rewrite.Replace(index, registers.staticInvoke(instruction, "$AD_BLOCKER->getAllByName(Ljava/lang/String;)[Ljava/net/InetAddress;"))
        "Ljava/net/InetAddress;->getByName(Ljava/lang/String;)Ljava/net/InetAddress;" ->
            Rewrite.Replace(index, registers.staticInvoke(instruction, "$AD_BLOCKER->getByName(Ljava/lang/String;)Ljava/net/InetAddress;"))
        "Ljava/net/URL;->openConnection()Ljava/net/URLConnection;" ->
            Rewrite.Replace(index, registers.staticInvoke(instruction, "$AD_BLOCKER->openConnection(Ljava/net/URL;)Ljava/net/URLConnection;"))
        "Ljava/net/URL;->openConnection(Ljava/net/Proxy;)Ljava/net/URLConnection;" ->
            Rewrite.Replace(index, registers.staticInvoke(instruction, "$AD_BLOCKER->openConnection(Ljava/net/URL;Ljava/net/Proxy;)Ljava/net/URLConnection;"))
        "Landroid/net/Uri;->parse(Ljava/lang/String;)Landroid/net/Uri;" ->
            Rewrite.Replace(index, registers.staticInvoke(instruction, "$AD_BLOCKER->parseUri(Ljava/lang/String;)Landroid/net/Uri;"))
        "Landroid/webkit/WebView;->loadUrl(Ljava/lang/String;)V" ->
            Rewrite.Replace(index, registers.staticInvoke(instruction, "$WEB_BLOCKER->loadUrl(Landroid/webkit/WebView;Ljava/lang/String;)V"))
        "Landroid/webkit/WebView;->loadUrl(Ljava/lang/String;Ljava/util/Map;)V" ->
            Rewrite.Replace(index, registers.staticInvoke(instruction, "$WEB_BLOCKER->loadUrl(Landroid/webkit/WebView;Ljava/lang/String;Ljava/util/Map;)V"))
        "Landroid/webkit/WebView;->loadDataWithBaseURL(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V" ->
            Rewrite.Replace(index, registers.staticInvoke(instruction, "$WEB_BLOCKER->loadDataWithBaseURL(Landroid/webkit/WebView;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V"))
        else -> stringArgumentRewrite(index, instruction, reference, registers)
    }
}

private fun stringArgumentRewrite(
    index: Int,
    instruction: Instruction,
    reference: MethodReference,
    registers: List<Int>,
): Rewrite? {
    val parameterTypes = reference.parameterTypes.map { it.toString() }
    val stringIndex = when {
        reference.definingClass == "Ljava/net/URL;" && reference.name == "<init>" &&
            parameterTypes == listOf("Ljava/lang/String;") -> 0
        reference.name in setOf("newUrlRequestBuilder", "url", "setUri") &&
            parameterTypes.firstOrNull() == "Ljava/lang/String;" -> 0
        else -> return null
    }
    val parameterOffset = if (instruction.isStaticInvoke()) 0 else 1
    val register = registers.getOrNull(parameterOffset + stringIndex) ?: return null
    if (register > 255) return null
    val invoke = if (register > 15 || instruction is RegisterRangeInstruction) {
        "invoke-static/range { v$register .. v$register }"
    } else {
        "invoke-static { v$register }"
    }
    return Rewrite.Insert(
        index,
        """
            $invoke, $AD_BLOCKER->sanitizeNetworkUrl(Ljava/lang/String;)Ljava/lang/String;
            move-result-object v$register
        """.trimIndent(),
    )
}

private fun Instruction.argumentRegisters(): List<Int>? = when (this) {
    is Instruction35c -> listOf(registerC, registerD, registerE, registerF, registerG).take(registerCount)
    is RegisterRangeInstruction -> (startRegister until startRegister + registerCount).toList()
    else -> null
}

private fun Instruction.isStaticInvoke() = opcode == Opcode.INVOKE_STATIC || opcode == Opcode.INVOKE_STATIC_RANGE

private fun List<Int>.staticInvoke(instruction: Instruction, target: String): String {
    val mnemonic = if (instruction is RegisterRangeInstruction) "invoke-static/range" else "invoke-static"
    val arguments = if (instruction is RegisterRangeInstruction && isNotEmpty()) {
        "v${first()} .. v${last()}"
    } else {
        joinToString(", ") { "v$it" }
    }
    return "$mnemonic { $arguments }, $target"
}
