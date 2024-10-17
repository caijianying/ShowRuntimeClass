import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "1.9.23"
    id("java")
    id("com.github.johnrengelman.shadow") version ("8.1.1")
    id("distribution")
}


apply(from = rootProject.file("config.gradle.kts"))
val versions = extra["versions"] as Map<*, *>
var projectVersion = "${extra["projectVersion"]}"

group = "com.xiaobaicai.plugin"
version "${projectVersion}"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":plugin-core"))
    implementation("${versions["hutool-all"]}")
    implementation("${versions["slf4j"]}")
    compileOnly("${versions["lombok"]}")
    testAnnotationProcessor("${versions["lombok"]}")
    testCompileOnly("${versions["lombok"]}")
    // 单测
    testCompileOnly("${versions["junit"]}")
    testImplementation("${versions["junit.jupiter.api"]}")
    testImplementation("${versions["junit.jupiter.engine"]}")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}

tasks.withType<ShadowJar> {
    manifest {
        attributes(
            "Implementation-Version" to "${projectVersion}",
            "Implementation-Title" to rootProject.name,
            "Manifest-Version" to "${projectVersion}",
            "Premain-Class" to "com.xiaobaicai.plugin.agent.PluginAgent",
            "Agent-Class" to "com.xiaobaicai.plugin.agent.PluginAgent",
            "Can-Redefine-Classes" to true,
            "Can-Retransform-Classes" to true
        )
    }
}

tasks.register("prepareKotlinBuildScriptModel") {
    // No specific action required
}

// 设置 build 任务依赖于 shadowJar 任务
tasks.named("build") {
    dependsOn("shadowJar")
}