package main


class Main {

    public static void main(String[] args){
        try {
            Searcher.searchGithubProjects("pivotaltracker-1072", "groovy")
            Searcher.findProjectsWithLinkAmongTaskAndChangesAndTest(args)
        } catch (IOException e) {
            System.out.println("Problem during projects searching: "+e.getMessage());
        }
    }

}
