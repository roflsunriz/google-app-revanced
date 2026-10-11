package dev.roflsunriz.googleapp

import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction11x
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction21c
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction21t
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction35c
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction3rc
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class GmsCompliancePatchTest {
    private val booleanValue = ImmutableMethodReference("Ljava/lang/Boolean;", "booleanValue", emptyList(), "Z")

    @Test
    fun `復旧処理を持つ無効経路へ分岐するBooleanだけを特定する`() {
        assertEquals(ComplianceGate(2, 5), findGmsComplianceGate(gate()))
    }

    @Test
    fun `range呼び出しと高位レジスタも検出する`() {
        val instructions = gate(register = 200).toMutableList()
        instructions[1] = ImmutableInstruction3rc(Opcode.INVOKE_VIRTUAL_RANGE, 200, 1, booleanValue)
        assertEquals(ComplianceGate(2, 200), findGmsComplianceGate(instructions))
    }

    @Test
    fun `他のBoolean判定や変更された分岐は書き換えない`() {
        assertNull(findGmsComplianceGate(gate().drop(1)))
        assertNull(findGmsComplianceGate(gate().dropLast(1)))
        assertNull(findGmsComplianceGate(gate().reversed()))
        val wrongBranch = gate().toMutableList()
        wrongBranch[3] = ImmutableInstruction21t(Opcode.IF_EQZ, 5, 20)
        assertNull(findGmsComplianceGate(wrongBranch))
        wrongBranch[3] = ImmutableInstruction21t(Opcode.IF_NEZ, 6, 20)
        assertNull(findGmsComplianceGate(wrongBranch))
        val wrongReturn = gate().toMutableList()
        wrongReturn[2] = ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 5)
        assertNull(findGmsComplianceGate(wrongReturn))
    }

    @Test
    fun `曖昧な複数のゲートを拒否する`() {
        val instructions = gate().toMutableList()
        instructions.addAll(4, gate().subList(1, 4))
        assertNull(findGmsComplianceGate(instructions))
    }

    private fun gate(register: Int = 5): List<Instruction> = listOf(
        ImmutableInstruction21c(Opcode.CONST_STRING, 0, ImmutableStringReference(COMPLIANCE_CHECK_LOG)),
        ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 5, 0, 0, 0, 0, booleanValue),
        ImmutableInstruction11x(Opcode.MOVE_RESULT, register),
        ImmutableInstruction21t(Opcode.IF_NEZ, register, 20),
        ImmutableInstruction21c(Opcode.CONST_STRING, 0, ImmutableStringReference(COMPLIANCE_DISABLED_LOG)),
    )
}
