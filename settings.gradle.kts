pluginManagement {
    val isCi = System.getenv("GITHUB_ACTIONS") == "true" || System.getenv("CI") == "true"
    repositories {
        if (isCi) {
            google()
            mavenCentral()
            gradlePluginPortal()
        } else {
            maven { url = uri("https://maven.aliyun.com/repository/google") }
            maven { url = uri("https://maven.aliyun.com/repository/public") }
            maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
            gradlePluginPortal()
            mavenCentral()
            google()
        }
    }
}
dependencyResolutionManagement {
    val isCi = System.getenv("GITHUB_ACTIONS") == "true" || System.getenv("CI") == "true"
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        if (isCi) {
            google()
            mavenCentral()
        } else {
            maven { url = uri("https://maven.aliyun.com/repository/google") }
            maven { url = uri("https://maven.aliyun.com/repository/public") }
            mavenCentral()
            google()
        }
    }
}

rootProject.name = "AutoFlix"
include(":app")
