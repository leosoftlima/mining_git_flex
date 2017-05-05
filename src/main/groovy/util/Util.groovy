package util

import taskSearch.Task

import java.util.regex.Matcher

class Util {

    private static boolean isSecundaryTestFile(path){
        def p = path?.replaceAll(RegexUtil.FILE_SEPARATOR_REGEX, Matcher.quoteReplacement(File.separator))
        if (p?.contains(ConstantData.UNIT_TEST_FILES_RELATIVE_PATH) || p?.contains(ConstantData.STEPS_FILES_RELATIVE_PATH)
                || p?.contains("test${File.separator}")) true
        else false
    }

    private static boolean isValidFile(path) {
        def p = path?.replaceAll(RegexUtil.FILE_SEPARATOR_REGEX, Matcher.quoteReplacement(File.separator))
        def validFolder = ConstantData.VALID_FOLDERS.any { p?.contains(it) }
        def validExtension = ConstantData.VALID_EXTENSIONS.any { p?.endsWith(it) }
        def validViewExtension = (p?.endsWith(".erb") || p?.endsWith(".haml") || p?.endsWith(".slim"))
        if (validFolder && validExtension) true
        else if(validFolder && p?.count(".")==1 && validViewExtension) true
        else false
    }

    static deleteFile(String filename){
        def file = new File(filename)
        if(file.exists() && file.isFile()) file.delete()
    }

    static deleteFolder(String folder) {
        def dir = new File(folder)
        def files = dir.listFiles()
        if (files != null) {
            files.each { f ->
                if (f.isDirectory()) emptyFolder(f.getAbsolutePath())
                else f.delete()
            }
        }
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

    static boolean isTestFile(path) {
        def p = path?.replaceAll(RegexUtil.FILE_SEPARATOR_REGEX, Matcher.quoteReplacement(File.separator))
        if (p?.contains(ConstantData.GHERKIN_FILES_RELATIVE_PATH) && p?.endsWith(".feature")) true
        else false
    }

    static boolean isProductionFile(path) {
        println "$path is valid? ${isValidFile(path)}"
        if (isValidFile(path) && !isTestFile(path) && !isSecundaryTestFile(path)) true
        else false
    }

    static void exportTasks(List<Task> tasks, String file) {
        String[] header = ["REPO_URL", "TASK_ID", "#HASHES", "HASHES", "#PROD_FILES", "#TEST_FILES"]
        List<String[]> content = []
        content += header
        tasks?.each{ task ->
            def hashes = task.commits*.hash
            String[] line = [task.repositoryUrl, task.id, hashes.size(), hashes.toString(),
                             task.productionFiles.size(), task.testFiles.size()]
            content += line
        }
        CsvUtil.write(file, content)
    }

    static exportProjectTasks(List<Task> tasks, List<Task> tasksPT, String tasksCsv, String url){
        exportTasks(tasksPT, tasksCsv)
        if(!tasksPT || tasksPT.empty) return null
        String[] info = [url, tasks.size(), tasksPT.size()]
        [allTasks:tasksPT, repository:info]
    }

}
