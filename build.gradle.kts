plugins {
    kotlin("jvm") version "2.1.21" apply false
    kotlin("plugin.serialization") version "2.1.21" apply false
}

/** Latest reachable git tag without the leading `v`; null when the repository has no tags. */
fun latestGitTag(): String? = runCatching {
    val process = ProcessBuilder("git", "describe", "--tags", "--abbrev=0")
        .directory(rootDir)
        .redirectErrorStream(true)
        .start()
    val tag = process.inputStream.bufferedReader().readText().trim()
    if (process.waitFor() == 0) tag.removePrefix("v").takeIf { it.isNotBlank() } else null
}.getOrNull()

/**
 * -PcatanVersion=… > RELEASE_VERSION > latest git tag > default. Tag the release (and fetch tags)
 * before building so the artifact name matches the GitHub release.
 */
val projectVersion = providers.gradleProperty("catanVersion")
    .orElse(providers.environmentVariable("RELEASE_VERSION"))
    .getOrElse(latestGitTag() ?: "0.1.0")

allprojects {
    group = "com.hmhamster"
    version = projectVersion
}

/**
 * Development build: regenerates the front model types, builds the Vue front and the game plugin jar,
 * which the launcher's DEV watcher picks up automatically.
 */
val devFront = tasks.register<Exec>("devFront") {
    group = "development"
    description = "Builds the Vue front into front/dist (needs npm)"
    dependsOn(":back:generateTs")
    workingDir = projectDir
    val npm = if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
        listOf("cmd", "/c", "npm")
    } else {
        listOf("npm")
    }
    commandLine(npm + listOf("run", "build-front"))
}

tasks.register("devBuild") {
    group = "development"
    description = "Regenerates the model types, builds the front and the game plugin jar"
    dependsOn(":back:generateTs", devFront, ":back:jar")
}

/** Release build: front + dual-format game jar (with classes.dex), zipped for a GitHub release. */
tasks.register("releaseBuild") {
    group = "release"
    description = "Builds back/build/dist/catan-<version>.zip (front, jar and classes.dex)"
    dependsOn(":back:releaseZip")
}
