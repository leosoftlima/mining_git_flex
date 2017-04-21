package util

import au.com.bytecode.opencsv.CSVReader
import au.com.bytecode.opencsv.CSVWriter
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
        File zipFolder = new File(folder)
        if (!zipFolder.exists()) {
            zipFolder.mkdirs()
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

    static extractCsvContent(String csv){
        def file = new File(csv)
        if(!file.exists()) return []
        CSVReader reader = new CSVReader(new FileReader(file))
        List<String[]> entries = reader.readAll()
        reader.close()
        entries
    }

    static appendCsv(String filename, List<String[]> content){
        def file = new File(filename)
        CSVWriter writer = new CSVWriter(new FileWriter(file, true))
        writer.writeAll(content)
        writer.close()
    }

    static createCsv(String filename, String[] header, List content){
        def file = new File(filename)
        CSVWriter writer = new CSVWriter(new FileWriter(file))
        writer.writeNext(header)
        writer.writeAll(content)
        writer.close()
    }

    static boolean isTestCode(def path) {
        if (path ==~ /$DataProperties.TEST_CODE_REGEX/) true
        else false
    }

    static void exportTasks(List<Task> tasks, String file) {
        String[] header = ["INDEX", "REPO_URL", "TASK_ID", "HASHES", "#PROD_FILES", "#TEST_FILES"]
        List<String[]> content = []
        tasks?.each{ task ->
            String[] line = [task.repositoryIndex, task.repositoryUrl, task.id, (task.commits*.hash).toString(),
                             task.productionFiles.size(), task.testFiles.size()]
            content += line
        }
        createCsv(file, header, content)
    }

    static exportProjectTasks(List<Task> tasks, List<Task> tasksPT, String tasksCsv, String index, String url){
        String[] info = null
        def allTasks = []
        if(tasksPT.size() > DataProperties.TASK_LIMIT*2) {
            def sublist = tasksPT.subList(0,DataProperties.TASK_LIMIT*2)
            allTasks = sublist
            exportTasks(sublist, tasksCsv)
        }
        else{
            allTasks = tasksPT
            exportTasks(tasksPT, tasksCsv)
        }

        if(tasksPT.size() > 0) {
            info = [index, url, tasks.size(), tasksPT.size()]
        }

        [allTasks:allTasks, repository:info]
    }

}
