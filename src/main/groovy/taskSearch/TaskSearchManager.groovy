package taskSearch

import groovy.util.logging.Slf4j
import repositorySearch.GithubAPIQueryService
import repositorySearch.GoogleArchiveQueryService
import repositorySearch.QueryService
import taskSearch.id.IdTaskExtractor
import taskSearch.merge.MergeTaskExtractor
import util.ConstantData
import util.DataProperties
import filter.RepositoryFilterManager
import util.Util

@Slf4j
class TaskSearchManager {

    boolean filterCommitMessage
    QueryService queryService
    RepositoryFilterManager filterManager

    TaskSearchManager(){
        if(!DataProperties.FILTER_BY_DEFAULT_MESSAGE && !DataProperties.FILTER_BY_PIVOTAL_TRACKER){
            queryService = new GithubAPIQueryService()
            filterCommitMessage = false
            log.info "Searching by GitHub API"
        } else {
            queryService = new GoogleArchiveQueryService()
            filterCommitMessage = true
            log.info "Searching by Google Archive"
        }
        filterManager = new RepositoryFilterManager()
    }

    private static void findTasksById(){
        log.info "Finding tasks based on ID in commit message..."
        List<String[]> entries = Util.extractCsvContent(ConstantData.CANDIDATE_REPOSITORIES_FILE)
        if (entries.size() > 0) entries.remove(0) //ignore sheet header

        List<String[]> selectedRepositories = []
        List<Task> alltasks = []
        for (String[] entry : entries) {
            entry[1] = entry[1].trim()
            def taskExtractor = new IdTaskExtractor(entry[0], entry[1])
            def result = taskExtractor.findLinkTaskChangeset()
            List<Task> tasks = result.tasks
            if (!tasks.empty) {
                def tasksPT = tasks.findAll { !it.productionFiles.empty && !it.testFiles.empty }
                String[] cell = [entry[0], entry[1], tasks.size(), tasksPT.size()]
                selectedRepositories += cell
                exportTasks(tasksPT, "${ConstantData.TASKS_FOLDER}${result.repository}.csv")
                alltasks += tasksPT
            }
        }
        exportTasks(alltasks)
        log.info "The tasks of GitHub projects are saved in '${ConstantData.TASKS_FOLDER}' folder"

        exportSelectedProjects(selectedRepositories)
        log.info "The repositories that contains link amog tasks and code changes are saved in ${ConstantData.SELECTED_REPOSITORIES_FILE}"
    }

    private static findTasksByMerge(){
        log.info "Finding tasks based on merge commits..."
        List<String[]> selectedRepositories = []
        List<Task> allTasks = []
        def mergeFiles =  MergeTaskExtractor.retrieveMergeFiles()
        mergeFiles?.each { mergeFile ->
            println "mergeFile: ${mergeFile}"
            MergeTaskExtractor taskExtractor = new MergeTaskExtractor(mergeFile)
            def r = taskExtractor.extractTasksFromMergeFile()
            if(r){
                if(!r.allTasks.empty) allTasks += r.allTasks
                if(r.repository) {
                    String[] info = r.repository
                    selectedRepositories += info
                }
            }
        }
        log.info "The tasks of GitHub projects are saved in '${ConstantData.TASKS_FOLDER}' folder"
        exportTasks(allTasks)

        exportSelectedProjects(selectedRepositories)
        log.info "The repositories that contains link amog tasks and code changes are saved in ${ConstantData.SELECTED_REPOSITORIES_FILE}"
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

    private static void exportTasks(List<Task> tasks) {
        exportTasks(tasks, ConstantData.TASKS_FILE)
    }

    private static void exportSelectedProjects(List<String[]> projects) {
        String[] header = ["INDEX", "REPO_URL", "#TASKS", "#P&T_TASKS"]
        Util.createCsv(ConstantData.SELECTED_REPOSITORIES_FILE, header, projects)
    }

    def searchGithubProjects() {
        queryService.searchProjects()
        log.info "Found repositories are saved in ${ConstantData.REPOSITORIES_TO_DOWNLOAD_FILE}"
    }

    def filterGithubProjects(){
        filterManager.searchRepositoriesByFileTypeAndGems()
        log.info "Filtered repositories are saved in ${ConstantData.CANDIDATE_REPOSITORIES_FILE}"
    }

    def searchTasks() {
        if(!filterCommitMessage) findTasksByMerge()
        else findTasksById()
    }

}
