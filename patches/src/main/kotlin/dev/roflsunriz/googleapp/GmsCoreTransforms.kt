package dev.roflsunriz.googleapp

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.net.URI

internal const val ORIGINAL_PACKAGE = "com.google.android.googlequicksearchbox"
internal const val REVANCED_PACKAGE = "app.revanced.android.googleapp"
internal const val GMS_CORE_VENDOR = "app.revanced"
internal const val GMS_CORE_PACKAGE = "$GMS_CORE_VENDOR.android.gms"
internal const val GOOGLE_APP_OAUTH_SIGNATURE_SHA1 = "38918a453d07199354f8b19af05ec6562ced5788"

internal object GmsCoreState {
    val appPermissions = mutableMapOf<String, String>()
    val appAuthorities = mutableMapOf<String, String>()

    fun reset() {
        appPermissions.clear()
        appAuthorities.clear()
    }
}

private val gmsPermissions = setOf(
    "com.google.android.providers.gsf.permission.READ_GSERVICES",
    "com.google.android.c2dm.permission.RECEIVE",
    "com.google.android.c2dm.permission.SEND",
    "com.google.android.gtalkservice.permission.GTALK_SERVICE",
    "com.google.android.googleapps.permission.GOOGLE_AUTH",
    "com.google.android.googleapps.permission.GOOGLE_AUTH.cp",
    "com.google.android.googleapps.permission.GOOGLE_AUTH.local",
    "com.google.android.googleapps.permission.GOOGLE_AUTH.mail",
    "com.google.android.googleapps.permission.GOOGLE_AUTH.writely",
    "com.google.android.gms.permission.ACTIVITY_RECOGNITION",
    "com.google.android.gms.permission.AD_ID_NOTIFICATION",
    "com.google.android.gms.auth.api.phone.permission.SEND",
    "com.google.android.gms.permission.CAR_INFORMATION",
    "com.google.android.gms.permission.CAR_SPEED",
    "com.google.android.gms.permission.CAR_FUEL",
    "com.google.android.gms.permission.CAR_MILEAGE",
    "com.google.android.gms.permission.CAR_VENDOR_EXTENSION",
    "com.google.android.gms.locationsharingreporter.periodic.STATUS_UPDATE",
    "com.google.android.gms.auth.permission.GOOGLE_ACCOUNT_CHANGE",
)

private val gmsAuthorities = setOf(
    "com.google.android.gms.fileprovider",
    "com.google.android.gms.auth.accounts",
    "com.google.android.gms.chimera",
    "com.google.android.gms.fonts",
    "com.google.android.gms.phenotype",
    "com.google.android.gsf.gservices",
    "com.google.settings",
    "subscribedfeeds",
)

private val gmsCoreServiceActionOverrides = mapOf(
    "com.google.android.gms.phenotype.service.START" to
        "$GMS_CORE_PACKAGE.phenotype.service.START",
)

internal fun configureGmsCoreManifest(document: Document) {
    GmsCoreState.reset()
    val manifest = document.documentElement
    val application = manifest.childElements().first { it.tagName == "application" }

    manifest.getElementsByTagName("permission").elements().forEach { permission ->
        val original = permission.androidAttribute("name")
        if (original.isEmpty()) return@forEach
        val replacement = cloneOwnedName(original)
        GmsCoreState.appPermissions[original] = replacement
        permission.setAttribute("android:name", replacement)
    }

    manifest.getElementsByTagName("uses-permission").elements().forEach { permission ->
        val original = permission.androidAttribute("name")
        val replacement = GmsCoreState.appPermissions[original]
            ?: transformGmsString(original)
            ?: return@forEach
        permission.setAttribute("android:name", replacement)
    }

    manifest.getElementsByTagName("provider").elements().forEach { provider ->
        val authorities = provider.androidAttribute("authorities")
        if (authorities.isEmpty() || authorities.startsWith("@")) return@forEach
        val replacement = authorities.split(';').joinToString(";") { authority ->
            transformGmsString(authority) ?: GmsCoreState.appAuthorities.getOrPut(authority) {
                cloneOwnedName(authority)
            }
        }
        provider.setAttribute("android:authorities", replacement)
    }

    manifest.getElementsByTagName("application").elements().plus(
        manifest.getElementsByTagName("activity").elements(),
    ).plus(
        manifest.getElementsByTagName("activity-alias").elements(),
    ).plus(
        manifest.getElementsByTagName("service").elements(),
    ).plus(
        manifest.getElementsByTagName("receiver").elements(),
    ).plus(
        manifest.getElementsByTagName("provider").elements(),
    ).forEach { component ->
        listOf("permission", "readPermission", "writePermission").forEach { attribute ->
            val original = component.androidAttribute(attribute)
            val replacement = GmsCoreState.appPermissions[original]
                ?: transformGmsString(original)
                ?: return@forEach
            component.setAttribute("android:$attribute", replacement)
        }
    }

    manifest.setAttribute("package", REVANCED_PACKAGE)
    application.setAttribute("android:label", "Google ReVanced")

    val queries = manifest.getElementsByTagName("queries").item(0) as? Element
        ?: document.createElement("queries").also { manifest.insertBefore(it, application) }
    val hasGmsCoreQuery = queries.childElements().any {
        it.tagName == "package" && it.androidAttribute("name") == GMS_CORE_PACKAGE
    }
    if (!hasGmsCoreQuery) {
        queries.appendChild(document.createElement("package").apply {
            setAttribute("android:name", GMS_CORE_PACKAGE)
        })
    }

    addMetadata(document, application, "$GMS_CORE_PACKAGE.SPOOFED_PACKAGE_NAME", ORIGINAL_PACKAGE)
    addMetadata(
        document,
        application,
        "$GMS_CORE_PACKAGE.SPOOFED_PACKAGE_SIGNATURE",
        GOOGLE_APP_OAUTH_SIGNATURE_SHA1,
    )
    addMetadata(document, application, "app.revanced.MICROG_PACKAGE_NAME", GMS_CORE_PACKAGE)
}

internal fun transformGmsString(value: String): String? {
    if (value.isEmpty()) return null
    return when {
        value == "com.google" -> GMS_CORE_VENDOR
        value == "com.google.android.gms" -> GMS_CORE_PACKAGE
        value in gmsCoreServiceActionOverrides -> gmsCoreServiceActionOverrides.getValue(value)
        value in gmsPermissions -> value.replace("com.google", GMS_CORE_VENDOR)
        value in gmsAuthorities -> if (value.startsWith("com.google")) {
            value.replace("com.google", GMS_CORE_VENDOR)
        } else {
            "$GMS_CORE_VENDOR.$value"
        }
        value in GmsCoreState.appPermissions -> GmsCoreState.appPermissions.getValue(value)
        value in GmsCoreState.appAuthorities -> GmsCoreState.appAuthorities.getValue(value)
        value.startsWith("content://") -> transformContentAuthority(value)
        else -> null
    }
}

private fun transformContentAuthority(value: String): String? = runCatching {
    val authority = URI.create(value).authority ?: return@runCatching null
    val replacement = transformGmsString(authority) ?: return@runCatching null
    value.replaceFirst(authority, replacement)
}.getOrNull()

private fun cloneOwnedName(value: String) = if (value.startsWith(ORIGINAL_PACKAGE)) {
    value.replaceFirst(ORIGINAL_PACKAGE, REVANCED_PACKAGE)
} else if (value.startsWith(REVANCED_PACKAGE)) {
    value
} else {
    "$REVANCED_PACKAGE.$value"
}

private fun addMetadata(document: Document, application: Element, name: String, value: String) {
    val existing = application.childElements().firstOrNull {
        it.tagName == "meta-data" && it.androidAttribute("name") == name
    }
    (existing ?: document.createElement("meta-data").also(application::appendChild)).apply {
        setAttribute("android:name", name)
        setAttribute("android:value", value)
    }
}

private fun Element.androidAttribute(name: String): String =
    getAttributeNS(ANDROID_NAMESPACE, name).ifEmpty { getAttribute("android:$name") }

private fun Node.childElements(): List<Element> = (0 until childNodes.length)
    .mapNotNull { childNodes.item(it) as? Element }

private fun org.w3c.dom.NodeList.elements(): List<Element> = (0 until length)
    .mapNotNull { item(it) as? Element }
