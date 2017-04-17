package taskSearch

import groovy.util.logging.Slf4j
import repositorySearch.GithubAPIQueryService
import repositorySearch.GoogleArchiveQueryService
import repositorySearch.QueryService
import taskSearch.mergeScenario.MergeScenario
import taskSearch.mergeScenario.MergeScenarioExtractor
import util.ConstantData
import util.DataProperties
import util.RegexUtil
import repositorySearch.DowloadManager
import util.Util

@Slf4j
class TaskSearchManager {

    static int taskId = 0
    boolean filterCommitMessage
    QueryService queryService
    DowloadManager downloadManager
    MergeScenarioExtractor mergeScenarioExtractor

    TaskSearchManager(){
        if(!DataProperties.FILTER_BY_DEFAULT_MESSAGE && !DataProperties.FILTER_BY_PIVOTAL_TRACKER){
            queryService = new GithubAPIQueryService()
            filterCommitMessage = false
            mergeScenarioExtractor = new MergeScenarioExtractor()
            log.info "Searching by GitHub API"
        } else {
            queryService = new GoogleArchiveQueryService()
            filterCommitMessage = true
            log.info "Searching by Google Archive"
        }
        downloadManager = new DowloadManager()
    }

    private static List<Task> organizeCommitsByTaskId(List<Commit> commits, String index, String url) {
        List<Task> tasks = []
        def organizedCommits = []

        commits.each { commit ->
            def idsFromCommit = commit.message.findAll(/#\d+/).unique().collect { it - "#" }
            organizedCommits += [commit: commit, code: idsFromCommit]
        }

        def ids = (organizedCommits*.code)?.unique()?.flatten()
        ids.each { id ->
            def commitsWithId = organizedCommits.findAll { id in it.code }
            Task task = new Task(index, url, id, commitsWithId*.commit)
            tasks += task
        }

        log.info "Total tasks: ${tasks.size()}"
        tasks
    }

    static void exportTasks(List<Task> tasks, String file) {
        String[] header = ["INDEX", "REPO_URL", "TASK_ID", "HASHES", "#PROD_FILES", "#TEST_FILES"]
        List<String[]> content = []
        tasks?.each{ task ->
            String[] line = [task.repositoryIndex, task.repositoryUrl, task.id, (task.commits*.hash).toString(),
                             task.productionFiles.size(), task.testFiles.size()]
            content += line
        }
        Util.createCsv(file, header, content)
    }

    static void exportTasks(List<Task> tasks) {
        exportTasks(tasks, ConstantData.TASKS_FILE)
    }

    static void exportSelectedProjects(List<String[]> projects) {
        String[] header = ["INDEX", "REPO_URL", "#TASKS", "#P&T_TASKS"]
        Util.createCsv(ConstantData.SELECTED_REPOSITORIES_FILE, header, projects)
    }

    private static findLinkTaskChangeset(String index, String url) {
        GitRepository repository = GitRepository.getRepository(url)
        List<Task> tasks
        List<Commit> commits = []
        if (DataProperties.FILTER_BY_PIVOTAL_TRACKER) commits = repository.searchByComment(RegexUtil.PIVOTAL_TRACKER_ID_REGEX)
        else if (DataProperties.FILTER_BY_DEFAULT_MESSAGE) commits = repository.searchByComment(RegexUtil.GENERAL_ID_REGEX)
        tasks = organizeCommitsByTaskId(commits, index, url)
        [tasks:tasks, repository:repository.name]
    }

    private static List<MergeScenario> extractMerges(String csv){
        def merges = []
        def url = ""
        List<String[]> entries = Util.extractCsvContent(csv)
        if (entries.size() > 2){
            url = entries.first()[0]
            entries.removeAt(0)
            entries.removeAt(0)
            entries?.each{ entry ->
                def v1 = entry[4]?.trim()?.tokenize("@@")?.flatten()
                def v2 = entry[5]?.trim()?.tokenize("@@")?.flatten()
                merges += new MergeScenario(url:url, merge:entry[0], left:entry[1], right:entry[2], base:entry[3],
                        leftCommits: v1 as List<String>, rightCommits: v2 as List<String>)
            }
        }
        merges
    }

    def searchGithubProjects() {
        queryService.searchProjects()
        log.info "Found repositories are saved in ${ConstantData.REPOSITORIES_TO_DOWNLOAD_FILE}"
    }

    def filterGithubProjects(){
        downloadManager.searchRepositoriesByFileTypeAndGems()
        log.info "Filtered repositories are saved in ${ConstantData.CANDIDATE_REPOSITORIES_FILE}"
    }

    private static void findTasksById(){
        List<String[]> entries = Util.extractCsvContent(ConstantData.CANDIDATE_REPOSITORIES_FILE)
        if (entries.size() > 0) entries.remove(0) //ignore sheet header

        List<String[]> selectedRepositories = []
        List<Task> alltasks = []
        for (String[] entry : entries) {
            entry[1] = entry[1].trim()
            def result = findLinkTaskChangeset(entry[0], entry[1])
            List<Task> tasks = result.tasks
            if (!tasks.isEmpty()) {
                def tasksPT = tasks.findAll { !it.productionFiles.empty && !it.testFiles.empty }
                String[] cell = [entry[0], entry[1], tasks.size(), tasksPT.size()]
                selectedRepositories += cell
                exportTasks(tasks, "${ConstantData.TASKS_FOLDER}${result.repository}.csv")
                alltasks += tasks
            }
        }
        exportTasks(alltasks)
        log.info "The tasks of GitHub projects are saved in '${ConstantData.TASKS_FOLDER}' folder"

        exportSelectedProjects(selectedRepositories)
        log.info "The repositories that contains link amog tasks and code changes are saved in ${ConstantData.SELECTED_REPOSITORIES_FILE}"
    }

    static findCandidatesIndex(String url){
        List<String[]> lines = Util.extractCsvContent(ConstantData.CANDIDATE_REPOSITORIES_FILE)
        def repo = url - ConstantData.GITHUB_URL
        def selected = lines.find{ it[1] == repo }
        if(selected) selected[0]
        else null
    }

    def findTasksByMerge(){
        mergeScenarioExtractor?.extract()
        List<String[]> selectedRepositories = []
        List<Task> alltasks = []
        def urls = []
        def indexes = []
        def mergeFiles = Util.findFilesFromFolder(ConstantData.MERGES_FOLDER)?.findAll{
            it.endsWith(ConstantData.MERGE_TASK_SUFIX)
        }

        mergeFiles?.each { mergeFile ->
            def merges = extractMerges(mergeFile)
            def url = merges.first().url
            def index = findCandidatesIndex(url)
            if(index){
                urls += url
                indexes += index
                def tasks = []
                GitRepository repository = GitRepository.getRepository(url)
                merges?.each{ merge ->
                    List<Commit> commits = repository.searchCommits(merge.leftCommits as String[])
                    def leftTask = new MergeTask(index, url, ++taskId as String, commits, merge)
                    tasks += leftTask

                    commits = repository.searchCommits(merge.rightCommits as String[])
                    def rightTask = new MergeTask(index, url, ++taskId as String, commits, merge)
                    tasks += rightTask

                    alltasks += tasks
                }

                def tasksPT = tasks.findAll { !it.productionFiles.empty && !it.testFiles.empty }
                String[] cell = [index, url, tasks.size(), tasksPT.size()]
                selectedRepositories += cell

                exportTasks(tasks, "${ConstantData.TASKS_FOLDER}${repository.name}.csv")
            }
        }
        log.info "The tasks of GitHub projects are saved in '${ConstantData.TASKS_FOLDER}' folder"
        exportTasks(alltasks)

        exportSelectedProjects(selectedRepositories)
        log.info "The repositories that contains link amog tasks and code changes are saved in ${ConstantData.SELECTED_REPOSITORIES_FILE}"
    }

    def findTasks() {
        if(!filterCommitMessage) findTasksByMerge()
        else findTasksById()
    }

}
