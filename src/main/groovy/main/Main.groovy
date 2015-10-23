package main


class Main {

    //arg[0] = project id
    //arg[1] = programming language
    public static void main(String[] args){

        try {
            //Searcher.searchGithubProjects("pivotaltracker-1072", "groovy")
            Searcher.searchProjectsWithLinkAmongTaskAndChangesAndTest(args) //nao esta localizando o arquivo de propriedades
        } catch (IOException e) {
            System.out.println("Problem during projects searching: "+e.getMessage());
        }
    }

    /*  "SELECT repository_url, repository_master_branch, payload_commit_msg, "
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
        */
}
