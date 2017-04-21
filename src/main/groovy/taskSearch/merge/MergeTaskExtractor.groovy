package taskSearch.merge

import groovy.util.logging.Slf4j
import taskSearch.GitRepository
import taskSearch.Task
import util.ConstantData
import util.DataProperties
import util.Util

@Slf4j
class MergeTaskExtractor {

    String mergesCsv
    GitRepository repository
    String index
    List<MergeScenario> mergeScenarios
    String tasksCsv
    private static int taskId = 0

    MergeTaskExtractor(String mergeFile){
        mergesCsv = mergeFile
        mergeScenarios = extractMergeScenarios()
        def url = mergeScenarios.first().url
        index = findCandidatesIndex(url)
        repository = GitRepository.getRepository(url)
        tasksCsv = "${ConstantData.TASKS_FOLDER}${repository.name}.csv"
    }

    private static findCandidatesIndex(String url){
        List<String[]> lines = Util.extractCsvContent(ConstantData.CANDIDATE_REPOSITORIES_FILE)
        def repo = url - ConstantData.GIT_EXTENSION
        def selected = lines.find{ it[1] == repo }
        if(selected) selected[0]
        else null
    }

    private static void exportTasks(List<Task> tasks, String file) {
        String[] header = ["INDEX", "REPO_URL", "TASK_ID", "HASHES", "#PROD_FILES", "#TEST_FILES"]
        List<String[]> content = []
        tasks?.each{ task ->
            String[] line = [task.repositoryIndex, task.repositoryUrl, task.id, (task.commits*.hash).toString(),
                             task.productionFiles.size(), task.testFiles.size()]
            content += line
        }
        Util.createCsv(file, header, content)
    }

    private configureMergeTask(MergeScenario merge){
        def commits1 = repository?.searchCommits(merge.leftCommits)
        def leftTask = new MergeTask(index, repository.url, ++taskId as String, commits1, merge)
        def commits2 = repository?.searchCommits(merge.rightCommits)
        def rightTask = new MergeTask(index, repository.url, ++taskId as String, commits2, merge)
        [leftTask, rightTask]
    }

    private List<MergeScenario> extractMergeScenarios(){
        def merges = []
        def url = ""
        List<String[]> entries = Util.extractCsvContent(mergesCsv)
        if (entries.size() > 2){
            url = entries.first()[0]
            entries.removeAt(0)
            entries.removeAt(0)
            entries?.each{ entry ->
                def v1, v2
                if(entry[4].size()>2) v1 = entry[4].substring(1, entry[4].size()-1).tokenize(', ')
                else v1 = []
                if(entry[5].size()>2) v2 = entry[5].substring(1, entry[5].size()-1).tokenize(', ')
                else v2 = []
                merges += new MergeScenario(url:url, merge:entry[0], left:entry[1], right:entry[2], base:entry[3],
                        leftCommits: v1 as List<String>, rightCommits: v2 as List<String>)
            }
        }
        merges
    }

    private exportResult(List<MergeTask> tasks, List<MergeTask> tasksPT){
        String[] info = null
        def allTasks = []
        if(tasksPT.size() > DataProperties.TASK_LIMIT) {
            def sublist = tasksPT.subList(0,DataProperties.TASK_LIMIT)
            allTasks = sublist
            exportTasks(sublist, tasksCsv)
        }
        else{
            allTasks = tasksPT
            exportTasks(tasksPT, tasksCsv)
        }

        if(tasksPT.size() > 0) {
            info = [index, repository.url, tasks.size(), tasksPT.size()]
        }

        [allTasks:allTasks, repository:info]
    }

    static retrieveMergeFiles(){
        MergeScenarioExtractor mergeScenarioExtractor = new MergeScenarioExtractor()
        mergeScenarioExtractor?.extract()
        def mergeFiles = Util.findFilesFromFolder(ConstantData.MERGES_FOLDER)?.findAll{
            it.endsWith(ConstantData.MERGE_TASK_SUFIX)
        }
        mergeFiles
    }

    def extractTasksFromMergeFile(){
        def result = null
        if(index){
            def tasks = []
            mergeScenarios?.each{ tasks += configureMergeTask(it) }
            def tasksPT = tasks.findAll { !it.productionFiles.empty && !it.testFiles.empty }
            result = exportResult(tasks, tasksPT)
        }
        result
    }

}
