group = "dev.roflsunriz.googleapp.revanced"

patches {
    about {
        name = "Google App ReVanced Patches"
        description = "広告通信、広告枠、Google アプリ内のセルフプロモーションを除去するパッチ"
        source = "https://github.com/roflsunriz/google-app-revanced"
        author = "google-app-revanced contributors"
        contact = "https://github.com/roflsunriz/google-app-revanced/issues"
        website = "https://github.com/roflsunriz/google-app-revanced"
        license = "MIT"
    }
}

dependencies {
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
