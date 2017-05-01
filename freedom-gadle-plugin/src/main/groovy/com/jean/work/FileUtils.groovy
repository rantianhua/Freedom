package com.jean.work

import org.apache.tools.ant.taskdefs.Zip
import org.gradle.api.Project

import java.nio.charset.Charset
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

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

    static def getTmpApkPath(Project project) {
        File cacheDir = getFreedomCacheDir(project)
        File tmpApkDir = new File(cacheDir, Constants.TMP_APK_DIR)
        if (!tmpApkDir.exists()) {
            tmpApkDir.mkdirs()
        }
        return tmpApkDir.absolutePath
    }

    static def copyApkFile(File apk, Project project) {
        File tmpApkDir = new File(getTmpApkPath(project))

        File tmpApkFile = new File(tmpApkDir, apk.name)

        new AntBuilder().copy(file: "$apk.canonicalPath", tofile: "$tmpApkFile.canonicalPath")

        return tmpApkFile
    }

    static void unZipAPk(String fileName, String filePath) throws IOException {
        def zipFile = new ZipFile(new File(fileName), Charset.forName("UTF-8"))
        zipFile.entries().each {
            if (!it.isDirectory()) {
                def out = new File(filePath, it.name)
                new File(out.getParent()).mkdirs()

                def srcStream = new DataInputStream(zipFile.getInputStream(it))
                def dstStream = out.newDataOutputStream()
                dstStream << srcStream

                srcStream.close()
                dstStream.close()
            }
        }
    }

    static String getTmpApkUnzipPath(Project project) {
        File tmpOutput = new File(getTmpApkPath(project), Constants.APK_OUTPUT_DIR)
        return tmpOutput.absolutePath
    }

    static File getZipResPatchFile(Project project) {
        File tmpApkDir = new File(getTmpApkPath(project))
        File resZipFile = new File(tmpApkDir, Constants.RES_ZIP_PATCH_NAME)
        return resZipFile
    }

    static def zipDir(File filesDir, File zipFile) {
        List<File> fileList = new ArrayList<>();
        filesDir.listFiles().each {
            fileList.add(it)
        }
        zipFiles(fileList, zipFile)
    }


    static void zipFiles(Collection<File> resFileList, File zipPath) throws IOException {
        ZipOutputStream zipout = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipPath), 2048));
        for (File resFile : resFileList) {
            if (resFile.exists()) {
                zipFile(resFile, zipout, "");
            }
        }
        zipout.close();
    }

    static void zipFile(File resFile, ZipOutputStream zipout, String rootpath) throws IOException {
        rootpath = rootpath + (rootpath.trim().length() == 0 ? "" : File.separator) + resFile.getName()
        if (resFile.isDirectory()) {
            File[] fileList = resFile.listFiles()
            for (File file : fileList) {
                zipFile(file, zipout, rootpath)
            }
        } else {
            final byte[] fileContents = readContents(resFile)
            if (rootpath.contains("\\")) {
                rootpath = rootpath.replace("\\", "/")
            }
            ZipEntry entry = new ZipEntry(rootpath);
            entry.setMethod(ZipEntry.DEFLATED);
            zipout.putNextEntry(entry)
            zipout.write(fileContents)
            zipout.flush()
            zipout.closeEntry()
        }
    }

    private static byte[] readContents(final File file) throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream()
        final int bufferSize = 2048
        try {
            final FileInputStream inputStream = new FileInputStream(file)
            final BufferedInputStream bIn = new BufferedInputStream(inputStream)
            int length
            byte[] buffer = new byte[bufferSize]
            byte[] bufferCopy
            while ((length = bIn.read(buffer, 0, bufferSize)) != -1) {
                bufferCopy = new byte[length]
                System.arraycopy(buffer, 0, bufferCopy, 0, length)
                output.write(bufferCopy)
            }
            bIn.close()
        } finally {
            output.close()
        }
        return output.toByteArray()
    }

}
