// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // AGP 9 provides built-in Kotlin compilation, which is what EVHardware's library module
    // relies on — it applies no Kotlin plugin of its own. The library is on the classpath so
    // the :evhardware subproject can resolve `com.android.library`.
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
}