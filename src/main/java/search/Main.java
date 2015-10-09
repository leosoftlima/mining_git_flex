package search;


public class Main {

    public static void main(String[] args){
        SearchManager searcher = new SearchManager();

        //Downloading and unzipping a project (example)
        //searcher.analyseRepository("https://github.com/spgroup/rgms/archive/master.zip");

        // Downloading and unzipping projects from csv file
        searcher.searchGherkinProjects();
    }

}
