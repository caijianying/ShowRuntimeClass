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
    annotationProcessor("org.projectlombok:lombok:1.18.30")
    compileOnly("org.projectlombok:lombok:1.18.30")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.30")
    testCompileOnly("org.projectlombok:lombok:1.18.30")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}