plugins {
    id("dev.kikugie.loom-back-compat")
}

val fairPlay = providers.gradleProperty("mod.flavor").orNull == "fair-play"
version = "${property("mod.version")}+${sc.current.version}" + if (fairPlay) "-fair-play" else ""
base.archivesName = property("mod.id") as String

sourceSets.main {
    java.exclude(
        if (fairPlay) "**/client/mixin/AnvilScreenMixin.java"
        else "**/client/mixin/FairPlayAnvilScreenMixin.java"
    )
    if (fairPlay) {
        java.exclude(
            "**/client/automation/**",
            "**/client/preset/**",
            "**/client/plan/**",
            "**/client/gui/AnvilPresetView.java",
            "**/client/mixin/AnvilSyncMixin.java",
            "**/client/mixin/AnvilExperienceMixin.java"
        )
    }
}

val targetJava = if (sc.current.parsed >= "26.1") 25 else 21

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    loomx.applyMojangMappings()

    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    modImplementation(
        "net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}"
    )

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json")
    runConfigs.all {
        runDirectory = rootProject.file("run")
    }
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.toVersion(targetJava)
    targetCompatibility = JavaVersion.toVersion(targetJava)
    toolchain.languageVersion = JavaLanguageVersion.of(26)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = targetJava
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    val replacements = mapOf(
        "version" to project.version.toString(),
        "mod_name" to if (fairPlay) "Enchantment Calculator (Fair Play Edition)" else "Enchantments Calculator",
        "mod_icon" to if (fairPlay) "icon_fair.png" else "icon.png",
        "mixin_screen" to if (fairPlay) "FairPlayAnvilScreenMixin" else "AnvilScreenMixin",
        "automation_mixins" to if (fairPlay) "" else ", \"AnvilSyncMixin\", \"AnvilExperienceMixin\"",
        "minecraft_version" to sc.properties["mod.mc_compat"],
        "loader_version" to project.property("deps.fabric_loader").toString(),
        "java_version" to targetJava.toString()
    )
    replacements.forEach { (key, value) -> inputs.property(key, value) }
    exclude(if (fairPlay) "assets/enchantmentcalculator/icon.png"
            else "assets/enchantmentcalculator/icon_fair.png")
    filesMatching(listOf("fabric.mod.json", "enchantmentcalculator.mixins.json")) {
        expand(replacements)
    }
}

tasks.withType<Jar>().configureEach {
    from(rootProject.file("LICENSE.txt")) {
        rename { "${it}_enchantmentcalculator" }
    }
}
