package util

import taskSearch.Task

import java.util.regex.Matcher

class Util {

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
        if (p?.contains(ConstantData.GHERKIN_FILES_RELATIVE_PATH)
                || p?.contains(ConstantData.STEPS_FILES_RELATIVE_PATH)) {
            true
        } else false
    }

    static void exportTasks(List<Task> tasks, String file) {
        String[] header = ["INDEX", "REPO_URL", "TASK_ID", "HASHES", "#PROD_FILES", "#TEST_FILES"]
        List<String[]> content = []
        content += header
        tasks?.each{ task ->
            String[] line = [task.repositoryIndex, task.repositoryUrl, task.id, (task.commits*.hash).toString(),
                             task.productionFiles.size(), task.testFiles.size()]
            content += line
        }
        CsvUtil.write(file, content)
    }

    static exportProjectTasks(List<Task> tasks, List<Task> tasksPT, String tasksCsv, String index, String url){
        String[] info = null
        exportTasks(tasksPT, tasksCsv)
        if(tasksPT.size() > 0) {
            info = [index, url, tasks.size(), tasksPT.size()]
        }
        [allTasks:tasksPT, repository:info]
    }

}
