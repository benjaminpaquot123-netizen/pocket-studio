pluginManagement {
    repositories {
        google()
        // Google-hosted copy of Maven Central, tried first: Maven Central itself
        // answers false 404s (rate limit) to shared cloud addresses, and Gradle
        // never retries a 404. Anything missing here falls through to mavenCentral().
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        // Same mirror as in pluginManagement above.
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        mavenCentral()
    }
}

rootProject.name = "pocket-studio"

include(":app", ":engine", ":ui-components", ":project-model", ":separation")
