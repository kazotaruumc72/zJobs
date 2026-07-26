rootProject.name = "zJobs"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include("API")

file("Hooks").listFiles()?.forEach { file ->
    if (file.isDirectory and !file.name.equals("build") and !file.name.startsWith(".")) {
        println("Include Hooks:${file.name}")
        include(":Hooks:${file.name}")
    }
}