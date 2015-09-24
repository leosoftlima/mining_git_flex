package search;

import au.com.bytecode.opencsv.CSVWriter;
import util.Util;
import java.io.FileWriter;
import java.util.ArrayList;


public class Main {

    private static int counter = 0;
    private static int selectedCounter = 0;

    private static void resetCounters(){
        counter = 0;
        selectedCounter = 0;
    }

    private static void check(Repository repository, CSVWriter writer, int index) {
        try {
            repository.downloadZip();
            repository.unzip();
            if (!repository.hasFeatureFile()) {
                System.out.println("The project does not contain feature file!");
                repository.deleteAll();
            } else{
                System.out.println("The project does contain feature file!");
                selectedCounter++;
                repository.deleteUnzipedDir();
                writer.writeNext(new String[]{String.valueOf(index), repository.getUrl()});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void check(Repository repository) {
        try {
            repository.downloadZip();
            repository.unzip();
            if (!repository.hasFeatureFile()) {
                System.out.println("The project does not contain feature file!");
                repository.deleteAll();
            } else{
                System.out.println("The project does contain feature file!");
                selectedCounter++;
                repository.deleteUnzipedDir();
            }
        } catch (Exception e) {
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
        ArrayList<Repository> repositories;
        resetCounters();

        try {
            repositories = FileHandler.extractProjectsDefaultBranch();
            for(Repository repository: repositories){
                check(repository);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void searchCucumberProjects(){
        CSVWriter writer;
        ArrayList<Repository> repositories;
        resetCounters();

        try {
            repositories = FileHandler.extractProjects();
            writer = new CSVWriter(new FileWriter(Util.SELECTED_PROJECTS_FILE));
            writer.writeNext(new String[]{"index", "repository_url"});
            for(int i=0; i<repositories.size(); i++){
                check(repositories.get(i), writer, i+2);
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args){
        //Downloading and unzipping a project (example)
        checkProject("https://github.com/spgroup/rgms/archive/master.zip");

        // Downloading and unzipping projects from csv file
        searchCucumberProjects();
        System.out.printf("Number of analyzed projects: %d%n", counter);
        System.out.printf("Number of selected projects: %d (%.2f%%)%n", selectedCounter, ((double)selectedCounter/counter)*100);
    }

}
