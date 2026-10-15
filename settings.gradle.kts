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

rootProject.name = "fitpersonal"
include(":app")
include(":contrato")
// O app do aluno agora é o modo "aluno" do app principal (app/.../aluno). O módulo :aluno foi aposentado.
