package search;

import java.io.IOException;
import java.util.ArrayList;


public class Main {

    private static void check(Repository repository) {
        try {
            repository.downloadCommits();
            repository.unzipCommits();
            if (!repository.usesCucumber()) {
                System.out.println("The project does not use Cucumber!");
                repository.delete();
            } else System.out.println("The project does use Cucumber!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void checkProject(String url, String branch){
        Repository repo = new Repository(url, branch);
        check(repo);
    }

    public static void searchCucumberProjects(){
        ArrayList<Repository> repositories = new ArrayList<>();
        try {
            repositories = FileHandler.extractProjects();
        } catch (IOException e) {
            e.printStackTrace();
        }
        for(Repository repository: repositories){
            check(repository);
        }
    }

    public static void main(String[] args){
        //Downloading and unzipping a project
        //checkProject("https://github.com/spgroup/rgms", "master");

        // Downloading and unzipping projects from csv file
        searchCucumberProjects();
    }

}
