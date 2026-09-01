package dev.roflsunriz.googleapp

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File

internal const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"

internal fun removeAdvertisingManifestEntries(document: Document) {
    val root = document.documentElement
    root.childElements()
        .filter { element ->
            when (element.tagName) {
                "uses-permission" -> element.androidName() in setOf(
                    "com.google.android.gms.permission.AD_ID",
                    "android.permission.ACCESS_ADSERVICES_AD_ID",
                    "android.permission.ACCESS_ADSERVICES_ATTRIBUTION",
                    "android.permission.POST_PROMOTED_NOTIFICATIONS",
                )
                "uses-library" -> element.androidName() == "android.ext.adservices"
                else -> false
            }
        }
        .forEach(root::removeChild)

    val application = root.childElements().firstOrNull { it.tagName == "application" } ?: return
    // Google 17.52 targets API 37 while the newest public SDK available to the
    // patcher is API 36. This future, optional package-matching hint cannot be
    // linked by API 36 AAPT2 and is ignored by current devices, so omit it.
    application.removeAttributeNS(ANDROID_NAMESPACE, "intentMatchingFlags")
    application.removeAttribute("android:intentMatchingFlags")
    application.childElements()
        .filter { element ->
            val name = element.androidName()
            (element.tagName == "meta-data" && name == "com.google.android.gms.ads.AD_MANAGER_APP") ||
                (element.tagName == "property" && name.contains("AD_SERVICES", ignoreCase = true)) ||
                (element.tagName == "uses-library" && name == "android.ext.adservices")
        }
        .forEach(application::removeChild)

    application.childElements()
        .filter { element ->
            element.tagName in setOf("service", "receiver") &&
                element.androidName().startsWith("com.google.android.gms.measurement.")
        }
        .forEach { it.setAttribute("android:enabled", "false") }

    application.childElements()
        .filter { it.androidName() == "com.google.firebase.components.ComponentDiscoveryService" }
        .flatMap { it.childElements() }
        .filter { it.androidName().contains("firebase.analytics", ignoreCase = true) }
        .toList()
        .forEach { it.parentNode.removeChild(it) }
}

internal fun installExtensionComponents(document: Document) {
    val application = document.documentElement.childElements()
        .first { it.tagName == "application" }

    val settingsActivity = "app.revanced.extension.googleapp.GoogleAppReVancedSettingsActivity"
    if (!application.hasComponent("activity", settingsActivity)) {
        application.appendChild(document.createElement("activity").apply {
            setAttributeNS(ANDROID_NAMESPACE, "android:name", settingsActivity)
            setAttributeNS(ANDROID_NAMESPACE, "android:label", "Google ReVanced")
            setAttributeNS(ANDROID_NAMESPACE, "android:theme", "@style/Theme.GoogleApp.Settings")
            setAttributeNS(ANDROID_NAMESPACE, "android:exported", "false")
            setAttributeNS(ANDROID_NAMESPACE, "android:process", ":googleapp")
        })
    }

    listOf(
        "app.revanced.extension.googleapp.BootstrapProvider" to null,
        "app.revanced.extension.googleapp.GoogleAppBootstrapProvider" to ":googleapp",
        "app.revanced.extension.googleapp.SearchBootstrapProvider" to ":search",
    ).forEachIndexed { index, (providerClass, process) ->
        val authority = "com.google.android.googlequicksearchbox.revanced.bootstrap.$index"
        val exists = application.childElements().any {
            it.tagName == "provider" && it.androidAttribute("authorities") == authority
        }
        if (!exists) {
            application.appendChild(document.createElement("provider").apply {
                setAttributeNS(
                    ANDROID_NAMESPACE,
                    "android:name",
                    providerClass,
                )
                setAttributeNS(ANDROID_NAMESPACE, "android:authorities", authority)
                setAttributeNS(ANDROID_NAMESPACE, "android:exported", "false")
                setAttributeNS(ANDROID_NAMESPACE, "android:initOrder", "999999")
                process?.let { setAttributeNS(ANDROID_NAMESPACE, "android:process", it) }
            })
        }
    }
}

internal fun collapseLayout(document: Document) {
    val root = document.documentElement
    if (root.tagName == "merge") {
        root.childElements().forEach(Element::collapse)
    } else {
        root.collapse()
    }
}

internal fun collapseNamedAdViews(document: Document): Int {
    var collapsed = 0
    fun visit(element: Element) {
        val id = element.androidAttribute("id")
            .substringAfterLast('/')
            .substringAfterLast('+')
        if (ResourceClassifier.isAdName(id) || ResourceClassifier.isPromotionName(id)) {
            element.collapse()
            collapsed++
            return
        }
        element.childElements().forEach(::visit)
    }
    visit(document.documentElement)
    return collapsed
}

internal fun zeroAdvertisingDimensions(document: Document): Int {
    var changed = 0
    document.documentElement.childElements()
        .filter { it.tagName == "dimen" }
        .filter {
            val name = it.getAttribute("name")
            ResourceClassifier.isAdName(name) || ResourceClassifier.isPromotionName(name)
        }
        .forEach {
            it.textContent = "0.0dp"
            changed++
        }
    return changed
}

internal fun restorePrivateFrameworkReferences(resourceRoot: File): Int {
    var changed = 0
    resourceRoot.walkTopDown()
        .filter { it.isFile && it.extension == "xml" }
        .forEach { file ->
            val original = file.readText()
            val restored = original
                .replace("@android:", "@*android:")
                .replace(
                    Regex("\\s+android:(usesAssistData|usesAssistScreenshots|usesAssistStructureScreenContent)=\"[^\"]*\""),
                    "",
                )
            if (restored != original) {
                file.writeText(restored)
                changed++
            }
        }
    return changed
}

private fun Element.collapse() {
    setAndroidAttribute("layout_width", "0.0dp")
    setAndroidAttribute("layout_height", "0.0dp")
    setAndroidAttribute("minWidth", "0.0dp")
    setAndroidAttribute("minHeight", "0.0dp")
    setAndroidAttribute("visibility", "gone")
}

private fun Element.setAndroidAttribute(name: String, value: String) {
    val qualifiedName = "android:$name"
    if (hasAttribute(qualifiedName)) {
        setAttribute(qualifiedName, value)
    } else {
        setAttributeNS(ANDROID_NAMESPACE, qualifiedName, value)
    }
}

private fun Element.androidName() = androidAttribute("name")

private fun Element.androidAttribute(name: String): String =
    getAttributeNS(ANDROID_NAMESPACE, name).ifEmpty { getAttribute("android:$name") }

private fun Element.hasComponent(tag: String, name: String) = childElements().any {
    it.tagName == tag && it.androidName() == name
}

private fun Node.childElements(): List<Element> = (0 until childNodes.length)
    .mapNotNull { childNodes.item(it) as? Element }
