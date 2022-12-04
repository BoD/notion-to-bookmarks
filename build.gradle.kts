import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.Dockerfile

plugins {
    kotlin("jvm")
    id("application")
    id("com.bmuschko.docker-java-application")
}

group = "org.jraf"
version = "1.0.0"

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://jitpack.io")
}

application {
    mainClass.set("org.jraf.notiontobookmark.main.MainKt")
}

dependencies {
    // Ktor
    implementation(Ktor.server.core)
    implementation(Ktor.server.netty)

    // Logback
    runtimeOnly("ch.qos.logback:logback-classic:_")

    // KNotion API
    implementation("com.github.petersamokhin:knotion-api:_")

    // JSON
    implementation(KotlinX.serialization.json)
}

docker {
    javaApplication {
        maintainer.set("BoD <BoD@JRAF.org>")
        ports.set(listOf(8080))
        images.add("bodlulu/${rootProject.name}:latest")
        jvmArgs.set(listOf("-Xms16m", "-Xmx128m"))
    }
    registryCredentials {
        username.set(System.getenv("DOCKER_USERNAME"))
        password.set(System.getenv("DOCKER_PASSWORD"))
    }
}

tasks.withType<DockerBuildImage> {
    platform.set("linux/amd64")
}

tasks.withType<Dockerfile> {
    environmentVariable("MALLOC_ARENA_MAX", "4")
}


// `./gradlew distZip` to create a zip distribution
// `./gradlew refreshVersions` to update dependencies
// `DOCKER_USERNAME=<your docker hub login> DOCKER_PASSWORD=<your docker hub password> ./gradlew dockerPushImage` to build and push the image
