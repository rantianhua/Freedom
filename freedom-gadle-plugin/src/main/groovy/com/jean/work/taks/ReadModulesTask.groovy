package com.jean.work.taks

import com.jean.work.Constants
import com.jean.work.FileUtils
import com.jean.work.model.FileMd5Model
import com.jean.work.model.ProjectChangeInfo
import groovy.io.FileType
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

/**
 * Created by rantianhua on 17/4/27.
 * read modules
 */
class ReadModulesTask extends DefaultTask {

    String modulesFile

    @TaskAction
    public void read() {
        checkParameter();

        List<String> modules = readModules(project.rootProject.file(modulesFile))

        calculateModuleFileMd5(modules)
    }

    /**
     * 遍历每个module里的源文件，记录文件路径和md5值
     * @param modules 所有的module
     */
    void calculateModuleFileMd5(List<String> modules) {

        JsonSlurper jsonSlurper = new JsonSlurper()
        def result = jsonSlurper.parseText('''{}''')

        modules.each {moduleName ->
            final pathName = moduleName + File.separator + Constants.ALL_SOURCE_DIR
            File moduleDir = project.rootProject.file(pathName)
            def moduleFilesMd5 = []
            moduleDir.traverse(type: FileType.FILES) {
                if (it.isFile()) {
                    String strMd5 = FileUtils.generateMD5(it)
                    String strFilePath = it.absolutePath

                    moduleFilesMd5.add(new FileMd5Model(strFilePath, strMd5))
                }
            }

            JsonBuilder jsonBuilder = new JsonBuilder()
            def subItem = jsonBuilder."${moduleName}" moduleFilesMd5, { FileMd5Model fileMd5Model ->
                filePath fileMd5Model.filePath
                md5 fileMd5Model.md5
            }

            result."${moduleName}" = subItem."${moduleName}"
        }

        JsonBuilder jsonBuilder = new JsonBuilder(result)
        saveNewModuleFilesInfo(jsonBuilder)
    }

    /**
     * 将新的文件的状态保存到文件
     * @param moduleFilesInfo 文件状态
     */
    void saveNewModuleFilesInfo(JsonBuilder moduleFilesInfo) {
        final String strModuleFilesInfo = moduleFilesInfo.toPrettyString()
        File file = FileUtils.getFreedomNewFileStateCacheFile(project)

        println("freedom write module files info to " + file.absolutePath)

        file.write(strModuleFilesInfo)

        calculateDiffModuleFilesInfo(file);
    }

    /**
     * 计算新旧两个文件信息的差量，得出一个待重新编译的文件集合
     */
    void calculateDiffModuleFilesInfo(File newStates) {
        if (FileUtils.haveOldFilesStates(project)) {
            def oldStates = FileUtils.getFreedomOldFilesStateCacheFile(project)

            def oldStatesMap = new JsonSlurper().parse(oldStates) as Map
            def newStatesMap = new JsonSlurper().parse(newStates) as Map

            def oldModules = oldStatesMap.keySet() as List
            def newModules = newStatesMap.keySet() as List

            def modifiedFiles = [:] as Map
            def deletedFiles = [:] as Map
            def addedFiles = [:] as Map

            //找出共有module中有变更的文件
            def commonModules = oldModules.intersect(newModules)
            println("common modules: " + commonModules)
            commonModules.each {
                modifiedFiles.put(it, [])
                deletedFiles.put(it, [])
                addedFiles.put(it, [])

                def oldFiles = oldStatesMap."${it}"
                def newFiles = newStatesMap."${it}"

                for (oldItem in oldFiles) {
                    boolean isOldFileInNew = false
                    for (newItem in newFiles) {
                        if (oldItem.filePath == newItem.filePath) {
                            isOldFileInNew = true
                            //比较md5值是否一样
                            if (oldItem.md5 != newItem.md5) {
                                //文件已修改
                                modifiedFiles.get(it).add(oldItem.filePath)
                            }
                            break
                        }
                    }
                    if (!isOldFileInNew) {
                        //该文件已经删除
                        deletedFiles.get(it).add(oldItem.filePath)
                    }
                }

                for (newItem in newFiles) {
                    boolean isNewFileInOld = false
                    for (oldItem in oldFiles) {
                        if (newItem.filePath == oldItem.filePath) {
                            isNewFileInOld = true
                            break
                        }
                    }
                    if (!isNewFileInOld) {
                        //该文件为新增文件
                        addedFiles.get(it).add(newItem.filePath)
                    }
                }
            }

            //已经删除的module
            def deletedModules = oldModules - commonModules
//            println("deleted modules: " + deletedModules)
            deletedModules.each {
                deletedFiles.put(it, [])

                def files = oldStatesMap.it
                for (item in files) {
                    deletedFiles.get(it).add(item.filePath)
                }
            }

            //新增的module
            def addedModules = newModules - commonModules
//            println("added modules: " + addedModules)
            addedModules.each {
                addedFiles.put(it, [])

                def files = newStatesMap.it
                for (item in files) {
                    addedFiles.get(it).add(item.filePath)
                }
            }

            //将文件信息存储在json结构中
            def allModules = commonModules + deletedModules + addedModules
            List<ProjectChangeInfo> projectChangeInfos = new ArrayList<>()
            allModules.each {
                String moduleState
                if (it in commonModules) {
                    moduleState = Constants.MODULE_STATE_MODIFY
                }else if (it in deletedModules) {
                    moduleState = Constants.MODULE_STATE_DELETE
                }else {
                    moduleState = Constants.MODULE_STATE_ADD
                }
                ProjectChangeInfo projectChangeInfo = new ProjectChangeInfo()
                projectChangeInfo.moduleName = it
                projectChangeInfo.moduleState = moduleState
                projectChangeInfo.addedFiles = addedFiles.get(it) ?: []
                projectChangeInfo.deletedFiles = deletedFiles.get(it) ?: []
                projectChangeInfo.modifiedFiles = modifiedFiles.get(it) ?: []

                projectChangeInfos.add(projectChangeInfo)
            }

//            println("added-->" + addedFiles)
//            println("deleted-->" + deletedFiles)
//            println("modified-->" + modifiedFiles)


            JsonBuilder json = new JsonBuilder()
            json projectChangeInfos, { ProjectChangeInfo info ->
                "${info.moduleName}" {
                    state info.moduleState
                    deleted info.deletedFiles as List
                    modified info.modifiedFiles as List
                    added info.addedFiles as List
                }
            }

            //保存到文件
            File file = FileUtils.getDiffFilesStateCacheFile(project)
            file.write(json.toPrettyString())
        }

        //新文件信息变为旧的文件信息
        newStates.renameTo(FileUtils.getFreedomOldFilesStateCacheFile(project))
    }

    void checkParameter() {
        if (modulesFile == null) {
            throw new GradleException("modulesFile cannot be null")
        }
    }

    /**
     * 读取项目所有的饿module
     * @param file 包含项目module信息的文件
     */
    static def readModules(File file) {
        List<String> list = []
        file.eachLine("UTF-8") { line ->
            line = line.trim()
            line = line.replaceAll(" ","")
            line = line.replaceAll("'","")
            line = line.replaceAll(",","")
            if (line.startsWith("include")) {
                String[] lineContents = line.split(":")
                for (int i = 1; i< lineContents.length; i++) {
                    list.add(lineContents[i])
                }
            }
        }
        list.remove("freedom-runtime")
        return list
    }
}
