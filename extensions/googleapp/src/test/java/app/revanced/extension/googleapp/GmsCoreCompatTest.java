package app.revanced.extension.googleapp;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

    @Test
    public void requestsCloudMessagingOnlyInCloneGoogleAppProcess() {
        assertTrue(GmsCoreCompat.shouldRequestCloudMessagingRegistration(
                "app.revanced.android.googleapp",
                "app.revanced.android.googleapp:googleapp"
        ));
        assertFalse(GmsCoreCompat.shouldRequestCloudMessagingRegistration(
                "app.revanced.android.googleapp",
                "app.revanced.android.googleapp:search"
        ));
        assertFalse(GmsCoreCompat.shouldRequestCloudMessagingRegistration(
                "com.google.android.googlequicksearchbox",
                "com.google.android.googlequicksearchbox:googleapp"
        ));
    }
}
