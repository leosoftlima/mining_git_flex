package demo;


import repositorySearch.BigQueryServiceManager;
import repositorySearch.SearchManager;

import java.io.IOException;

public class Demo {

    /***
     * Searchs groovy projects from the last 5 years that contain Gherkin files and commit message with ID information.
     */
    public static void searchGherkinProjects (){
        String query = "SELECT repository_url, repository_master_branch, payload_commit_msg, "
                + "(repository_url + '/commit/' + payload_commit_id) AS commit_link, payload_commit_id, created_at, repository_watchers "
                + "FROM [githubarchive:github.timeline] "
                + "WHERE PARSE_UTC_USEC(repository_created_at) <= PARSE_UTC_USEC ('2015-08-31 23:59:59') "
                + "AND PARSE_UTC_USEC(repository_created_at) >= PARSE_UTC_USEC ('2010-08-31 23:59:59') "
                + "AND type = 'PushEvent' "
                + "AND repository_language = 'Groovy' "
                + "AND ( (LOWER(payload_commit_msg) LIKE '[#%]%') "
                + "OR (LOWER(payload_commit_msg) LIKE '[fix% #%]%') "
                + "OR (LOWER(payload_commit_msg) LIKE '[complet% #%]%') "
                + "OR (LOWER(payload_commit_msg) LIKE '[finish% #%]%') ) "
                + "GROUP BY repository_url, repository_master_branch, payload_commit_msg, commit_link, payload_commit_id, created_at, repository_watchers "
                + "ORDER BY repository_url";

        try {
            /* Searches GitHub projects and saves the result in a csv file. If the csv file already exists, this step is
            not necessary */
            BigQueryServiceManager.searchProjects("pivotaltracker-1072", query); //projectd id, query

            // Downloading and unzipping projects from csv file
            SearchManager searcher = new SearchManager();
            searcher.searchGherkinProjects();
        } catch (IOException e) {
            System.out.println("Problem during projects searching: "+e.getMessage());
        }
    }

    /***
     * Downloads and unzips https://github.com/spgroup/rgms/archive/master.zip
     */
    public static void downloadRepository(){
        SearchManager searcher = new SearchManager();
        searcher.analyseRepository("https://github.com/spgroup/rgms/archive/master.zip");
    }

}
