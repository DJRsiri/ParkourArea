plugins {
    id("java-library")
    id("com.gradleup.shadow") version "8.3.5"
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.enginehub.org/repo/")           // WorldEdit
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") // PlaceholderAPI
    maven("https://repo.dmulloy2.net/repository/public/") // ProtocolLib
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    // WorldEdit/FAWE 软依赖：仅取 API 类，排除传递依赖（其锁定的旧版 Guava/Gson/FastUtil 会与 paper-api 冲突，
    // 运行时这些库由 Paper 服务端提供）。运行时兼容任意 7.x WE / FAWE。
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.3.9") { isTransitive = false }
    compileOnly("com.sk89q.worldedit:worldedit-core:7.3.9") { isTransitive = false }
    compileOnly("me.clip:placeholderapi:2.11.6") { isTransitive = false }
    compileOnly("com.comphenix.protocol:ProtocolLib:5.3.0") { isTransitive = false }

    implementation("org.xerial:sqlite-jdbc:3.46.1.3")
    implementation("com.zaxxer:HikariCP:6.2.1")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.assertj:assertj-core:3.26.3")
    // 单测用到 adventure 序列化器（ColorUtil 等）；compileOnly 不传导到测试类路径
    testImplementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    // Gradle 9 需显式声明 JUnit Platform launcher 才能运行测试
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

val javaToolchainService = extensions.getByType<JavaToolchainService>()

tasks {
    runServer {
        minecraftVersion("1.21.11")
        jvmArgs("-Xms2G", "-Xmx2G")
        // 本地调试服务端用 Java 25 启动：ProtocolLib 5.5.x dev 构建按 Java 25 编译（class 69），
        // 默认 toolchain 21 会 UnsupportedClassVersionError；仅影响本地 runServer，不影响产物
        javaLauncher.set(javaToolchainService.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(25))
        })
    }

    processResources {
        val props = mapOf("version" to version, "description" to project.description)
        // 把 expand 的属性声明为任务输入：version 变化时 processResources 不再被误判 UP-TO-DATE，
        // 否则 jar 内 plugin.yml 会停留在旧版本号
        inputs.properties(props.mapValues { it.value.toString() })
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    test {
        useJUnitPlatform()
    }

    // shadow 时把运行时依赖打进 jar；compileOnly 依赖（paper-api/WE/PAPI）由服务端提供
    jar {
        // 瘦 jar 与 shadowJar（classifier 为空）输出路径会撞名，须区分，否则执行顺序靠后的会覆盖胖 jar
        archiveClassifier.set("thin")
    }

    shadowJar {
        archiveClassifier.set("")
        mergeServiceFiles()
    }

    build {
        dependsOn(shadowJar)
    }
}
