// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false
}

// Task to bump version via scripts/bump_version.sh
// Usage:
//   ./gradlew bumpVersion -PversionName=0.4.0 [-PversionCode=7] [-PwithChangelog=true] [-PchangelogDate=YYYY-MM-DD] [-PdryRun=true]
tasks.register<Exec>("bumpVersion") {
    val versionName = (project.findProperty("versionName") as String?)
        ?: throw GradleException("-PversionName is required, e.g. -PversionName=0.4.0")
    val versionCode = project.findProperty("versionCode") as String?
    val withChangelog = (project.findProperty("withChangelog") as String?)?.toBoolean() ?: false
    val changelogDate = project.findProperty("changelogDate") as String?
    val dryRun = (project.findProperty("dryRun") as String?)?.toBoolean() ?: false

    workingDir = project.rootDir

    val args = mutableListOf("scripts/bump_version.sh", "-v", versionName)
    if (versionCode != null) args.addAll(listOf("-c", versionCode))
    if (withChangelog) args.add("--changelog")
    if (changelogDate != null) args.addAll(listOf("-d", changelogDate))
    if (dryRun) args.add("-n")

    commandLine = listOf("bash") + args
    isIgnoreExitValue = false
}
