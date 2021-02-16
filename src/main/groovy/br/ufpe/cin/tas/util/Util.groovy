package br.ufpe.cin.tas.util

import br.ufpe.cin.tas.search.task.Task
import br.ufpe.cin.tas.search.task.merge.MergeTask
import groovy.util.logging.Slf4j

import java.util.regex.Matcher

@Slf4j
class Util {

    static deleteFile(String filename){
        def file = new File(filename)
        if(file.exists() && file.isFile()) file.delete()
    }

    static deleteFolder(String folder) {
        emptyFolder(folder)
        def dir = new File(folder)
        dir.deleteDir()
    }

    static emptyFolder(String folder) {
        def dir = new File(folder)
        def files = dir.listFiles()
        if (files != null) {
            files.each { f ->
                if (f.isDirectory()) emptyFolder(f.getAbsolutePath())
                else f.delete()
            }
        }
    }

    static String configureGitRepositoryName(String url) {
        String name = url - ConstantData.GITHUB_URL - ConstantData.GIT_EXTENSION
        return name.replaceAll(RegexUtil.FILE_SEPARATOR_REGEX, "_")
    }

    static createFolder(String folder) {
        File file = new File(folder)
        if (!file.exists()) {
            file.mkdirs()
        }
    }

    static List<String> findFilesFromFolder(String folder) {
        def f = new File(folder)
        def files = []

        if (!f.exists()) return files

        f?.eachDirRecurse { dir ->
            dir.listFiles().each {
                if (it.isFile()) files += it.absolutePath.replaceAll(RegexUtil.FILE_SEPARATOR_REGEX, Matcher.quoteReplacement(File.separator))
            }
        }
        f?.eachFile {
            if (it.isFile()) files += it.absolutePath.replaceAll(RegexUtil.FILE_SEPARATOR_REGEX, Matcher.quoteReplacement(File.separator))
        }
        files.sort()
    }

    private static String configurePath(path){
        def p = path.replaceAll(RegexUtil.FILE_SEPARATOR_REGEX, Matcher.quoteReplacement(File.separator))
        def root = extractRootFolder(path)
        p = p - root
        p
    }

    static boolean isTestFile(path) {
        if(!path || path.empty) return false
        def p = configurePath(path)
        if(DataProperties.UNDEFINED_DIRECTORY_STRUCTURE){
            if(p.startsWith("test${File.separator}") || p.contains("${File.separator}test${File.separator}"))
                true
            else false
        } else {
            if (p.startsWith(DataProperties.GHERKIN_FOLDER) && p.endsWith(ConstantData.VALID_TEST_EXTENSION)) true
            else false
        }
    }

    static boolean isProductionFile(path) {
        if(!path || path.empty) return false
        if (isValidFile(path) && !isTestFile(path) && !isSecondaryTestFile(path)) true
        else false
    }

    static void exportTasksWithConflictInfo(List<MergeTask> tasks, String file) {
        String[] header = ["REPO_URL", "TASK_ID", "#HASHES", "HASHES", "#PROD_FILES", "#TEST_FILES", "LAST", "MERGE", "BASE"]
        List<String[]> content = []
        content += header
        tasks?.each{ task ->
            def hashes = task.commits*.hash
            String[] line = [task.repositoryUrl, task.id, hashes.size(), hashes.toString(), task.productionFiles.size(),
                             task.testFiles.size(), task.newestCommit, task.merge, task.base]
            content += line
        }
        CsvUtil.write(file, content)
    }

    static void exportTasks(List<Task> tasks, String file) {
        String[] header = ["REPO_URL", "TASK_ID", "#HASHES", "HASHES", "#PROD_FILES", "#TEST_FILES", "LAST"]
        List<String[]> content = []
        content += header
        tasks?.each{ task ->
            def hashes = task.commits*.hash
            String[] line = [task.repositoryUrl, task.id, hashes.size(), hashes.toString(),
                             task.productionFiles.size(), task.testFiles.size(), task.newestCommit]
            content += line
        }
        CsvUtil.write(file, content)
    }

    static exportProjectTasks(List<Task> tasks, String tasksCsv, String url){
        def tasksPT = tasks.findAll { !it.productionFiles.empty && !it.testFiles.empty }
        exportTasks(tasksPT, tasksCsv)
        if(!tasksPT || tasksPT.empty) return null

        String[] info = [url, tasks.size(), tasksPT.size()]
        [allTasks:tasksPT, repository:info]
    }

    private static extractRootFolder(path){
        def root = ""
        if(!path || path.empty) return root
        def p = path?.replaceAll(RegexUtil.FILE_SEPARATOR_REGEX, Matcher.quoteReplacement(File.separator))

        if(p?.contains(ConstantData.REPOSITORY_FOLDER)){
            def i1 = p.indexOf(ConstantData.REPOSITORY_FOLDER)
            def begin = p.substring(0, i1)
            def temp = p.substring(i1+ConstantData.REPOSITORY_FOLDER.size())
            def i2 = temp.indexOf(File.separator)
            def projectFolder = temp.substring(0,i2)
            root = begin + ConstantData.REPOSITORY_FOLDER + projectFolder + File.separator
            root = root.replaceAll(RegexUtil.FILE_SEPARATOR_REGEX, Matcher.quoteReplacement(File.separator))
        }
        root
    }

    private static boolean isSecondaryTestFile(path){
        if(!path || path.empty) return false
        def p = configurePath(path)
        if(DataProperties.UNDEFINED_DIRECTORY_STRUCTURE){
            if(p.startsWith("spec${File.separator}") || p.contains("${File.separator}spec${File.separator}"))
                true
            else false
        } else {
            if (p.startsWith(DataProperties.UNIT_FOLDER) || p.startsWith(DataProperties.STEPS_FOLDER)
                    || p.startsWith("test${File.separator}")) true
            else false
        }
    }

    private static boolean isValidFile(path) {
        if(!path || path.empty) return false
        def p = configurePath(path)
        if(DataProperties.UNDEFINED_DIRECTORY_STRUCTURE){
            def validExtension = DataProperties.VALID_PROD_FILES_EXTENSION.any { p.endsWith(it) }
            validExtension
        } else {
            def validFolder = DataProperties.PRODUCTION_FOLDERS.any { p.startsWith(it) }
            def validExtension = DataProperties.VALID_PROD_FILES_EXTENSION.any { p.endsWith(it) }
            if (validFolder && validExtension) true
            else false
        }
    }

}
