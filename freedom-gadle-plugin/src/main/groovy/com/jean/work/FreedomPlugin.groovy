package com.jean.work

import com.jean.work.extensions.FreedomExtension
import com.jean.work.taks.IncrementPatchTask
import com.jean.work.taks.ReadModulesTask
import groovy.io.FileType
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Created by rantianhua on 17/4/25.
 */
class FreedomPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

        project.extensions.add("freedom", FreedomExtension)

        project.afterEvaluate {

            project.android.applicationVariants.each { variant ->
                String variantName = variant.name.capitalize()

                ReadModulesTask readModulesTask = project.tasks.create("freedomRead${variantName}Modules", ReadModulesTask)
                readModulesTask.modulesFile = project.extensions.freedom.modulesFile

                IncrementPatchTask incrementPatchTask = project.tasks.create("freedomPatch${variantName}", IncrementPatchTask)
                incrementPatchTask.dependsOn readModulesTask
                incrementPatchTask.mustRunAfter readModulesTask

                variant.outputs.each { output ->
                    incrementPatchTask.buildApk = output.outputFile
                }

                def assembleTask = project.tasks.findByName("assemble${variantName}")
                readModulesTask.dependsOn assembleTask
                readModulesTask.mustRunAfter assembleTask

            }
        }
    }

//    private static int getMinSdkVersion(def mergedFlavor, String manifestPath) {
//        if (mergedFlavor.minSdkVersion != null) {
//            return mergedFlavor.minSdkVersion.apiLevel
//        } else {
//            return getMinSdkVersion(manifestPath)
//        }
//    }
//
//    private static int getMinSdkVersion(String manifestPath) {
//        def minSdkVersion = 0
//        def manifestFile = new File(manifestPath)
//        if (manifestFile.exists() && manifestFile.isFile()) {
//            def manifest = new XmlSlurper(false, false).parse(manifestFile)
//            minSdkVersion = manifest."uses-sdk"."@android:minSdkVersion".text()
//        }
//        return Integer.valueOf(minSdkVersion)
//    }
}
