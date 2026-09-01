package dev.roflsunriz.googleapp

import app.revanced.patcher.patch.PatchException
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PatcherRuntimeRequirementsTest {
    @Test
    fun `rejects Manager in-process heap before the multi-dex write can stall`() {
        assertThrows(PatchException::class.java) {
            requireSufficientPatcherHeap(512L * 1024L * 1024L)
        }
    }

    @Test
    fun `accepts Manager process runtime default heap`() {
        assertDoesNotThrow {
            requireSufficientPatcherHeap(MINIMUM_PATCHER_HEAP_MIB * 1024L * 1024L)
        }
    }
}
