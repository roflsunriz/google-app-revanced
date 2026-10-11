package dev.roflsunriz.googleapp

import app.revanced.patcher.extensions.instructionsOrNull
import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.firstMethod
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.patch.PatchException
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

internal const val COMPLIANCE_CHECK_LOG = "Checking for device compliance..."
internal const val COMPLIANCE_DISABLED_LOG = "Feature is disabled!"

internal data class ComplianceGate(val resultIndex: Int, val resultRegister: Int)

internal fun findGmsComplianceGate(instructions: List<Instruction>): ComplianceGate? {
    fun stringIndex(value: String) = instructions.indexOfFirst {
        ((it as? ReferenceInstruction)?.reference as? StringReference)?.string == value
    }
    val checkIndex = stringIndex(COMPLIANCE_CHECK_LOG)
    val disabledIndex = stringIndex(COMPLIANCE_DISABLED_LOG)
    if (checkIndex < 0 || disabledIndex <= checkIndex) return null

    val gates = ((checkIndex + 1) until disabledIndex).mapNotNull { index ->
        val instruction = instructions[index]
        if (instruction.opcode !in setOf(Opcode.INVOKE_VIRTUAL, Opcode.INVOKE_VIRTUAL_RANGE)) {
            return@mapNotNull null
        }
        val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
            ?: return@mapNotNull null
        if (reference.toString() != "Ljava/lang/Boolean;->booleanValue()Z") return@mapNotNull null
        val result = instructions.getOrNull(index + 1) as? OneRegisterInstruction ?: return@mapNotNull null
        if (result.opcode != Opcode.MOVE_RESULT) return@mapNotNull null
        val branch = instructions.getOrNull(index + 2) as? OneRegisterInstruction ?: return@mapNotNull null
        if (branch.opcode != Opcode.IF_NEZ || branch.registerA != result.registerA) return@mapNotNull null
        ComplianceGate(index + 1, result.registerA)
    }
    return gates.singleOrNull()
}

internal fun BytecodePatchContext.patchGmsComplianceGate() {
    val matches = classDefs.asSequence().flatMap { it.methods.asSequence() }
        .filter { it.returnType == "V" }
        .mapNotNull { method ->
            findGmsComplianceGate(method.instructionsOrNull?.toList().orEmpty())?.let { method to it }
        }.toList()
    if (matches.size != 1) {
        throw PatchException("Google app GmsCompliance feature gate was not uniquely found (${matches.size})")
    }
    val (method, gate) = matches.single()
    // Use Google's existing feature-disabled branch, including component recovery.
    // Do not forge device certification or remove the refusal Activity alone:
    // the refusal callback also disables services, receivers and providers.
    firstMethod(method).replaceInstruction(gate.resultIndex, "const/16 v${gate.resultRegister}, 0x0")
}
