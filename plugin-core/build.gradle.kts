plugins {
    kotlin("jvm") version "1.9.23"
}


apply(from = rootProject.file("config.gradle.kts"))
val versions = extra["versions"] as Map<*, *>


group = "com.xiaobaicai.plugin"
version = "${extra["projectVersion"]}"

repositories {
    mavenCentral()
}

dependencies {
    annotationProcessor("${versions["lombok"]}")
    compileOnly("${versions["lombok"]}")
    testAnnotationProcessor("${versions["lombok"]}")
    testCompileOnly("${versions["lombok"]}")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}