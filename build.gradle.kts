import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    kotlin("jvm") version "1.3.70"
    id("application")
    id("com.github.ben-manes.versions") version "0.28.0"
}

group = "org.jraf"
version = "1.0.0"

repositories {
    mavenLocal()
    jcenter()
    mavenCentral()
//    maven("https://dl.bintray.com/bod/JRAF")
    maven("https://jitpack.io")
}

val versions = mapOf(
    "gradle" to "6.2.2",
//    "ktor" to "1.3.1",
    "ktor" to "1.2.0",
    "klibappstorerating" to "1.1.0",
    "logback" to "1.2.3",
    "kotlinxHtml" to "0.7.1",
//    "kNotionApi" to "1.0.0",
    "kNotionApi" to "0.0.3",
    "json" to "20190722"
)

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }

    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }

    wrapper {
        distributionType = Wrapper.DistributionType.ALL
        gradleVersion = versions["gradle"]
    }
}

tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        candidate.version.contains("alpha", true)
    }
}

tasks.register("stage") {
    dependsOn(":installDist")
}

application {
    mainClassName = "org.jraf.notiontobookmark.main.MainKt"
}

dependencies {
    // Kotlin
    implementation(kotlin("stdlib-jdk8"))

    // Ktor
    implementation("io.ktor:ktor-server-core:${versions["ktor"]}")
    implementation("io.ktor:ktor-server-netty:${versions["ktor"]}")

    // Logback
    runtimeOnly("ch.qos.logback:logback-classic:${versions["logback"]}")

    // Kotlinx Html
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:${versions["kotlinxHtml"]}")

    // KNotion API
    implementation("com.github.petersamokhin:knotion-api:${versions["kNotionApi"]}")
//    implementation("com.petersamokhin:notionapi:${versions["kNotionApi"]}")

    // JSON
    implementation("org.json:json:${versions["json"]}")
}

// Run `./gradlew distZip` to create a zip distribution
