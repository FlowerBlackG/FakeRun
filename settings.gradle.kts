pluginManagement {
    repositories {
	    maven { url = uri("https://mirrors.cloud.tencent.com/maven/") }
        maven { url = uri("https://mirrors.huaweicloud.com/repository/maven/") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://mirrors.cloud.tencent.com/maven/") }
        maven { url = uri("https://mirrors.huaweicloud.com/repository/maven/") }
        google()
        mavenCentral()
    }
}

rootProject.name = "Fake Run"
include(":app")
