// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    // No Android or Kotlin plugins should be applied at the root level.
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
