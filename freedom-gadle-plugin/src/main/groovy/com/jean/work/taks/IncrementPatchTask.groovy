package com.jean.work.taks

import com.jean.work.Constants
import com.jean.work.FileUtils
import groovy.io.FileType
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.compile.JavaCompile

/**
 * Created by rantianhua on 17/4/27.
 * create a patch file
 */
class IncrementPatchTask extends DefaultTask {

    JavaCompile javaCompile


    IncrementPatchTask() {
        group = "freedom"
    }

    @TaskAction
    void patch() {
        boolean haveDiffFilesStates = FileUtils.haveDiffFilesStates(project)
        if (!haveDiffFilesStates) {
            throw new GradleException("please init freedom first")
        }

        File diffFiles = FileUtils.getDiffFilesStateCacheFile(project)
        def diffContent = new JsonSlurper().parse(diffFiles) as List

        checkIncrementable(diffContent)

        if (!FileUtils.haveDiffFilesStates(project)) {
            println("cannot have an incremental patch")
            return
        }

        def changedSourceFiles = [:]
        collectChangedSourceFiles(changedSourceFiles, diffContent)

        if (changedSourceFiles.size() == 0) {
            println("no file changes, stop patch")
            return
        }

        findRelativeClasses(changedSourceFiles)

        genDex()

        transDex()
    }

    void transDex() {
        File file = new File(FileUtils.getGenDexesPath(project))
        File[] dexs = file.listFiles()
        if (dexs.length == 0) {
            println("no dex file, skip transform")
            return
        }

        String content = dexToStr(dexs)

        transformContent(content)
    }

    def transformContent(String content) {
        def adbExe = project.android.getAdbExe().toString()
        for (i in 0..19) {
            int port = Constants.PORT_START + i
            println "${adbExe} forward tcp:${port} tcp:${port}".execute().text
            try {
                Socket socket = new Socket("127.0.0.1", port)
                socket.withStreams {input, output ->
                    output << content
                }
                println "${adbExe} forward --remove tcp:${port} tcp:${port}".execute().text
                break
            }catch (Exception e) {
                println("connect server error: " + e.message)
            }
            println "${adbExe} forward --remove tcp:${port} tcp:${port}".execute().text
        }
    }

    static String dexToStr(File[] files) {

        def map = [:]
        map.put("name", [])
        map.put("content", [])
        files.each {File f ->
            String name = f.name
            map.get("name").add(name)

            String content = f.newInputStream().bytes.encodeBase64()
            map.get("content").add(content)
        }

        return new JsonBuilder(map).toString()
    }

    def findRelativeClasses(LinkedHashMap changedSourceFiles) {
        File cacheClassesDir = new File(FileUtils.getGenClassesPath(project))
        if (cacheClassesDir.listFiles().size() > 0) {
            cacheClassesDir.deleteDir()
        }

        Set modules = changedSourceFiles.keySet()
        for (m in modules) {
            def files = changedSourceFiles.get(m) as List
            if (files.size() == 0) continue
            def fileNames = []
            for (f in files) {
                File file = new File(f as String)
                String name = file.name
                name = name.replace(".java", "")
                fileNames.add(name)
            }

            File moduleDir = project.rootProject.file(m)
            File classesDir = new File(moduleDir, Constants.MODULE_CLASSES_DIR)
            File flavorDir
            if (m == project.extensions.freedom.mainModule) {
                flavorDir = new File(classesDir, "debug")
            }else {
                flavorDir = new File(classesDir, "release")
            }

            File sourceDir = new File(moduleDir, Constants.ALL_SOURCE_DIR)
            File manifestFile = new File(sourceDir, Constants.MANIFEST_NAME)
            String packageName = FileUtils.getPackageName(manifestFile.absolutePath)

            packageName = packageName.replaceAll("\\.", File.separator)
            File modulePackageDir = new File(flavorDir, packageName)

            modulePackageDir.traverse(type: FileType.FILES) {
                if (it.isFile()) {
                    String className = it.name
                    className = className.replace(".class", "")
                    if(fileNames.contains(className)) {
                        //将该class文件复制到freedom目录下
                        FileUtils.copyClassFile(it, packageName, project)
                    }else {
                        if (className.contains("\$")) {
                            def names = className.split("\\\$")
                            if (fileNames.contains(names[0])) {
                                FileUtils.copyClassFile(it, packageName, project)
                            }
                        }
                    }
                }
            }
        }
    }

    static def collectChangedSourceFiles(Map changedSourceFiles, List diffContent) {

        for (item in diffContent) {
            Map module = item as Map
            def moduleNames = module.keySet()
            for (m in moduleNames) {
                def content = module.get(m)
                def added = content.added as List
                def deleted = content.deleted as List
                def modified = content.modified as List

                List<String> javaFiles = new ArrayList<>()

                def collect = added + modified
                for (file in collect) {
                    String path = file as String
                    if (path.endsWith(".java")) {
                        javaFiles.add(path)
                    }
                }

                if (javaFiles.size() > 0) {
                    changedSourceFiles.put(m, javaFiles)
                }
            }
        }
    }

    void genDex() {
        File dexDir = new File(FileUtils.getGenDexesPath(project))
        if (dexDir.listFiles().size() > 0) {
            dexDir.deleteDir()
        }

        def args = []
        args.add("dx")
        args.add("--dex")
        args.add("--no-optimize")
        args.add("--force-jumbo")
        args.add("--output=" + FileUtils.getGenDexesPath(project))
        args.add(FileUtils.getGenClassesPath(project))

        println "execute : " + args

        Process p = args.execute()
        printExecute(p)
    }

    void compileSourceFile(List list) {
        List<String> javaFiles = new ArrayList<>()

        for (item in list) {
            Map module = item as Map
            def moduleNames = module.keySet()
            for (m in moduleNames) {
                def content = module.get(m)
                def added = content.added as List
                def deleted = content.deleted as List
                def modified = content.modified as List

                def collect = added + modified
                for (file in collect) {
                    String path = file as String
                    if (path.endsWith(".java")) {
                        javaFiles.add(path)
                    }
                }
            }
        }

        File classesDir = new File(FileUtils.getGenClassesPath(project))
        if (classesDir.listFiles().size() > 0) {
            classesDir.deleteDir()
        }

        def args = []
        args.add("javac")
        args.add("-target")
        args.add("1.7")
        args.add("-source")
        args.add("1.7")
        args.add("-encoding")
        args.add("UTF-8")
        args.add("-cp")
        args.add(FileUtils.getAndroidJarPath(project) + ":"
                + "/Users/rantianhua/Library/Android/sdk/extras/android/m2repository/com/android/support/support-annotations/25.3.0/support-annotations-25.3.0.jar"
                + ":" + "/Users/rantianhua/Library/Android/sdk/extras/android/m2repository/com/android/support/design/25.3.0/design-25.3.0-sources.jar"
                + ":" + "/Users/rantianhua/Library/Android/sdk/extras/android/m2repository/com/android/support/design/25.3.0/appcompat-v7-25.3.0-sources.jar"
                + ":" + "/Users/rantianhua/bs/Freedom/FreedomSample/app/build/intermediates/classes/debug")
        args.addAll(javaFiles)
        args.add("-d")
        args.add(FileUtils.getGenClassesPath(project))

        println "execute : " + args

        Process p = args.execute()
        printExecute(p)
    }

    void checkIncrementable(List list) {

    }

    static void printExecute(Process p) {
        def out = new StringBuilder()
        def err = new StringBuilder()

        p.waitForProcessOutput(out, err)

        if (out.length() > 0) {
            println("$out")
        }

        if (err.length() > 0) {
            println("$err")
            System.exit(2)
        }
    }
}
