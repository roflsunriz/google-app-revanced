package app.revanced.extension.googleapp;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class GmsCoreCompatTest {
    @Test
    public void exposesCloneProcessesAsOriginalProcesses() {
        assertEquals(
                "com.google.android.googlequicksearchbox:googleapp",
                GmsCoreCompat.asOriginalProcessName("app.revanced.android.googleapp:googleapp")
        );
        assertEquals(
                "com.google.android.googlequicksearchbox",
                GmsCoreCompat.asOriginalProcessName("app.revanced.android.googleapp")
        );
        assertEquals("other.process", GmsCoreCompat.asOriginalProcessName("other.process"));
        assertNull(GmsCoreCompat.asOriginalProcessName(null));
    }
}
