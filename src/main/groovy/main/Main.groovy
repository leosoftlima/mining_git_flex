package main

import static taskSearch.TaskSearchManager.searchGithubProjects
import static taskSearch.TaskSearchManager.findProjectsWithLinkAmongTaskAndChangesAndTest

class Main {

    public static void main(String[] args){
        try {
            searchGithubProjects()
            findProjectsWithLinkAmongTaskAndChangesAndTest(args)
        } catch (IOException e) {
            println "Problem during projects searching: "+e.getMessage()
        }
    }

}
