package dev.roflsunriz.googleapp

import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.w3c.dom.Element

class GmsCoreTransformsTest {
    @Test
    fun `manifestを非root用パッケージとGmsCoreへ変換する`() {
        val document = xml(
            """
            <manifest xmlns:android="$ANDROID_NAMESPACE" package="$ORIGINAL_PACKAGE">
              <uses-permission android:name="com.google.android.c2dm.permission.RECEIVE"/>
              <permission android:name="$ORIGINAL_PACKAGE.INTERNAL"/>
              <queries/>
              <application android:label="Google">
                <provider android:name="example.Provider" android:authorities="$ORIGINAL_PACKAGE.provider"
                    android:permission="$ORIGINAL_PACKAGE.INTERNAL"/>
                <receiver android:name="example.GcmReceiver">
                  <intent-filter>
                    <action android:name="com.google.android.c2dm.intent.RECEIVE"/>
                    <category android:name="$ORIGINAL_PACKAGE"/>
                    <category android:name="$ORIGINAL_PACKAGE.gcm"/>
                    <category android:name="android.intent.category.DEFAULT"/>
                  </intent-filter>
                </receiver>
              </application>
            </manifest>
            """.trimIndent(),
        )

        configureGmsCoreManifest(document)

        assertEquals(REVANCED_PACKAGE, document.documentElement.getAttribute("package"))
        val permission = document.getElementsByTagName("permission").item(0) as Element
        assertEquals("$REVANCED_PACKAGE.INTERNAL", permission.getAttribute("android:name"))
        val usesPermission = document.getElementsByTagName("uses-permission").item(0) as Element
        assertEquals("$GMS_CORE_VENDOR.android.c2dm.permission.RECEIVE", usesPermission.getAttribute("android:name"))
        val provider = document.getElementsByTagName("provider").item(0) as Element
        assertEquals("$REVANCED_PACKAGE.provider", provider.getAttribute("android:authorities"))
        assertEquals("$REVANCED_PACKAGE.INTERNAL", provider.getAttribute("android:permission"))
        val categories = (0 until document.getElementsByTagName("category").length)
            .map { document.getElementsByTagName("category").item(it) as Element }
            .map { it.getAttribute("android:name") }
        assertEquals(
            listOf(REVANCED_PACKAGE, "$REVANCED_PACKAGE.gcm", "android.intent.category.DEFAULT"),
            categories,
        )
        val queriedPackages = (0 until document.getElementsByTagName("package").length)
            .map { document.getElementsByTagName("package").item(it) as Element }
            .map { it.getAttribute("android:name") }
        assertTrue(GMS_CORE_PACKAGE in queriedPackages)
        val metadata = (0 until document.getElementsByTagName("meta-data").length)
            .map { document.getElementsByTagName("meta-data").item(it) as Element }
            .associate { it.getAttribute("android:name") to it.getAttribute("android:value") }
        assertEquals(ORIGINAL_PACKAGE, metadata["$GMS_CORE_PACKAGE.SPOOFED_PACKAGE_NAME"])
        assertEquals(GOOGLE_APP_OAUTH_SIGNATURE_SHA1, metadata["$GMS_CORE_PACKAGE.SPOOFED_PACKAGE_SIGNATURE"])
        assertEquals("38918a453d07199354f8b19af05ec6562ced5788", GOOGLE_APP_OAUTH_SIGNATURE_SHA1)
        assertNotNull(metadata["app.revanced.MICROG_PACKAGE_NAME"])
    }

    @Test
    fun `resource参照のprovider authorityは変更しない`() {
        val document = xml(
            """
            <manifest xmlns:android="$ANDROID_NAMESPACE" package="$ORIGINAL_PACKAGE">
              <application>
                <provider android:name="example.Provider" android:authorities="@string/provider_authority"/>
              </application>
            </manifest>
            """.trimIndent(),
        )

        configureGmsCoreManifest(document)

        val provider = document.getElementsByTagName("provider").item(0) as Element
        assertEquals("@string/provider_authority", provider.getAttribute("android:authorities"))
    }

    @Test
    fun `GmsCoreとプロセスとcontent authorityの文字列を変換する`() {
        GmsCoreState.reset()
        GmsCoreState.appAuthorities["$ORIGINAL_PACKAGE.provider"] = "$REVANCED_PACKAGE.provider"
        assertEquals(GMS_CORE_PACKAGE, transformGmsString("com.google.android.gms"))
        assertEquals(
            "$GMS_CORE_PACKAGE.phenotype.service.START",
            transformGmsString("com.google.android.gms.phenotype.service.START"),
        )
        assertNull(transformGmsString("com.google.android.gms.clearcut.service.START"))
        assertNull(transformGmsString("com.google.android.gms.common.GoogleApiAvailability"))
        assertNull(transformGmsString(ORIGINAL_PACKAGE))
        assertEquals(
            "$GMS_CORE_VENDOR.android.gsf.gservices",
            transformGmsString("com.google.android.gsf.gservices"),
        )
        assertNull(transformGmsString("$ORIGINAL_PACKAGE:googleapp"))
        assertEquals(
            "content://$REVANCED_PACKAGE.provider/items",
            transformGmsString("content://$ORIGINAL_PACKAGE.provider/items"),
        )
        assertTrue(escapeSmaliString("a\\b\"c").contains("\\\\"))
    }

    private fun xml(value: String) = DocumentBuilderFactory.newInstance().run {
        isNamespaceAware = true
        newDocumentBuilder().parse(ByteArrayInputStream(value.toByteArray()))
    }
}
