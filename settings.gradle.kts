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

rootProject.name = "CYRUS"
include(":app")
include(":core")
include(":core-ui")
include(":database")
include(":file-manager")
include(":library")
include(":emulator-api")
include(":native-bridge")
include(":emulator-engines")
include(":emulator-manager")
include(":controller-manager")
include(":save-manager")
include(":bios-manager")
include(":audio-manager")
include(":external-display-manager")
include(":screen-casting-manager")
include(":settings")
