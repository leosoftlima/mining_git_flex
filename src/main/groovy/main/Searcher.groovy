package main

import au.com.bytecode.opencsv.CSVReader
import commitSearch.Commit
import commitSearch.CommitSearchManager
import edu.unl.cse.git.App
import groovy.time.TimeCategory
import net.wagstrom.research.github.Github
import net.wagstrom.research.github.GithubProperties
import net.wagstrom.research.github.PropNames
import repositorySearch.BigQueryServiceManager
import repositorySearch.SearchManager
import util.Util


class Searcher {

    private static final String TASK_ID_REGEX = /([#.*] | [fix.* #.*] | [complet.* #.*] | [finish.* #.*]).*/

    private static void downloadRepository(String[] args){
        Github g = new Github()
        g.run(args)
        App a = new App()
        a.run(args)
        println "The repository was downloaded successfully!"
    }

    /**
     * Searches for GitHub projects from the last 5 years.
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
     *
     * @param repositoryName
     */
    private static void updatePropertiesFile(String repositoryName){
        Properties props = GithubProperties.props()
        props.setProperty(PropNames.GITHUB_PROJECT_NAMES, repositoryName)
        props.setProperty("edu.unl.cse.git.repositories", repositoryName)
        ClassLoader loader = Thread.currentThread().getContextClassLoader()
        URL url = loader.getResource("configuration.properties");
        FileOutputStream fos = new FileOutputStream(new File(url.toURI()))
        props.store(fos, null)
        fos.close()
    }

    /**
     * Searches for GitHub projects from the last 5 years that contains Gherkin files.
     * Gherkin is the language used by some BDD (Behavior Driven Development) tools as Cucumber.
     * The searching uses Google BigQuery service.
     * @param projectId BigQuery repository id
     * @param language repository's programming language
     * @throws IOException if there's an error during the remote repositorySearch.
     */
    public static void searchGithubProjects(String projectId, String language) throws IOException {
        String query = configureQuery(language)

        /* Searches GitHub projects and saves the result in a csv file. If the file already exists, this step is not required. */
        BigQueryServiceManager.searchProjects(projectId, query); //projectd id, query

        /* Downloading and unzipping projects from csv file*/
        SearchManager searcher = new SearchManager();
        searcher.searchGherkinProjects();
    }

    /***
     * Checks if a GitHub repository enables to link development tasks, code changes in production files and code
     * changes in test files. Such a link is needed to compute task interfaces.
     * @param args command-line arguments required by GitMiner
     * @param repository short name of the GitHub repository to analyse
     * */
    public static void findLinkAmongTaskAndChangesAndTest(String[] args, String repository){
        downloadRepository(args) //Download the repository specified in configuration.properties
        CommitSearchManager manager = new CommitSearchManager(repository)
        List<Commit> commits = manager.searchByComment(TASK_ID_REGEX)
        commits.eachWithIndex{ commit, index ->
            println "($index): ${commit.message}"
        }
        //nesse ponto, o arraylist commits contém a lista de commits cuja mensagem possui ID
        //Ainda falta:
        //1 - Checar se existem vários commits usando o mesmo ID
        //2 - Para cada ID, checar tipo dos arquivos alterados. queremos arquivos de teste e arquivo de produção. Lembrar
        // de distinguir tipo de arquivo por caminho de diretório
    }

    /***
     * Checks if the previous selected GitHub projects (content of file "output/selected-projects.csv") enable to link
     * development tasks, code changes in production files and code changes in test files. Such a link is needed to
     * compute task interfaces.
     * @param args command-line arguments required by GitMiner
     */
    public static void findProjectsWithLinkAmongTaskAndChangesAndTest(String[] args){
        CSVReader reader = new CSVReader(new FileReader(Util.SELECTED_PROJECTS_FILE))
        List<String[]> entries = reader.readAll()
        reader.close()

        if(entries.size()>0) entries.remove(0) //ignore sheet header

        for(String[] entry: entries){
            String repositoryName = entry[1] - Util.GITHUB_URL
            updatePropertiesFile(repositoryName)
            String repositoryShortName = repositoryName.substring(repositoryName.lastIndexOf("/")+1)
            findLinkAmongTaskAndChangesAndTest(args, repositoryShortName)
        }
    }

}
