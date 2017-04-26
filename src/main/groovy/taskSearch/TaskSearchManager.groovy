package taskSearch

import groovy.util.logging.Slf4j
import repositorySearch.GithubAPIQueryService
import repositorySearch.GoogleArchiveQueryService
import repositorySearch.QueryService
import taskSearch.id.IdTaskExtractor
import taskSearch.merge.MergeScenarioExtractor
import taskSearch.merge.MergeTaskExtractor
import util.ConstantData
import util.CsvUtil
import util.DataProperties
import filter.RepositoryFilterManager
import util.Util

@Slf4j
class TaskSearchManager {

    boolean filterCommitMessage
    QueryService queryService
    RepositoryFilterManager filterManager
    String candidateProjectsFile
    MergeScenarioExtractor mergeScenarioExtractor

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
        candidateProjectsFile = ConstantData.CANDIDATE_REPOSITORIES_FILE
        mergeScenarioExtractor = new MergeScenarioExtractor()
    }

    def start(){
        try {
            if(DataProperties.SEARCH_PROJECTS) searchGithubProjects()
            if(DataProperties.FILTER_PROJECTS) filterGithubProjects()
            if(DataProperties.SEARCH_TASKS) searchTasks()
        } catch (Exception ex) {
            log.info "Problem during projects searching."
            log.info ex.message
            ex.stackTrace.each{ log.error it.toString() }
        }
    }

    private void findTasksById(){
        log.info "Finding tasks based on ID in commit message..."
        List<String[]> selectedRepositories = []
        List<Task> allTasks = []
        List<String[]> entries = CsvUtil.read(candidateProjectsFile)
        if (entries.size() > 1) {
            entries.remove(0) //ignore sheet header
            for (String[] entry : entries) {
                entry[1] = entry[1].trim()
                def taskExtractor = new IdTaskExtractor(entry[0], entry[1])
                def r = taskExtractor.extractTasks()
                if(r){
                    if(!r.allTasks.empty) allTasks += r.allTasks
                    if(r.repository) {
                        String[] info = r.repository
                        selectedRepositories += info
                    }
                }
            }
        }
        exportTasks(allTasks)
        log.info "The tasks of GitHub projects are saved in '${ConstantData.TASKS_FOLDER}' folder"

        exportSelectedProjects(selectedRepositories)
        log.info "The repositories that contains link amog tasks and code changes are saved in ${ConstantData.SELECTED_REPOSITORIES_FILE}"
    }

    private findTasksByMerge(){
        log.info "Finding tasks based on merge commits..."
        List<String[]> selectedRepositories = []
        List<Task> allTasks = []
        def mergeFiles = mergeScenarioExtractor.getMergeFiles()
        mergeFiles?.each { mergeFile ->
            MergeTaskExtractor taskExtractor = new MergeTaskExtractor(mergeFile)
            def r = taskExtractor.extractTasks()
            if(r){
                if(!r.allTasks.empty) allTasks += r.allTasks
                else log.info "No task was found!"
                if(r.repository) {
                    String[] info = r.repository
                    selectedRepositories += info
                }
            } else {
                log.error "Error while searching merge tasks. Please, verify project's index."
            }
        }
        log.info "The tasks of GitHub projects are saved in '${ConstantData.TASKS_FOLDER}' folder"
        exportTasks(allTasks)

        exportSelectedProjects(selectedRepositories)
        log.info "The repositories that contains link amog tasks and code changes are saved in ${ConstantData.SELECTED_REPOSITORIES_FILE}"
    }

    private static void exportTasks(List<Task> tasks) {
        Util.exportTasks(tasks, ConstantData.TASKS_FILE)
    }

    private static void exportSelectedProjects(List<String[]> projects) {
        String[] header = ["INDEX", "REPO_URL", "#TASKS", "#P&T_TASKS"]
        List<String[]> content = []
        content += header
        content += projects
        CsvUtil.write(ConstantData.SELECTED_REPOSITORIES_FILE, content)
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
