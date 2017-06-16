package br.ufpe.cin.tas.util

import br.ufpe.cin.tas.search.task.Task

import java.util.regex.Matcher

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
        if (p.startsWith(ConstantData.GHERKIN_FILES_RELATIVE_PATH) && p.endsWith(ConstantData.VALID_TEST_EXTENSION)) true
        else false
    }

    static boolean isProductionFile(path) {
        if(!path || path.empty) return false
        if (isValidFile(path) && !isTestFile(path) && !isSecondaryTestFile(path)) true
        else false
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
        if (p.startsWith(ConstantData.UNIT_TEST_FILES_RELATIVE_PATH) || p.startsWith(ConstantData.STEPS_FILES_RELATIVE_PATH)
                || p.startsWith("test${File.separator}")) true
        else false
    }

    private static boolean isValidFile(path) {
        if(!path || path.empty) return false
        def p = configurePath(path)
        def validFolder = ConstantData.VALID_PROD_FOLDERS.any { p.startsWith(it) }
        def validExtension = ConstantData.VALID_PROD_EXTENSIONS.any { p.endsWith(it) }
        def validViewExtension = (p?.endsWith(".erb") || p?.endsWith(".haml") || p?.endsWith(".slim"))
        if (validFolder && validExtension) true
        else if(validFolder && p?.count(".")==1 && validViewExtension) true
        else false
    }

}
