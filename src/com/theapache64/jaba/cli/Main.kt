package com.theapache64.jaba.cli

import com.google.gson.Gson
import com.theapache64.jaba.cli.models.Architectures
import com.theapache64.jaba.cli.models.Project
import com.theapache64.jaba.cli.utils.*
import java.io.File
import java.io.FileNotFoundException
import java.util.*

const val IS_DEBUG = false
const val ERROR_NOT_AN_ANDROID_PROJECT = "ERROR_NOT_AN_ANDROID_PROJECT"
const val ERROR_UNSUPPORTED_ARCH = "UNSUPPORTED_ARCH"
const val ERROR_NOT_KOTLIN_PROJECT = "NOT_KOTLIN_PROJECT"
const val JABA_API_BASE_URL = "http://theapache64.com/mock_api/get_json/jaba/"

class Main {

}


val jarFile = File(Main::class.java.protectionDomain.codeSource.location.toURI().path)
val currentDir: String = if (IS_DEBUG) "lab/jabroid" else System.getProperty("user.dir")

fun File.getPathFromCurrentDir(): String {
    return this.absolutePath.split("$currentDir/")[1]
}

const val COMMAND_PROVIDE_ACTIVITY_SUPPORT = "-pas"
const val COMMAND_PROVIDE_FRAGMENT_SUPPORT = "-paf"

/**
 * Magic starts from here
 */
fun main(args: Array<String>) {

    if (args.isNotEmpty()) {
        // it's not about project init
        when (val command = args[0]) {
            COMMAND_PROVIDE_ACTIVITY_SUPPORT -> {

                @Suppress("ConstantConditionIf")
                if (IS_DEBUG) {
                    logDoing("Cleaning lab...")
                    LabUtils.cleanForPAS()
                    logDone()
                }

                var componentName = args[1]
                val activityName = if (componentName.endsWith("Activity")) {
                    val activityName = componentName
                    componentName = componentName.replace("Activity", "")
                    activityName
                } else {
                    "${componentName}Activity"
                }

                val activityFileName = "${componentName}Activity.kt"
                println("Component Name : $componentName")
                println("Activity Name : $activityName")
                println("File Name : $activityFileName")
                println("Searching for $activityFileName in $currentDir")

                val matchingList = FileUtils.find(activityFileName, currentDir)
                if (matchingList.isNotEmpty()) {
                    if (matchingList.size == 1) {
                        val activityFile = matchingList.first()
                        println("Found : ${activityFile.getPathFromCurrentDir()}")

                        provideActivitySupport(activityFile, componentName)
                    } else {

                        println("Multiple files found, choose one")
                        matchingList.forEachIndexed { index, file ->
                            println("${index + 1}) ${file.getPathFromCurrentDir()}")
                        }
                        val scanner = Scanner(System.`in`)
                        val fileNum = scanner.nextInt()
                        val file = matchingList[fileNum - 1]
                        println("Chosen : ${file.getPathFromCurrentDir()}")
                        provideActivitySupport(file, componentName)

                    }
                } else {
                    error("$activityFileName not found in $currentDir")
                }

            }

            COMMAND_PROVIDE_FRAGMENT_SUPPORT -> {
                println("Fragment support coming soon...")
            }
            else -> {
                error("Unknown command $command")
            }
        }

    } else {
        // jaba's initial support
        performInitialProjectSetup()
    }


}

private fun provideActivitySupport(activityFile: File, componentName: String) {

    val projectFile = File("$currentDir/jaba_project.json")
    if (projectFile.exists()) {

        println("Decoding project JSON...")
        val projectJson = projectFile.readText()
        val project = Gson().fromJson(projectJson, Project::class.java)

        println("Project : ${project.name}")
        println("Package : ${project.packageName}")
        println()

        copyAssets(project)
        Jaba.provideActivitySupport(project, currentDir, activityFile, componentName)
        deleteAssets(project)

    } else {
        error("$currentDir is not a jaba project. Init jaba by running `jaba` in the project root")
    }


}

private fun performInitialProjectSetup() {

    if (IS_DEBUG) {
        logDoing("Cleaning lab...")
        LabUtils.clean()
        logDone()
    }


    // Current directory will be treated as an android project

    //val currentDir = "lab/jabroid"
    //val currentDir = "/home/theapache64/Documents/projects/MyApp"
    try {
        val androidUtils = AndroidUtils(currentDir)

        if (androidUtils.isAndroidProject()) {

            // Getting project name
            val projectName = androidUtils.provideProjectName()
            val packageName = androidUtils.providePackageName()

            if (androidUtils.isKotlinProject()) {


                println("Project : $projectName")
                println("Package : $packageName")
                println()
                println("Choose architecture")
                for (arch in Architectures.values().withIndex()) {
                    println("${arch.index + 1}) ${arch.value}")
                }

                val scanner = Scanner(System.`in`)
                val inputUtils = InputUtils.getInstance(scanner)

                // Asking architecture
                val totalArchs = Architectures.values().size
                val architecture = if (IS_DEBUG) 1 else inputUtils.getInt("Response", 1, totalArchs)
                val selArch = Architectures.values().get(architecture - 1)

                if (selArch == Architectures.MVP) {
                    failure(ERROR_UNSUPPORTED_ARCH, "MVP not supported yet!")
                    return
                }

                // Google Fonts
                val googleFontResponse =
                    if (IS_DEBUG) "yes" else inputUtils.getString("Do you need google fonts? (y/N)", true)
                val isNeedGoogleFont = isYes(googleFontResponse)

                // Network
                val networkResponse =
                    if (IS_DEBUG) "yes" else inputUtils.getString("Do you need network module ? (y/N)", true)
                val isNeedNetwork = isYes(networkResponse)

                var baseUrl: String? = null
                if (isNeedNetwork) {
                    baseUrl = if (IS_DEBUG) JABA_API_BASE_URL else inputUtils.getString(
                        "Enter base url : (empty to use default jaba api)",
                        false
                    )

                    if (baseUrl.trim().isEmpty()) {
                        baseUrl = JABA_API_BASE_URL
                    }
                }

                // Room support
                val roomResponse =
                    if (IS_DEBUG) "yes" else inputUtils.getString("Do you need local database (room) ? (y/N)", true)
                val isNeedRoomSupport = isYes(roomResponse)

                // Splash
                val splashResponse =
                    if (IS_DEBUG) "yes" else inputUtils.getString("Do you need splash screen? (y/N)", true)
                val isNeedSplashScreen = isYes(splashResponse)


                val isNeedLogInScreen = if (isNeedNetwork) {
                    val logInResponse =
                        if (IS_DEBUG) "yes" else inputUtils.getString("Do you need login screen? (y/N)", true)
                    isYes(logInResponse)
                } else {
                    false
                }

                var newMainName: String? = if (IS_DEBUG) "LabActivity" else
                    inputUtils.getString("Change default MainActivity name to : (empty to keep default)", false)
                if (newMainName!!.trim().isEmpty()) {
                    newMainName = null
                }

                if (newMainName != null) {
                    println("MainActivty -> $newMainName")
                } else {
                    println("Default will be set")
                }

                // Copy assets to current folder
                val project = Project(
                    projectName,
                    currentDir,
                    packageName,
                    architecture,
                    isNeedGoogleFont,
                    isNeedNetwork,
                    baseUrl,
                    isNeedRoomSupport,
                    isNeedSplashScreen,
                    isNeedLogInScreen,
                    newMainName
                )


                copyAssets(project)
                Jaba(project, androidUtils).build()
                deleteAssets(project)

                // Jaba(androidUtils, project).buildOld()

            } else {
                failure(ERROR_NOT_KOTLIN_PROJECT, "$currentDir is not a kotlin android project")
            }

        } else {
            notAnAndroidProject(currentDir)
        }

    } catch (e: FileNotFoundException) {
        e.printStackTrace()
        notAnAndroidProject(currentDir)
    }
}


private fun deleteAssets(project: Project) {
    if (!IS_DEBUG) {
        FileUtils.deleteDir("${project.dir}/assets")
    }
}

private fun copyAssets(project: Project) {
    if (!IS_DEBUG) {
        // Copy assets to current project folder from jar folder
        val assetsDir = File("${jarFile.parent}/assets")
        FileUtils.copyOneLevelDir(assetsDir, "${project.dir}/assets")
    }
}

fun notAnAndroidProject(currentDir: String) {
    // invalid android project
    failure(ERROR_NOT_AN_ANDROID_PROJECT, "$currentDir is not an android project")
}


fun isYes(response: String): Boolean = response.trim().toLowerCase().let {
    val isYes = it == "y" || it == "yes"
    println("Response : ${if (isYes) "yes" else "no"}")
    return@let isYes
}

fun failure(errorCode: String, message: String) {
    println("ERROR : $errorCode : $message")
}

