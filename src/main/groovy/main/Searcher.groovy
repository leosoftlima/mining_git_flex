package main

import au.com.bytecode.opencsv.CSVReader
import au.com.bytecode.opencsv.CSVWriter
import commitSearch.Commit
import commitSearch.CommitSearchManager
import commitSearch.Task
import edu.unl.cse.git.App
import groovy.time.TimeCategory
import net.wagstrom.research.github.Github
import net.wagstrom.research.github.GithubProperties
import net.wagstrom.research.github.PropNames
import repositorySearch.BigQueryServiceManager
import repositorySearch.RepositorySearchManager
import util.SearchProperties


class Searcher {

    static final String PIVOTAL_TRACKER_ID_REGEX = /.*\[(#|fix.* #|complet.* #|finish.* #)\d+\].*/
    static final String GENERAL_ID_REGEX = /.*#\d+.*/

    private static void downloadRepository(String[] args){
        Github g = new Github()
        g.run(args)
        App a = new App()
        a.run(args)
        println "The repository was downloaded successfully!"
    }

    /**
     * Searches for GitHub projects from the last 5 years.
     *
     * @param language repository's programming language
     * */
    private static String configureQuery(String language){
        language = language.charAt(0).toString().toUpperCase()+language.substring(1) //capitalizes the first letter
        def endDate = new Date()
        def initialDate
        use(TimeCategory) {
            initialDate = endDate - 5.years
        }

        String query = """SELECT repository_url, repository_master_branch, payload_commit_msg,
        (repository_url + '/commit/' + payload_commit_id) AS commit_link, payload_commit_id, created_at, repository_watchers
        FROM [githubarchive:github.timeline]
        WHERE PARSE_UTC_USEC(repository_created_at) <= PARSE_UTC_USEC ('"""+new java.sql.Date(endDate.getTime())+"""')
        AND PARSE_UTC_USEC(repository_created_at) >= PARSE_UTC_USEC ('"""+new java.sql.Date(initialDate.getTime())+"""')
        AND type = 'PushEvent'
        AND repository_language = '""" + language + """'
        AND ( (LOWER(payload_commit_msg) LIKE '[#%]%')
        OR (LOWER(payload_commit_msg) LIKE '[fix% #%]%')
        OR (LOWER(payload_commit_msg) LIKE '[complet% #%]%')
        OR (LOWER(payload_commit_msg) LIKE '[finish% #%]%') )
        GROUP BY repository_url, repository_master_branch, payload_commit_msg, commit_link, payload_commit_id, created_at, repository_watchers
        ORDER BY repository_url"""
        return query
    }

    /***
     * Updates the configuration properties file required by GitMiner to download a GitHub repository.
     *
     * @param repository short path of the repository to download. For example, if the repository URL is
     * https://github.com/spgroup/rgms, the value of this parameter should be spgroup/rgms.
     */
    private static void updatePropertiesFile(String repository){
        def bdurlPath = "tmp-"+repository.replaceAll(CommitSearchManager.FILE_SEPARATOR_REGEX, "_")+"/graph.db"
        Properties props = GithubProperties.props()
        props.setProperty(PropNames.DBURL, bdurlPath)
        props.setProperty("edu.unl.cse.git.dburl", bdurlPath)
        props.setProperty(PropNames.GITHUB_PROJECT_NAMES, repository)
        props.setProperty("edu.unl.cse.git.repositories", repository)
        ClassLoader loader = Thread.currentThread().getContextClassLoader()
        URL url = loader.getResource("configuration.properties");
        FileOutputStream fos = new FileOutputStream(new File(url.toURI()))
        props.store(fos, null)
        fos.close()
    }

    private static List<Task> organizeCommitsByTaskId(List<Commit> commits, String index, String url) {
        List<Task> tasks = []
        def organizedCommits = []

        commits.each{ commit ->
            def idsFromCommit = commit.message.findAll(/#\d+/).unique().collect{it - "#"}
            organizedCommits += [commit:commit, code:idsFromCommit]
        }

        def ids = (organizedCommits*.code)?.unique()?.flatten()
        ids.each{ id ->
            def commitsWithId = organizedCommits.findAll{ id in it.code }
            Task task = new Task(index, url, id, commitsWithId*.commit)
            tasks += task
        }

        println "Total tasks: ${tasks.size()}"

        return tasks
    }

    private static void exportSearchResult(List<Task> tasks){
        CSVWriter writer = new CSVWriter(new FileWriter(SearchProperties.TASKS_FILE))
        String[] text = ["index","repository_url", "task_id", "commits_hash", "changed_production_files", "changed_test_files", "commits_message"]
        writer.writeNext(text)
        for (Task task : tasks) {
            text = [task.repositoryIndex, task.repositoryUrl, task.id, (task.commits*.hash).toString(),
                    task.productionFiles.toString(), task.testFiles.toString(), task.commits*.message?.flatten()]
            writer.writeNext(text)
        }
        writer.close()
    }

    private static def exportSelectedProjects(List<String[]> projects) {
        CSVWriter writer = new CSVWriter(new FileWriter(SearchProperties.SELECTED_REPOSITORIES_FILE))
        String[] text = ["index","repository_url"]
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
    public static void searchGithubProjects() throws IOException {
        Properties props = GithubProperties.props()
        String projectId = props.getProperty("spgroup.bigquery.project.id")
        String language = props.getProperty("spgroup.language")
        String query = configureQuery(language)

        /* Searches GitHub projects and saves the result in a csv file. */
        BigQueryServiceManager.searchProjects(projectId, query);
        System.out.printf("The repositories found by BigQuery service are saved in %s%n", SearchProperties.BIGQUERY_COMMITS_FILE)

        /* Downloading and unzipping projects from csv file. If this is step is not necessary, leave the
        spgroup.search.file.extension at configuration.properties empty.*/
        RepositorySearchManager searcher = new RepositorySearchManager()
        searcher.searchRepositoriesByFileType()

        System.out.printf("The final result of search for GitHub projects is saved in %s%n", SearchProperties.CANDIDATE_REPOSITORIES_FILE)
    }

    /***
     * Checks if a GitHub repository enables to link development tasks, code changes in production files and code
     * changes in test files. Such a link is needed to compute task interfaces.
     *
     * @param args command-line arguments required by GitMiner
     * @param index repository index in candidate-projects.csv
     * @param url repository url
     * @param repository short name of the GitHub repository to analyse
     *
     * @return list of found tasks
     * */
    public static List<Task> findLinkAmongTaskAndChangesAndTest(String[] args, String index, String url, String repository){
        Properties props = GithubProperties.props()
        String pivotalTrackerRegex = props.getProperty("spgroup.task.query.pivotaltracker")
        if(pivotalTrackerRegex==null | pivotalTrackerRegex=="") pivotalTrackerRegex = "false"

        downloadRepository(args) //Download the repository specified in configuration.properties
        CommitSearchManager manager = new CommitSearchManager(repository)

        List<Commit> commits = []
        if(pivotalTrackerRegex.equals("true")) commits = manager.searchByComment(PIVOTAL_TRACKER_ID_REGEX)
        else commits = manager.searchByComment(GENERAL_ID_REGEX)

        return organizeCommitsByTaskId(commits, index, url)
    }

    /***
     * Checks if the previous candidates GitHub projects (content of file "output/candidate-projects.csv") enable to link
     * development tasks, code changes in production files and code changes in test files. Such a link is needed to
     * compute task interfaces. The result is saved in two csv files (output/tasks.csv and output/selected-projects.csv).
     * @param args command-line arguments required by GitMiner
     */
    public static void findProjectsWithLinkAmongTaskAndChangesAndTest(String[] args){
        CSVReader reader = new CSVReader(new FileReader(SearchProperties.CANDIDATE_REPOSITORIES_FILE))
        List<String[]> entries = reader.readAll()
        reader.close()

        if(entries.size()>0) entries.remove(0) //ignore sheet header

        List<String[]> selectedRepositories = []
        List<Task> alltasks = []
        for(String[] entry: entries){
            entry[1] = entry[1].trim()
            String repositoryName = entry[1] - SearchProperties.GITHUB_URL
            String repositoryShortName = repositoryName.substring(repositoryName.lastIndexOf("/")+1)
            updatePropertiesFile(repositoryName)
            List<Task> tasks = findLinkAmongTaskAndChangesAndTest(args, entry[0], entry[1], repositoryShortName)
            if(!tasks.isEmpty()) {
                selectedRepositories += entry
                alltasks += tasks
            }
        }

        exportSearchResult(alltasks)
        System.out.printf("The tasks of GitHub projects are saved in %s%n", SearchProperties.TASKS_FILE)

        exportSelectedProjects(selectedRepositories)
        System.out.printf("The repositories that contains link amog tasks and code changes are saved in %s%n",
                SearchProperties.SELECTED_REPOSITORIES_FILE)
    }

}
