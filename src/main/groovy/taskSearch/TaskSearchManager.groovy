package taskSearch

import au.com.bytecode.opencsv.CSVReader
import au.com.bytecode.opencsv.CSVWriter
import groovy.util.logging.Slf4j
import repositorySearch.QueryService
import util.DataProperties
import util.RegexUtil
import repositorySearch.DowloadManager

@Slf4j
class TaskSearchManager {

    QueryService queryService

    TaskSearchManager(){
        queryService = new QueryService()
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

        return tasks
    }

    private static void exportSearchResult(List<Task> tasks) {
        def file = new File(DataProperties.TASKS_FILE)
        CSVWriter writer = new CSVWriter(new FileWriter(file))
        String[] text = ["index", "repository_url", "task_id", "commits_hash", "changed_production_files", "changed_test_files", "commits_message"]
        writer.writeNext(text)
        for (Task task : tasks) {
            String msgs = task.commits*.message?.flatten()?.toString()
            if(msgs.length()>1000) msgs = msgs.substring(0,999)+" [TOO_LONG]"

            text = [task.repositoryIndex, task.repositoryUrl, task.id, (task.commits*.hash).toString(),
                    task.productionFiles.size(), task.testFiles.size(), msgs]
            writer.writeNext(text)
        }
        writer.close()
    }

    private static void exportSelectedProjects(List<String[]> projects) {
        def file = new File(DataProperties.SELECTED_REPOSITORIES_FILE)
        CSVWriter writer = new CSVWriter(new FileWriter(file))
        String[] text = ["index", "repository_url", "tasks", "tasks_production_test"]
        writer.writeNext(text)
        writer.writeAll(projects)
        writer.close()
    }

    /**
     * Searches for GitHub projects from the last 5 years that contains files of a specific type.
     * The searching uses Google BigQuery service that requires a BigQuery repository id (spgroup.bigquery.project.id at
     * configuration.properties). It is also necessary to identify the repository's programming language
     * (spgroup.language at configuration.properties). If no file type is specified (spgroup.search.file.extension at
     * configuration.properties), such a criteria is not used.
     *
     * @throws IOException if there's an error during the remote repositorySearch.
     */
    def searchGithubProjects() {
        /* Searches GitHub projects and saves the result in a csv file. */
        queryService.searchProjects()
        log.info "The repositories found by BigQuery service are saved in ${DataProperties.BIGQUERY_COMMITS_FILE}"

        /* Downloading and unzipping projects from csv file. If this is step is not necessary, leave the
        spgroup.search.file.extension at configuration.properties empty.*/
        DowloadManager searcher = new DowloadManager()
        searcher.searchRepositoriesByFileType()

        log.info "The final result of search for GitHub projects is saved in ${DataProperties.CANDIDATE_REPOSITORIES_FILE}"
    }

    static List<Task> findLinkTaskChangeset(String index, String url) {
        GitRepository repository = GitRepository.getRepository(url)

        List<Commit> commits
        if (DataProperties.FILTER_BY_PIVOTAL_TRACKER) commits = repository.searchByComment(RegexUtil.PIVOTAL_TRACKER_ID_REGEX)
        else commits = repository.searchByComment(RegexUtil.GENERAL_ID_REGEX)

        organizeCommitsByTaskId(commits, index, url)
    }

    static findTasks() {
        def file = new File(DataProperties.CANDIDATE_REPOSITORIES_FILE)
        CSVReader reader = new CSVReader(new FileReader(file))
        List<String[]> entries = reader.readAll()
        reader.close()

        if (entries.size() > 0) entries.remove(0) //ignore sheet header

        List<String[]> selectedRepositories = []
        List<Task> alltasks = []
        for (String[] entry : entries) {
            entry[1] = entry[1].trim()
            List<Task> tasks = findLinkTaskChangeset(entry[0], entry[1])
            if (!tasks.isEmpty()) {
                def tasksPT = tasks.findAll { !it.productionFiles.isEmpty() && !it.testFiles.isEmpty() }
                String[] cell = [entry[0], entry[1], tasks.size(), tasksPT.size()]
                selectedRepositories += cell
                alltasks += tasks
            }
        }

        exportSearchResult(alltasks)
        log.info "The tasks of GitHub projects are saved in ${DataProperties.TASKS_FILE}"

        exportSelectedProjects(selectedRepositories)
        log.info "The repositories that contains link amog tasks and code changes are saved in ${DataProperties.SELECTED_REPOSITORIES_FILE}"
    }

}
