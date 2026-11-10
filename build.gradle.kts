plugins {
    id("java")
}

group = "com.ll"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")

    runtimeOnly("com.mysql:mysql-connector-j:9.5.0")

    testImplementation("org.assertj:assertj-core:3.27.6")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.20.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.20.1")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

// 과제 요구사항: JDK 21 기준으로 컴파일한다. (설치된 JDK 가 더 높아도 21 문법·API 로 제한)
tasks.withType<JavaCompile> {
    options.release = 21
}
