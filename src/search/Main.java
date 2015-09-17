package search;

import java.io.IOException;
import java.util.ArrayList;


public class Main {

    private static void check(Repository repository) {
        try {
            repository.downloadZip();
            repository.unzip();
            if (!repository.hasFeatureFile()) {
                System.out.println("The project does not contain feature file!");
                repository.delete();
            } else System.out.println("The project does contain feature file!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void checkProject(String url){
        Repository repo = new Repository(url);
        check(repo);
    }

    public static void checkProject(String url, String branch){
        Repository repo = new Repository(url, branch);
        check(repo);
    }

    public static void searchCucumberProjectsDefaultBranch(){
        ArrayList<Repository> repositories = new ArrayList<>();
        try {
            repositories = FileHandler.extractProjectsDefaultBranch();
        } catch (IOException e) {
            e.printStackTrace();
        }
        for(Repository repository: repositories){
            check(repository);
        }
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
        //Downloading and unzipping a project (example)
        checkProject("https://github.com/spgroup/rgms/archive/master.zip");

        // Downloading and unzipping projects from csv file
        searchCucumberProjects();
    }

}
