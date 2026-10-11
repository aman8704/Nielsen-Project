plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}

tasks.register("printErrors") {
    doLast {
        val f = file("app/build/tmp/kapt3/stubs/debug/error.txt")
        println(f.readText())
    }
}