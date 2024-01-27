plugins {
    id("java")
    id("io.freefair.lombok") version "8.4"
}

group = "dev.badbird.scraper"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jsoup:jsoup:1.17.2")

    implementation("org.apache.poi:poi:5.2.0")
    implementation("org.apache.poi:poi-ooxml:5.2.0")
    implementation("org.jxls:jxls-jexcel:1.0.9")
    implementation("org.dhatim:fastexcel-reader:0.15.3")
    implementation("org.dhatim:fastexcel:0.15.3")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.apache.commons:commons-lang3:3.14.0")


    implementation("com.theokanning.openai-gpt3-java:service:0.18.2")
}
