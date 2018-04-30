package br.ufpe.cin.tas.search.task

import br.ufpe.cin.tas.search.repository.BigQuerySearchManager
import br.ufpe.cin.tas.search.repository.GithubApiSearchManager
import br.ufpe.cin.tas.search.repository.RepositorySearchManager
import groovy.util.logging.Slf4j
import br.ufpe.cin.tas.search.task.id.IdTaskExtractor
import br.ufpe.cin.tas.search.task.merge.MergeScenarioExtractor
import br.ufpe.cin.tas.search.task.merge.MergeTaskExtractor
import br.ufpe.cin.tas.util.ConstantData
import br.ufpe.cin.tas.util.CsvUtil
import br.ufpe.cin.tas.util.DataProperties
import br.ufpe.cin.tas.filter.RepositoryFilterManager

@Slf4j
class TaskSearchManager {

    String tasksFile
    String selectedRepositoriesFile
    boolean filterCommitMessage
    RepositorySearchManager repoSearchManager
    RepositoryFilterManager repoFilterManager
    String candidateProjectsFile

    TaskSearchManager(){
        tasksFile = ConstantData.TASKS_FILE
        selectedRepositoriesFile = ConstantData.SELECTED_REPOSITORIES_FILE
        if(!DataProperties.FILTER_BY_DEFAULT_MESSAGE && !DataProperties.FILTER_BY_PIVOTAL_TRACKER){
            repoSearchManager = new GithubApiSearchManager()
            filterCommitMessage = false
            log.info "Searching by GitHub API"
        } else {
            repoSearchManager = new BigQuerySearchManager()
            filterCommitMessage = true
            log.info "Searching by Google Archive"
        }
        repoFilterManager = new RepositoryFilterManager()
        candidateProjectsFile = ConstantData.CANDIDATE_REPOSITORIES_FILE
    }

    private void findTasksById() {
        log.info "Finding tasks based on ID in commit message..."
        List<String[]> selectedRepositories = []
        List<Task> allTasks = []
        List<String[]> entries = CsvUtil.read(candidateProjectsFile)
        if (entries.size() > 1) {
            entries.remove(0) //ignore sheet header
            for (String[] entry : entries) {
                try{
                    IdTaskExtractor taskExtractor = new IdTaskExtractor(entry[0].trim())
                    def r = taskExtractor.extractTasks()
                    if(r){
                        if(!r.allTasks.empty) allTasks += r.allTasks
                        if(r.repository) {
                            String[] info = r.repository
                            selectedRepositories += info
                        }
                    }
                } catch (Exception ex) {
                    log.info "Problem while mining repository: ${ex.message}"
                }
            }
        }
        log.info "The tasks of GitHub projects are saved in '${ConstantData.TASKS_FOLDER}' folder"

        exportSelectedProjects(selectedRepositories)
        log.info "The repositories that contains link amog tasks and code changes are saved in ${ConstantData.SELECTED_REPOSITORIES_FILE}"
    }

    private findTasksByMerge() {
        log.info "Finding tasks based on merge commits..."
        List<String[]> selectedRepositories = []
        List<Task> allTasks = []
        def mergeScenarioExtractor = new MergeScenarioExtractor()
        def mergeFiles = mergeScenarioExtractor.getMergeFiles()
        mergeFiles?.each { mergeFile ->
            try{
                MergeTaskExtractor taskExtractor = new MergeTaskExtractor(mergeFile)
                def r
                if(DataProperties.CONFLICT_ANALYSIS) r = taskExtractor.extractCucumberConflictingTasks()
                else r = taskExtractor.extractTasks()
                if(r){
                    if(!r.allTasks.empty) allTasks += r.allTasks
                    else log.info "No task was found!"
                    if(r.repository) {
                        String[] info = r.repository
                        selectedRepositories += info
                    }
                }
            } catch (Exception ex) {
                log.info "Problem while mining repository: ${ex.message}"
            }
        }
        log.info "The tasks of GitHub projects are saved in '${ConstantData.TASKS_FOLDER}' folder"

        exportSelectedProjects(selectedRepositories)
        log.info "The repositories that contains link amog tasks and code changes are saved in ${ConstantData.SELECTED_REPOSITORIES_FILE}"
    }

    private void exportSelectedProjects(List<String[]> projects) {
        String[] header = ["REPO_URL", "#TASKS", "#P&T_TASKS", "#VALID_TASKS"]
        List<String[]> content = []
        content += header
        content += projects
        CsvUtil.write(selectedRepositoriesFile, content)
    }

    def start(){
        try {
            if(DataProperties.SEARCH_PROJECTS) searchGithubProjects()
            if(DataProperties.FILTER_PROJECTS) filterGithubProjects()
            if(DataProperties.SEARCH_TASKS) searchTasks()
        } catch (Exception ex) {
            log.info "Problem while mining repositories: ${ex.message}"
        }
    }

    def searchGithubProjects() {
        repoSearchManager.search()
        log.info "Found repositories are saved in ${ConstantData.REPOSITORIES_TO_DOWNLOAD_FILE}"
    }

    def filterGithubProjects(){
        repoFilterManager.searchRepositoriesByFileTypeAndGems()
        log.info "Filtered repositories are saved in ${ConstantData.CANDIDATE_REPOSITORIES_FILE}"
    }

    def searchTasks() {
        if(!filterCommitMessage) findTasksByMerge()
        else findTasksById()
    }

}
