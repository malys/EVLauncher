pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "EVLauncher"
include(":app")

// Shared vehicle layer, as a git submodule tracking the HEAD of EVHardware master.
// The launcher consumes its read-only telemetry API only; see AGENTS.md.
include(":evhardware")
project(":evhardware").projectDir = file("EVHardware/lib")