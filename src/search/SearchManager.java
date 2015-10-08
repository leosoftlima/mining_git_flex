package search;


import au.com.bytecode.opencsv.CSVWriter;
import util.Util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Scanner;

public class SearchManager {

    private int counter;
    private ArrayList<Repository> selected;

    public SearchManager(){
        selected = new ArrayList<>();
    }

    private void resetCounters(){
        counter = 0;
        selected.clear();
    }

    private void check(Repository repository, CSVWriter writer, int index) {
        try {
            counter++;
            repository.downloadZip();
            repository.unzip();
            if (!repository.hasFeatureFile()) {
                System.out.println("The project does not contain feature file!");
                repository.deleteAll();
            } else{
                System.out.println("The project does contain feature file!");
                selected.add(repository);
                repository.deleteUnzipedDir();
                writer.writeNext(new String[]{String.valueOf(index), repository.getUrl()});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void check(Repository repository) {
        try {
            counter++;
            repository.downloadZip();
            repository.unzip();
            if (!repository.hasFeatureFile()) {
                System.out.println("The project does not contain feature file!");
                repository.deleteAll();
            } else{
                System.out.println("The project does contain feature file!");
                selected.add(repository);
                repository.deleteUnzipedDir();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* The input is the zip file url. */
    public void checkProjectFromUrl(String url){
        Repository repo = new Repository(url);
        check(repo);
    }

    public void checkProjectsFromUrlFile(String file) throws FileNotFoundException {
        Scanner scanner = new Scanner(new File(file));
        while (scanner.hasNextLine()){
            String url = scanner.nextLine();
            System.out.println(url);
            checkProjectFromUrl(url);
        }
    }

    public void checkProjectFromUrl(String url, String branch){
        Repository repo = new Repository(url, branch);
        check(repo);
    }

    public void searchCucumberProjectsDefaultBranch(){
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

    public void searchCucumberProjects(){
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

    public int getCounter(){
        return counter;
    }

    public int getSelectedCounter(){
        return selected.size();
    }

    public void listSelectedRepositories(){
        for(Repository r: selected){
            System.out.printf("url: %s, branch: %s, zip: %s%n", r.getUrl(), r.getBranch(), r.getZipfileName());
        }
    }

}
