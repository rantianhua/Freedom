package com.jean.work

import org.gradle.api.Project

import java.security.DigestInputStream
import java.security.MessageDigest

/**
 * Created by rantianhua on 17/4/28.
 * some utility method for file operation
 */
class FileUtils {

    static def generateMD5(File file) {
        file.withInputStream {
            new DigestInputStream(it, MessageDigest.getInstance("MD5")).withStream {
                it.eachByte {}
                it.messageDigest.digest().encodeHex() as String
            }
        }
    }

    static def getFreedomCacheDir(Project project) {
        File cacheDir = new File(project.buildDir, Constants.FREEDOM_CACHE_DIR)
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        cacheDir
    }

    static def getFreedomNewFileStateCacheFile(Project project) {
        File cacheDir = getFreedomCacheDir(project)
        new File(cacheDir, Constants.FILE_NEW_STATE_CACHE)
    }

    static def getFreedomOldFilesStateCacheFile(Project project) {
        File cacheDir = getFreedomCacheDir(project)
        new File(cacheDir, Constants.FILE_OLD_STATE_CACHE)
    }

    static def haveOldFilesStates(Project project) {
        getFreedomOldFilesStateCacheFile(project).exists()
    }

    static def haveNewFilesStates(Project project) {
        getFreedomNewFileStateCacheFile(project).exists()
    }

    static def getDiffFilesStateCacheFile(Project project) {
        File cacheDir = getFreedomCacheDir(project)
        new File(cacheDir, Constants.FILE_DIFF_STATE_CACHE)
    }

    static def haveDiffFilesStates(Project project) {
        getDiffFilesStateCacheFile(project).exists()
    }

    static def getGenClassesPath(Project project) {
        File cacheDir = getFreedomCacheDir(project);
        File gen = new File(cacheDir, Constants.GEN_CLASSES_DIR)
        if (!gen.exists()) {
            gen.mkdirs()
        }
        gen.absolutePath
    }

    static def getGenDexesPath(Project project) {
        File cacheDir = getFreedomCacheDir(project);
        File gen = new File(cacheDir, Constants.GEN_DEX_DIR)
        if (!gen.exists()) {
            gen.mkdirs()
        }
        gen.absolutePath
    }

    static def readProjectSdkPath(Project project) {
        File local = project.rootProject.file("local.properties")
        String sdk
        local.eachLine {
            if (it.startsWith("sdk.dir")) {
                sdk = it.trim().split("=")[1]
            }
        }
        return sdk
    }

    static def getAndroidJarPath(Project project) {
        String sdkPath = readProjectSdkPath(project)
        sdkPath + File.separator + "platforms" + File.separator + project.android.getCompileSdkVersion() + File.separator + "android.jar"
    }

    static def getPackageName(String manifestPath) {
        def packageName = ""
        def manifestFile = new File(manifestPath)
        if (manifestFile.exists() && manifestFile.isFile()) {
            def manifest = new XmlSlurper(false, false).parse(manifestFile)
            packageName = manifest."@package".text()
        }
        return packageName
    }

    static void copyClassFile(File file, String packageName, Project project) {
        File parent = file.getParentFile()
        String parentPath = null
        while (parent != null) {
            if (parentPath == null) {
                parentPath = parent.getName()
            }else {
                parentPath = parent.getName() + File.separator + parentPath
            }
            if (parentPath.startsWith(packageName)) {
                break
            }
            parent = parent.getParentFile()
        }

        String cacheDir = getGenClassesPath(project)
        File classDir = new File(cacheDir, parentPath)
        if (!classDir.exists()) {
            classDir.mkdirs()
        }

        File newClassFile = new File(classDir, file.name)
        def srcStream = file.newDataInputStream()
        def dstStream = newClassFile.newDataOutputStream()
        dstStream << srcStream
        srcStream.close()
        dstStream.close()
    }
}
