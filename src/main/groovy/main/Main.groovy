package main

import taskSearch.TaskSearchManager


class Main {

    public static void main(String[] args){
        try {
            TaskSearchManager.searchGithubProjects()
            TaskSearchManager.findProjectsWithLinkAmongTaskAndChangesAndTest(args)
        } catch (IOException e) {
            System.out.println("Problem during projects searching: "+e.getMessage());
        }
    }

}
