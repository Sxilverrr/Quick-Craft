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

val jeiVersion = "19.44.0.403"

version = "${mod.version}-$minecraft-$loader"
base {
    archivesName.set(mod.id)
}
architectury {
    platformSetupLoomIde()
    neoForge()
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
    get("developmentNeoForge").extendsFrom(commonBundle)
}

repositories {
    maven("https://maven.neoforged.net/releases/")
    mavenCentral()
    maven("https://maven.blamejared.com")
    maven("https://modmaven.dev")
    maven("https://cursemaven.com") { name = "CurseMaven" }
    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
    strictMaven("https://maven.theillusivec4.top", "Curios", "top.theillusivec4.curios")
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft")
    mappings(loom.officialMojangMappings())
    "neoForge"("net.neoforged:neoforge:${common.mod.dep("neoforge_loader")}")

    modCompileOnly("curse.maven:jei-238222:8678372")
    modRuntimeOnly("curse.maven:jei-238222:8678372")

    compileOnly("curse.maven:applied-energistics-2-223794:7027323")
    compileOnly("curse.maven:projecte-226410:6611984")
    modCompileOnly("top.theillusivec4.curios:curios-neoforge:9.5.1+1.21.1")
    compileOnly("curse.maven:refined-storage-243076:8211701")
    modCompileOnly("maven.modrinth:emi:1.1.24+1.21.1+neoforge")
    modCompileOnly("curse.maven:just-enough-resources-240630:6174588")
    modCompileOnly("maven.modrinth:sophisticated-core:1.21.1-1.4.88.2283")
    modCompileOnly("maven.modrinth:sophisticated-backpacks:1.21.1-3.25.77.2086")
    modCompileOnly("maven.modrinth:sophisticated-storage:1.21.1-1.5.91.2127")
    modCompileOnly("maven.modrinth:travelersbackpack:1.21.1-10.1.39")

    commonBundle(project(common.path, "namedElements")) { isTransitive = false }
    shadowBundle(project(common.path, "transformProductionNeoForge")) { isTransitive = false }
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
    properties(listOf("META-INF/neoforge.mods.toml", "pack.mcmeta"),
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
