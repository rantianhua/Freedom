package com.jean.work.taks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

/**
 * Created by rantianhua on 17/4/28.
 */
class FullPatchTask extends DefaultTask {

    def buildApkPath

    @TaskAction
    public void fullPatch() {
        if (buildApkPath == null) {
            throw new GradleException("buildApkPath is null")
        }

        println("this is a full patch")
    }
}
