plugins {
    id("java")
    id("distribution")
    id("org.jetbrains.intellij.platform") version "2.1.0"
}

apply(from = rootProject.file("config.gradle.kts"))
val versions = extra["versions"] as Map<*, *>

group = "com.xiaobaicai.plugin"
version "${extra["projectVersion"]}"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}


dependencies {
    implementation(project(":plugin-core"))
    implementation("${versions["hutool-all"]}")
    annotationProcessor("${versions["lombok"]}")
    compileOnly("${versions["lombok"]}")
    testAnnotationProcessor("${versions["lombok"]}")
    testCompileOnly("${versions["lombok"]}")
    // 单测
    testCompileOnly("${versions["junit"]}")
    testImplementation("${versions["junit.jupiter.api"]}")
    testImplementation("${versions["junit.jupiter.engine"]}")

    intellijPlatform {
        intellijIdeaCommunity("2024.1")
        pluginVerifier()
        zipSigner()
        instrumentationTools()
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "241"
            untilBuild = "243.*"
        }
    }
    publishing {
        token = providers.environmentVariable("pluginToken")
    }
}

tasks.register<Copy>("copyJars") {
    val sourceDir = project(":plugin-agent").projectDir.resolve("build/libs")
    val targetDir = rootProject.projectDir.resolve("build/idea-sandbox/IC-2024.1/plugins/${rootProject.name}/lib")

    from(sourceDir) {
        include("**/*-all.jar")  // 只复制 .jar 文件
    }
    into(targetDir)

    // 打印复制操作的内容
    doLast {
        println("Copied .jar files from $sourceDir to $targetDir")
    }
}

tasks.named("buildPlugin") {
    dependsOn("copyJars")
}

tasks.named("build") {
    dependsOn(project(":plugin-agent").tasks.get("shadowJar"))
}

tasks.named("instrumentedJar") {
    mustRunAfter("copyJars")
}