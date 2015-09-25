package search;


public class Main {

    public static void main(String[] args){
        SearchManager searcher = new SearchManager();

        //Downloading and unzipping a project (example)
        searcher.checkProject("https://github.com/spgroup/rgms/archive/master.zip");

        // Downloading and unzipping projects from csv file
        searcher.searchCucumberProjects();

        System.out.printf("Number of analyzed projects: %d%n", searcher.getCounter());
        System.out.printf("Number of selected projects: %d (%.2f%%)%n", searcher.getSelectedCounter(),
                ((double)searcher.getSelectedCounter()/searcher.getCounter())*100);

        searcher.listSelectedRepositories();
    }

}
