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

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "desi-calculator-ai-pro"

include(
  ":app",
  // Legacy Home Theatre modules excluded from Desi Calculator AI PRO V2.0 build:
  // ":core",
  // ":device-compatibility",
  // ":audio-engine",
  // ":dj-console",
  // ":rgb-controller",
  // ":spatial-audio",
  // ":dolby-integration",
)
