pluginManagement {
    repositories {
        google()
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

rootProject.name = "sunset-chasers-android"

include(":app")
include(":core:model")
include(":core:network")
include(":core:database")
include(":core:datastore")
include(":core:designsystem")
include(":feature:forecast")
include(":feature:favorites")
include(":feature:settings")
