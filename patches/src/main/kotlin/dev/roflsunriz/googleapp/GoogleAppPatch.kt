package dev.roflsunriz.googleapp

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.firstMethodOrNull
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch

private const val GOOGLE_APP_PACKAGE = "com.google.android.googlequicksearchbox"
private const val MEBIBYTE = 1024L * 1024L
internal const val MINIMUM_PATCHER_HEAP_MIB = 640L

internal fun requireSufficientPatcherHeap(maxMemoryBytes: Long = Runtime.getRuntime().maxMemory()) {
    val maximumHeapMib = maxMemoryBytes / MEBIBYTE
    if (maximumHeapMib >= MINIMUM_PATCHER_HEAP_MIB) return

    throw PatchException(
        "Google アプリに対して現在のPatcherヒープ（${maximumHeapMib} MiB）は不足しています。" +
            "ReVanced Managerの「設定」→「高度な設定」で「Patcherを別のプロセスで実行」を有効にし、" +
            "メモリ上限を700 MiB以上にしてください。 " +
            "The Google App requires at least a 700 MiB patcher heap. Enable Settings > Advanced > " +
            "Run Patcher in another process in ReVanced Manager.",
    )
}

internal val googleAppResourcesPatch = resourcePatch {
    compatibleWith(GOOGLE_APP_PACKAGE)

    apply {
        document("AndroidManifest.xml").use { document ->
            removeAdvertisingManifestEntries(document)
            installExtensionComponents(document)
        }

        get("res").listFiles()
            .orEmpty()
            .filter { it.isDirectory && (it.name == "layout" || it.name.startsWith("layout-")) }
            .forEach { layoutDirectory ->
                layoutDirectory.listFiles()
                    .orEmpty()
                    .filter { it.isFile && it.extension == "xml" }
                    .forEach { file ->
                        val relativePath = "res/${layoutDirectory.name}/${file.name}"
                        document(relativePath).use { document ->
                            if (ResourceClassifier.shouldCollapseLayout(file.nameWithoutExtension)) {
                                collapseLayout(document)
                            } else {
                                collapseNamedAdViews(document)
                            }
                        }
                    }
            }

        get("res").walkTopDown()
            .filter { it.isFile && it.name == "dimens.xml" && it.parentFile.name.startsWith("values") }
            .forEach { file ->
                val relativePath = file.relativeTo(get(".")).invariantSeparatorsPath
                document(relativePath).use(::zeroAdvertisingDimensions)
            }

        val icon = get("res/drawable/google_revanced_settings_icon.xml")
        icon.parentFile.mkdirs()
        icon.writeText(
            """
            <vector xmlns:android="http://schemas.android.com/apk/res/android"
                android:width="24dp"
                android:height="24dp"
                android:viewportWidth="24"
                android:viewportHeight="24"
                android:tint="?android:attr/textColorPrimary">
                <path
                    android:fillColor="@android:color/white"
                    android:pathData="M12,2L4,5.5V11c0,5.05 3.41,9.76 8,11 4.59,-1.24 8,-5.95 8,-11V5.5L12,2zM8.5,8h4.75a2.75,2.75 0,0 1,0 5.5H12L15.5,17h-2.83l-4.17,-4.17V17H6.5V8h2zM8.5,10v1.5h4.75a0.75,0.75 0,0 0,0 -1.5H8.5z" />
            </vector>
            """.trimIndent(),
        )

        restorePrivateFrameworkReferences(get("res"))
    }
}

@Suppress("unused")
val googleAppReVancedPatch = bytecodePatch(
    name = "Google ReVanced",
    description = "広告SDK通信、広告表示枠、Google アプリ内のセルフプロモーションを除去します。",
) {
    compatibleWith(GOOGLE_APP_PACKAGE)
    dependsOn(googleAppResourcesPatch)
    extendWith("extensions/googleapp.rve")

    apply {
        requireSufficientPatcherHeap()
        patchAdNetworkBoundaries()
        patchComposeAdContainers()
        firstMethodOrNull {
            definingClass == "Lcom/google/android/apps/search/googleapp/settingsui/SettingsActivity;" &&
                name == "onCreate" &&
                parameterTypes.singleOrNull() == "Landroid/os/Bundle;" &&
                returnType == "V"
        }?.addInstructions(
            0,
            "invoke-static {p0}, Lapp/revanced/extension/googleapp/SettingsInjector;->onActivityResumed(Landroid/app/Activity;)V",
        )
    }
}
