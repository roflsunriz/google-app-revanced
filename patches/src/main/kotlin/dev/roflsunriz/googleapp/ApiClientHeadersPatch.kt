package dev.roflsunriz.googleapp

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instructionsOrNull
import app.revanced.patcher.firstMethod
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.patch.PatchException
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

private const val HEADER_WRITE_LOOKAHEAD = 96
private const val HEADER_KEY_CONSTRUCTOR_LOOKAHEAD = 12
private val SHA1_PATTERN = Regex("^[0-9A-Fa-f]{40}$")

internal enum class ApiClientHeader(val wireName: String) {
    PACKAGE("X-Android-Package"),
    CERTIFICATE("X-Android-Cert"),
}

internal data class ApiHeaderWrite(
    val header: ApiClientHeader,
    val instructionIndex: Int,
    val valueRegister: Int,
)

private data class MethodHeaderWrite(
    val method: Method,
    val write: ApiHeaderWrite,
)

private val invokeOpcodes = setOf(
    Opcode.INVOKE_DIRECT,
    Opcode.INVOKE_DIRECT_RANGE,
    Opcode.INVOKE_INTERFACE,
    Opcode.INVOKE_INTERFACE_RANGE,
    Opcode.INVOKE_STATIC,
    Opcode.INVOKE_STATIC_RANGE,
    Opcode.INVOKE_SUPER,
    Opcode.INVOKE_SUPER_RANGE,
    Opcode.INVOKE_VIRTUAL,
    Opcode.INVOKE_VIRTUAL_RANGE,
)

private val staticInvokeOpcodes = setOf(
    Opcode.INVOKE_STATIC,
    Opcode.INVOKE_STATIC_RANGE,
)

internal fun BytecodePatchContext.patchApiClientHeaders() {
    val methods = classDefs.asSequence()
        .flatMap { it.methods.asSequence() }
        .toList()
    val staticHeaderFields = findStaticHeaderFields(methods)
    val writes = methods.flatMap { method ->
        val instructions = method.instructionsOrNull?.toList().orEmpty()
        findApiHeaderWrites(instructions, staticHeaderFields).map { write ->
            MethodHeaderWrite(method, write)
        }
    }

    ApiClientHeader.entries.forEach { header ->
        if (writes.none { it.write.header == header }) {
            throw PatchException("Google API ${header.wireName} header writes were not found")
        }
    }

    val googleApiCertificate = discoverGoogleApiCertificate(writes)
    writes.groupBy(MethodHeaderWrite::method).forEach { (method, methodWrites) ->
        val mutableMethod = firstMethod(method)
        methodWrites
            .distinctBy { it.write.header to it.write.instructionIndex }
            .sortedByDescending { it.write.instructionIndex }
            .forEach { (_, write) ->
                if (write.valueRegister > UByte.MAX_VALUE.toInt()) {
                    throw PatchException(
                        "Google API ${write.header.wireName} value register is too large: " +
                            "v${write.valueRegister}",
                    )
                }
                val replacement = when (write.header) {
                    ApiClientHeader.PACKAGE -> ORIGINAL_PACKAGE
                    ApiClientHeader.CERTIFICATE -> googleApiCertificate
                }
                if (constantValueImmediatelyBefore(method, write) == replacement) return@forEach
                mutableMethod.addInstructions(
                    write.instructionIndex,
                    "const-string v${write.valueRegister}, \"${escapeSmaliString(replacement)}\"",
                )
            }
    }
}

private fun constantValueImmediatelyBefore(method: Method, write: ApiHeaderWrite): String? {
    val instruction = method.instructionsOrNull?.toList()?.getOrNull(write.instructionIndex - 1)
        ?: return null
    if (instruction.opcode !in setOf(Opcode.CONST_STRING, Opcode.CONST_STRING_JUMBO)) return null
    if ((instruction as? OneRegisterInstruction)?.registerA != write.valueRegister) return null
    return ((instruction as? ReferenceInstruction)?.reference as? StringReference)?.string
}

internal fun findApiHeaderWrites(
    instructions: List<Instruction>,
    staticHeaderFields: Map<String, ApiClientHeader> = emptyMap(),
): List<ApiHeaderWrite> {
    val writes = mutableListOf<ApiHeaderWrite>()

    instructions.forEachIndexed { index, instruction ->
        val string = ((instruction as? ReferenceInstruction)?.reference as? StringReference)?.string
        val directHeader = ApiClientHeader.entries.firstOrNull { it.wireName.equals(string, ignoreCase = true) }
        if (directHeader != null) {
            val headerRegister = (instruction as? OneRegisterInstruction)?.registerA ?: return@forEachIndexed
            val keyRegisters = mutableSetOf(headerRegister)
            instructions.indicesAfter(index, HEADER_KEY_CONSTRUCTOR_LOOKAHEAD).forEach { candidateIndex ->
                val candidate = instructions[candidateIndex]
                val reference = (candidate as? ReferenceInstruction)?.reference as? MethodReference
                    ?: return@forEach
                if (reference.name != "<init>") return@forEach
                val registers = candidate.invokeRegisters() ?: return@forEach
                if (headerRegister in registers.drop(1)) keyRegisters += registers.first()
            }
            findHeaderWrite(instructions, directHeader, index, keyRegisters)?.let(writes::add)
        }

        if (instruction.opcode != Opcode.SGET_OBJECT) return@forEachIndexed
        val field = (instruction as? ReferenceInstruction)?.reference as? FieldReference
            ?: return@forEachIndexed
        val header = staticHeaderFields[field.toString()] ?: return@forEachIndexed
        val keyRegister = (instruction as? OneRegisterInstruction)?.registerA ?: return@forEachIndexed
        findHeaderWrite(instructions, header, index, setOf(keyRegister))?.let(writes::add)
    }

    return writes.distinctBy { it.header to it.instructionIndex }
}

internal fun selectGoogleApiCertificate(candidates: Collection<String>): String {
    val certificateCounts = candidates
        .filter(SHA1_PATTERN::matches)
        .map(String::uppercase)
        .groupingBy { it }
        .eachCount()
    val highestCount = certificateCounts.values.maxOrNull()
    val certificates = certificateCounts.filterValues { it == highestCount }.keys
    if (certificates.size != 1) {
        throw PatchException(
            "A unique Google app API certificate was not found (${certificates.size} candidates)",
        )
    }
    return certificates.single()
}

private fun findStaticHeaderFields(methods: List<Method>): Map<String, ApiClientHeader> = buildMap {
    methods.filter { it.name == "<clinit>" }.forEach { method ->
        val instructions = method.instructionsOrNull?.toList().orEmpty()
        instructions.forEachIndexed { index, instruction ->
            val string = ((instruction as? ReferenceInstruction)?.reference as? StringReference)?.string
            val header = ApiClientHeader.entries.firstOrNull { it.wireName.equals(string, ignoreCase = true) }
                ?: return@forEachIndexed
            val field = instructions.indicesAfter(index, HEADER_KEY_CONSTRUCTOR_LOOKAHEAD)
                .firstNotNullOfOrNull { candidateIndex ->
                    val candidate = instructions[candidateIndex]
                    if (candidate.opcode != Opcode.SPUT_OBJECT) return@firstNotNullOfOrNull null
                    (candidate as? ReferenceInstruction)?.reference as? FieldReference
                }
                ?: return@forEachIndexed
            put(field.toString(), header)
        }
    }
}

private fun discoverGoogleApiCertificate(writes: List<MethodHeaderWrite>): String {
    val certificateMethods = writes.asSequence()
        .filter { it.write.header == ApiClientHeader.CERTIFICATE }
        .map(MethodHeaderWrite::method)
        .distinctBy { method ->
            listOf(
                method.definingClass,
                method.name,
                method.parameterTypes.joinToString(separator = ""),
                method.returnType,
            )
        }
        .toList()
    val candidates = certificateMethods.asSequence()
        .flatMap { method ->
            method.instructionsOrNull
                ?.asSequence()
                .orEmpty()
                .mapNotNull { instruction ->
                    ((instruction as? ReferenceInstruction)?.reference as? StringReference)?.string
                }
                .filter(SHA1_PATTERN::matches)
        }
        .toList()
    if (candidates.isEmpty()) {
        throw PatchException(
            "Google app API certificate candidates were not found " +
                "(${certificateMethods.size} certificate writers)",
        )
    }
    return selectGoogleApiCertificate(candidates)
}

private fun findHeaderWrite(
    instructions: List<Instruction>,
    header: ApiClientHeader,
    sourceIndex: Int,
    keyRegisters: Set<Int>,
): ApiHeaderWrite? = instructions.indicesAfter(sourceIndex, HEADER_WRITE_LOOKAHEAD)
    .firstNotNullOfOrNull { index ->
        val instruction = instructions[index]
        if (instruction.opcode !in invokeOpcodes) return@firstNotNullOfOrNull null
        val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
            ?: return@firstNotNullOfOrNull null
        if (reference.name == "<init>" || reference.returnType != "V" || reference.parameterTypes.size != 2) {
            return@firstNotNullOfOrNull null
        }
        val registers = instruction.invokeRegisters() ?: return@firstNotNullOfOrNull null
        val parameterOffset = if (instruction.opcode in staticInvokeOpcodes) 0 else 1
        if (registers.size < parameterOffset + 2) return@firstNotNullOfOrNull null
        if (registers[parameterOffset] !in keyRegisters) return@firstNotNullOfOrNull null
        ApiHeaderWrite(header, index, registers[parameterOffset + 1])
    }

private fun Instruction.invokeRegisters(): List<Int>? = when (this) {
    is FiveRegisterInstruction -> listOf(registerC, registerD, registerE, registerF, registerG)
        .take(registerCount)
    is RegisterRangeInstruction -> (startRegister until startRegister + registerCount).toList()
    else -> null
}

private fun List<Instruction>.indicesAfter(index: Int, maximumCount: Int): IntRange {
    if (index >= lastIndex) return IntRange.EMPTY
    return (index + 1)..minOf(lastIndex, index + maximumCount)
}
