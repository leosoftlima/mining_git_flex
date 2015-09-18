package search;

import au.com.bytecode.opencsv.CSVWriter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;


public class Main {

    private static void check(Repository repository, CSVWriter writer, int index) {
        try {
            repository.downloadZip();
            repository.unzip();
            if (!repository.hasFeatureFile()) {
                System.out.println("The project does not contain feature file!");
                repository.delete();
            } else{
                System.out.println("The project does contain feature file!");
                writer.writeNext(new String[]{String.valueOf(index), repository.getUrl()});
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void check(Repository repository) {
        try {
            repository.downloadZip();
            repository.unzip();
            if (!repository.hasFeatureFile()) {
                System.out.println("The project does not contain feature file!");
                repository.delete();
            } else{
                System.out.println("The project does contain feature file!");
            }
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
        CSVWriter writer;
        ArrayList<Repository> repositories;
        try {
            repositories = FileHandler.extractProjects();
            writer = new CSVWriter(new FileWriter(Util.SELECTED_PROJECTS_FILE));
            writer.writeNext(new String[]{"index", "repository_url"});
            for(int i=0; i<repositories.size(); i++){
                check(repositories.get(i), writer, i+2);
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args){
        //Downloading and unzipping a project (example)
        checkProject("https://github.com/spgroup/rgms/archive/master.zip");

        // Downloading and unzipping projects from csv file
        searchCucumberProjects();
    }

}
