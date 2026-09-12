@file:Suppress("UnstableApiUsage")

plugins {
    id("dev.architectury.loom")
    id("architectury-plugin")
    id("com.github.johnrengelman.shadow")
}

val loader = prop("loom.platform")!!
val minecraft: String = stonecutter.current.version
val common: Project = requireNotNull(stonecutter.node.sibling("")?.project) {
    "No common project for $project"
}

val jeiVersion = "15.20.0.112"

version = "${mod.version}-$minecraft-$loader"
base {
    archivesName.set(mod.id)
}
architectury {
    platformSetupLoomIde()
    forge()
}

val commonBundle: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

val shadowBundle: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

configurations {
    compileClasspath.get().extendsFrom(commonBundle)
    runtimeClasspath.get().extendsFrom(commonBundle)
    get("developmentForge").extendsFrom(commonBundle)
}

repositories {
    maven("https://maven.minecraftforge.net")
    maven("https://maven.blamejared.com")
    maven("https://modmaven.dev")
    maven("https://cursemaven.com") { name = "CurseMaven" }
    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
    strictMaven("https://maven.theillusivec4.top", "Curios", "top.theillusivec4.curios")
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft")
    mappings(loom.officialMojangMappings())
    "forge"("net.minecraftforge:forge:$minecraft-${common.mod.dep("forge_loader")}")

    modCompileOnly("curse.maven:jei-238222:6600311")
    modRuntimeOnly("curse.maven:jei-238222:6600311")

    modCompileOnly("maven.modrinth:emi:1.1.24+1.20.1+forge")

    modCompileOnly("curse.maven:just-enough-resources-240630:5057220")

    modCompileOnly("curse.maven:projecte-226410:4901949")
    modCompileOnly("top.theillusivec4.curios:curios-forge:5.14.1+1.20.1")

    modCompileOnly("curse.maven:applied-energistics-2-223794:7148487")
    modCompileOnly("curse.maven:refined-storage-243076:4844585")
    modCompileOnly("curse.maven:toms-storage-378609:6418133")
    modCompileOnly("maven.modrinth:sophisticated-core:1.20.1-1.3.65.2126")
    modCompileOnly("maven.modrinth:sophisticated-backpacks:1.20.1-3.24.59.1960")
    modCompileOnly("maven.modrinth:sophisticated-storage:1.20.1-1.4.86.2131")
    modCompileOnly("maven.modrinth:travelersbackpack:1.20.1-9.1.57")

    commonBundle(project(common.path, "namedElements")) { isTransitive = false }
    shadowBundle(project(common.path, "transformProductionForge")) { isTransitive = false }
}

loom {
    runConfigs.all {
        isIdeConfigGenerated = true
        runDir = "../../../run"
    }
}

java {
    withSourcesJar()
    val java = if (stonecutter.eval(minecraft, ">=1.20.5"))
        JavaVersion.VERSION_21 else JavaVersion.VERSION_17
    targetCompatibility = java
    sourceCompatibility = java
}

tasks.jar {
    archiveClassifier = "dev"
}

tasks.remapJar {
    input = tasks.shadowJar.get().archiveFile
    archiveClassifier = null
    dependsOn(tasks.shadowJar)
}

tasks.shadowJar {
    configurations = listOf(shadowBundle)
    archiveClassifier = "dev-shadow"
    exclude("fabric.mod.json", "architectury.common.json")
}

tasks.processResources {
    properties(listOf("META-INF/mods.toml", "pack.mcmeta"),
        "id" to mod.id,
        "name" to mod.name,
        "version" to mod.version,
        "minecraft" to common.mod.prop("mc_dep_forgelike")
    )
}

tasks.build {
    group = "versioned"
    description = "Must run through 'chiseledBuild'"
}

tasks.register<Copy>("buildAndCollect") {
    group = "versioned"
    description = "Must run through 'chiseledBuild'"
    from(tasks.remapJar.get().archiveFile, tasks.remapSourcesJar.get().archiveFile)
    into(rootProject.layout.buildDirectory.file("libs/${mod.version}/$loader"))
    dependsOn("build")
}
