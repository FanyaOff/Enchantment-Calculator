import org.gradle.kotlin.dsl.replace

plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.21.9"

val supportedVersions = listOf("1.21.8", "1.21.9", "1.21.10", "1.21.11", "26.1", "26.2", "26.3")
val variantBuildTasks = supportedVersions.map { ":$it:build" }

val buildDefaultVariants = tasks.register("buildDefaultVariants") {
    group = "build"
    description = "Build default jars for all supported Minecraft versions."
    dependsOn(variantBuildTasks)
}

val buildFairPlayVariants = tasks.register<Exec>("buildFairPlayVariants") {
    group = "build"
    description = "Build Fair Play jars for all supported Minecraft versions."
    val wrapperCommand = if (System.getProperty("os.name").startsWith("Windows")) {
        listOf("cmd", "/c", rootProject.file("gradlew.bat").absolutePath)
    } else {
        listOf(rootProject.file("gradlew").absolutePath)
    }
    commandLine(wrapperCommand + variantBuildTasks + listOf("-Pmod.flavor=fair-play"))
    mustRunAfter(buildDefaultVariants)
}

tasks.register("build") {
    group = "build"
    description = "Build both variants for every supported Minecraft version."
    dependsOn(buildDefaultVariants, buildFairPlayVariants)
}

stonecutter parameters {
    replacements {
        string {
            direction = eval(current.version, ">=1.21.11")
            replace("ResourceLocation", "Identifier")
        }
        string {
            direction = eval(current.version, ">=26.1")
            replace("GuiGraphics", "GuiGraphicsExtractor")
            replace("render(", "extractRenderState(")
            replace("renderTooltip(", "extractTooltip(")
            replace("ClickType", "ContainerInput")
            replace("handleInventoryMouseClick", "handleContainerInput")
            replace("renderLabels", "extractLabels")
            replace("drawCenteredString(", "centeredText(")
            replace("drawString(", "text(")
        }
    }
}
