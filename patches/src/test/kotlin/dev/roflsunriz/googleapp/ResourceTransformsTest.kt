package dev.roflsunriz.googleapp

import java.io.ByteArrayInputStream
import java.nio.file.Files
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.w3c.dom.Document
import org.w3c.dom.Element

class ResourceTransformsTest {
    @Test
    fun `広告権限とSDK宣言を除去し測定サービスを無効化する`() {
        val document = xml(
            """
            <manifest xmlns:android="$ANDROID_NAMESPACE">
              <uses-permission android:name="com.google.android.gms.permission.AD_ID"/>
              <uses-permission android:name="android.permission.INTERNET"/>
              <application android:intentMatchingFlags="0x00000001">
                <meta-data android:name="com.google.android.gms.ads.AD_MANAGER_APP" android:value="true"/>
                <service android:name="com.google.android.gms.measurement.AppMeasurementService" android:enabled="true"/>
              </application>
            </manifest>
            """.trimIndent(),
        )

        removeAdvertisingManifestEntries(document)

        val contents = document.documentElement.textContent
        assertFalse(contents.contains("AD_ID"))
        val permissions = document.getElementsByTagName("uses-permission")
        assertEquals(1, permissions.length)
        val service = document.getElementsByTagName("service").item(0) as Element
        assertEquals("false", service.getAttributeNS(ANDROID_NAMESPACE, "enabled"))
        val application = document.getElementsByTagName("application").item(0) as Element
        assertFalse(application.hasAttributeNS(ANDROID_NAMESPACE, "intentMatchingFlags"))
        assertEquals(0, document.getElementsByTagName("meta-data").length)
    }

    @Test
    fun `広告レイアウトと寸法を幅高さゼロへ折り畳む`() {
        val layout = xml(
            """
            <FrameLayout xmlns:android="$ANDROID_NAMESPACE"
                android:layout_width="match_parent" android:layout_height="120dp"/>
            """.trimIndent(),
        )
        collapseLayout(layout)
        val root = layout.documentElement
        assertEquals("0.0dp", root.getAttributeNS(ANDROID_NAMESPACE, "layout_width"))
        assertEquals("0.0dp", root.getAttributeNS(ANDROID_NAMESPACE, "layout_height"))
        assertEquals("gone", root.getAttributeNS(ANDROID_NAMESPACE, "visibility"))

        val dimensions = xml("<resources><dimen name=\"ad_lightbox_height\">120dp</dimen><dimen name=\"card_height\">48dp</dimen></resources>")
        assertEquals(1, zeroAdvertisingDimensions(dimensions))
        assertTrue(dimensions.documentElement.firstChild.textContent.contains("0.0dp"))
    }

    @Test
    fun `広告と無関係なaddやpaddingを誤検出しない`() {
        assertTrue(ResourceClassifier.shouldCollapseLayout("duplo_ad_video"))
        assertTrue(ResourceClassifier.shouldCollapseLayout("googleapp_discover_promo"))
        assertTrue(ResourceClassifier.shouldCollapseLayout("feature_promotional_card"))
        assertFalse(ResourceClassifier.shouldCollapseLayout("add_language"))
        assertFalse(ResourceClassifier.isAdName("content_padding"))
    }

    @Test
    fun `システム専用資源をAAPT2の非公開参照記法へ戻す`() {
        val directory = Files.createTempDirectory("google-revanced-resources").toFile()
        val file = directory.resolve("values/arrays.xml")
        file.parentFile.mkdirs()
        file.writeText("<resources android:usesAssistData=\"true\"><item>@android:color/SIM_color_blue</item><item>?android:attr/colorAccent</item></resources>")

        assertEquals(1, restorePrivateFrameworkReferences(directory))
        assertTrue(file.readText().contains("@*android:color/SIM_color_blue"))
        assertTrue(file.readText().contains("?android:attr/colorAccent"))
        assertFalse(file.readText().contains("usesAssistData"))
    }

    @Test
    fun `各プロセスへ固有の初期化Providerを追加する`() {
        val document = xml(
            """
            <manifest xmlns:android="$ANDROID_NAMESPACE">
              <application/>
            </manifest>
            """.trimIndent(),
        )

        installExtensionComponents(document)

        val providers = (0 until document.getElementsByTagName("provider").length)
            .map { document.getElementsByTagName("provider").item(it) as Element }
        assertEquals(
            listOf(
                "app.revanced.extension.googleapp.BootstrapProvider",
                "app.revanced.extension.googleapp.GoogleAppBootstrapProvider",
                "app.revanced.extension.googleapp.SearchBootstrapProvider",
            ),
            providers.map { it.getAttributeNS(ANDROID_NAMESPACE, "name") },
        )
        assertEquals(
            listOf("", ":googleapp", ":search"),
            providers.map { it.getAttributeNS(ANDROID_NAMESPACE, "process") },
        )
        assertEquals(3, providers.map { it.getAttributeNS(ANDROID_NAMESPACE, "authorities") }.toSet().size)
    }

    private fun xml(value: String): Document = DocumentBuilderFactory.newInstance().run {
        isNamespaceAware = true
        newDocumentBuilder().parse(ByteArrayInputStream(value.toByteArray()))
    }
}
