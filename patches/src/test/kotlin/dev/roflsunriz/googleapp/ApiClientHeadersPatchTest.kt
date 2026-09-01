package dev.roflsunriz.googleapp

import app.revanced.patcher.patch.PatchException
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction21c
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction35c
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction3rc
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableFieldReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ApiClientHeadersPatchTest {
    @Test
    fun `ローカル生成したAPIヘッダーの値レジスタを検出する`() {
        val instructions = listOf(
            stringInstruction(1, ApiClientHeader.PACKAGE.wireName),
            ImmutableInstruction35c(
                Opcode.INVOKE_DIRECT,
                3,
                2,
                1,
                3,
                0,
                0,
                method("Lkey;", "<init>", listOf("Ljava/lang/String;", "Ljava/lang/Object;"), "V"),
            ),
            stringInstruction(4, REVANCED_PACKAGE),
            ImmutableInstruction35c(
                Opcode.INVOKE_VIRTUAL,
                3,
                0,
                2,
                4,
                0,
                0,
                method("Lmetadata;", "put", listOf("Lkey;", "Ljava/lang/Object;"), "V"),
            ),
        )

        assertEquals(
            listOf(ApiHeaderWrite(ApiClientHeader.PACKAGE, 3, 4)),
            findApiHeaderWrites(instructions),
        )
    }

    @Test
    fun `静的フィールド化されたAPIヘッダーキーを追跡する`() {
        val field = ImmutableFieldReference("Lheaders;", "packageHeader", "Lkey;")
        val instructions = listOf(
            ImmutableInstruction21c(Opcode.SGET_OBJECT, 2, field),
            stringInstruction(4, REVANCED_PACKAGE),
            ImmutableInstruction35c(
                Opcode.INVOKE_VIRTUAL,
                3,
                0,
                2,
                4,
                0,
                0,
                method("Lmetadata;", "put", listOf("Lkey;", "Ljava/lang/Object;"), "V"),
            ),
        )

        assertEquals(
            listOf(ApiHeaderWrite(ApiClientHeader.PACKAGE, 2, 4)),
            findApiHeaderWrites(instructions, mapOf(field.toString() to ApiClientHeader.PACKAGE)),
        )
    }

    @Test
    fun `range形式の呼び出しでも証明書値レジスタを検出する`() {
        val instructions = listOf(
            stringInstruction(20, ApiClientHeader.CERTIFICATE.wireName),
            stringInstruction(21, "58E1C4133F7441EC3D2C270270A14802DA47BA0E"),
            ImmutableInstruction3rc(
                Opcode.INVOKE_VIRTUAL_RANGE,
                19,
                3,
                method("Lconnection;", "set", listOf("Ljava/lang/String;", "Ljava/lang/String;"), "V"),
            ),
        )

        assertEquals(
            listOf(ApiHeaderWrite(ApiClientHeader.CERTIFICATE, 2, 21)),
            findApiHeaderWrites(instructions),
        )
    }

    @Test
    fun `公式API証明書は表記を正規化して一意に選ぶ`() {
        assertEquals(
            "58E1C4133F7441EC3D2C270270A14802DA47BA0E",
            selectGoogleApiCertificate(
                listOf(
                    "58e1c4133f7441ec3d2c270270a14802da47ba0e",
                    "58E1C4133F7441EC3D2C270270A14802DA47BA0E",
                    "not-a-certificate",
                ),
            ),
        )
    }

    @Test
    fun `複数の証明書候補では公式経路で反復利用される値を選ぶ`() {
        assertEquals(
            "58E1C4133F7441EC3D2C270270A14802DA47BA0E",
            selectGoogleApiCertificate(
                listOf(
                    "58E1C4133F7441EC3D2C270270A14802DA47BA0E",
                    "0DE6ACA2462DC51F5511C450B1FA0AC613550FAE",
                    "58E1C4133F7441EC3D2C270270A14802DA47BA0E",
                ),
            ),
        )
    }

    @Test
    fun `公式API証明書を一意に決められなければ失敗する`() {
        assertThrows(PatchException::class.java) {
            selectGoogleApiCertificate(
                listOf(
                    "58E1C4133F7441EC3D2C270270A14802DA47BA0E",
                    "0DE6ACA2462DC51F5511C450B1FA0AC613550FAE",
                ),
            )
        }
    }

    private fun stringInstruction(register: Int, value: String) = ImmutableInstruction21c(
        Opcode.CONST_STRING,
        register,
        ImmutableStringReference(value),
    )

    private fun method(
        definingClass: String,
        name: String,
        parameters: List<String>,
        returnType: String,
    ) = ImmutableMethodReference(definingClass, name, parameters, returnType)
}
