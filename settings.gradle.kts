pluginManagement {
    repositories {
//        Kotlin DSL配置阿里云镜像
//        maven { setUrl("https://maven.aliyun.com/repository/central") }
//        maven { setUrl("https://maven.aliyun.com/repository/jcenter") }
//        maven { setUrl("https://maven.aliyun.com/repository/google") }
//        maven { setUrl("https://maven.aliyun.com/repository/gradle-plugin") }
//        maven { setUrl("https://maven.aliyun.com/repository/public") }
//        maven { setUrl("https://jitpack.io") }

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
        // 阿里云镜像
//        maven { setUrl("https://maven.aliyun.com/repository/central") }
//        maven { setUrl("https://maven.aliyun.com/repository/jcenter") }
//        maven { setUrl("https://maven.aliyun.com/repository/google") }
//        maven { setUrl("https://maven.aliyun.com/repository/gradle-plugin") }
//        maven { setUrl("https://maven.aliyun.com/repository/public") }
//        maven { setUrl("https://jitpack.io") }
        // 兜底：保留官方仓库
        google()
        mavenCentral()
    }
}

rootProject.name = "MemeMaster"
include(":app")
 