package main

import commitSearch.Commit
import commitSearch.CommitSearchManager
import edu.unl.cse.git.App
import groovy.time.TimeCategory
import net.wagstrom.research.github.Github
import repositorySearch.BigQueryServiceManager
import repositorySearch.SearchManager


class Searcher {

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

    public static void searchProjectWithLinkAmongTaskAndChangesAndTest(String[] args, String gitRepository){
        //Download the repository specified at gitminer/configuration.properties
        downloadRepository(args)

        def regex = /([#.*] | [fix.* #.*] | [complet.* #.*] | [finish.* #.*]).*/
        //def regex = /merge pull request #.*/
        CommitSearchManager manager = new CommitSearchManager(gitRepository)
        List<Commit> commits = manager.searchByComment(regex)

        commits.eachWithIndex{ commit, index ->
            println "($index): ${commit.message}"
        }

        //Checar se existem vários commits usando o mesmo ID
        //Para cada ID, checar tipo dos arquivos alterados. queremos arquivos de teste e arquivo de produção
        //distinguir tipo de arquivo por caminho de diretório
    }

    public static void searchProjectsWithLinkAmongTaskAndChangesAndTest(String[] args){

        //para cada projeto pre-selecionado, identificado no arquivo csv de saída (output/selected-projects.csv)
        String gitRepository

        //configurar net.wagstrom.research.github.projects em configuration.properties
        //configurar edu.unl.cse.git.repositories em configuration.properties
        searchProjectWithLinkAmongTaskAndChangesAndTest(args, "LicenseFinder")
    }


}
