package search;


import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import input.InputManager;
import util.Util;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class SearchManager {

    private int counter;
    private final ArrayList<Repository> selected;

    public SearchManager(){
        selected = new ArrayList<>();
    }

    private void resetCounters(){
        counter = 0;
        selected.clear();
    }

    private void listSelectedRepositories(){
        for(Repository r: selected){
            System.out.printf("url: %s, branch: %s, zip: %s%n", r.getUrl(), r.getBranch(), r.getLocalZipName());
        }
    }

    /**
     * Generates a list of repositories extracted from a csv file (/input/projects.csv). The first column must identify
     * the repository's url. It considers the default name for master branch (master).
     *
     * @throws IOException if there's an error reading the csv file.
     */
    private static ArrayList<Repository> extractRepositoriesDefaultBranch() throws IOException {
        ArrayList<Repository> repos = new ArrayList<>();

        InputManager.prepareInput();
        CSVReader reader = new CSVReader(new FileReader(Util.PREPARED_PROJECTS_FILE));
        List<String[]> entries = reader.readAll();
        reader.close();

        if(entries.size()>0) entries.remove(0); //ignore sheet header

        for(String[] line: entries){
            repos.add(new Repository(line[0], "master")); //url, branch
        }
        return repos;
    }

    /**
     * Generates a list of repositories extracted from a csv file (/input/projects.csv). The first column must identify
     * the repository's url and the second column must identify its master branch.
     *
     * @throws IOException if there's an error reading the csv file.
     */
    private static ArrayList<Repository> extractRepositories() throws IOException {
        ArrayList<Repository> repos = new ArrayList<>();

        InputManager.prepareInput();
        CSVReader reader = new CSVReader(new FileReader(Util.PREPARED_PROJECTS_FILE));
        List<String[]> entries = reader.readAll();
        reader.close();

        if(entries.size()>0) entries.remove(0); //ignore sheet header

        for(String[] line: entries){
            repos.add(new Repository(line[0], line[1])); //url, branch
        }
        return repos;
    }

    /**
     * Verifies if the repository does contain Gherkin files and updates a csv file to include the analysed repository if
     * the result is positive.
     *
     * @param repository the repository to analyse.
     * @param writer cvs file to update.
     * @param index original index of the analysed repository.
     */
    private void searchGherkinFile(Repository repository, CSVWriter writer, int index) {
        counter++;
        if (repository.hasGherkinFile()) {
            System.out.println("The repository does contain feature file!");
            selected.add(repository);
            repository.deleteUnzipedDir();
            writer.writeNext(new String[]{String.valueOf(index), repository.getUrl()});
        } else{
            System.out.println("The repository does not contain feature file!");
            repository.deleteAll();
        }
    }

    /**
     * Verifies if the repository does contain Gherkin files.
     *
     * @param repository the repository to analyse.
     */
    private void searchGherkinFile(Repository repository) {
        if (repository.hasGherkinFile()) {
            System.out.println("The repository does contain feature file!");
            selected.add(repository);
            repository.deleteUnzipedDir();
        } else{
            System.out.println("The repository does not contain feature file!");
            repository.deleteAll();
        }
    }

    /**
     * Verifies if a zip file contains Gherkin files.
     *
     * @param zipUrl the url from GitHub repository's zip file.
     */
    public void analyseRepository(String zipUrl){
        Repository repository = new Repository(zipUrl);
        searchGherkinFile(repository);
    }

    /**
     * Verifies if a GitHub repository contains Gherkin files.
     *
     * @param url the url from GitHub repository.
     * @param branch the repository's main branch.
     */
    public void analyseRepository(String url, String branch){
        Repository repository = new Repository(url, branch);
        searchGherkinFile(repository);
    }

    /**
     * Verifies if the GitHub repositories does contain Gherkin files. The repositories are identified at a text file by
     * the url of its zip file.
     *
     * @param file text file that contains the url of zip files from GitHub projects.
     *
     */
    public void checkProjectsFromUrlFile(String file) throws FileNotFoundException {
        resetCounters();
        Scanner scanner = new Scanner(new File(file));
        while (scanner.hasNextLine()){
            String url = scanner.nextLine();
            System.out.println(url);
            analyseRepository(url);
        }
        System.out.printf("Number of analyzed projects: %d%n", counter);
        System.out.printf("Number of selected projects: %d (%.2f%%)%n", selected.size(),((double)selected.size()/counter)*100);
        listSelectedRepositories();
    }

    /**
     * Verifies if the GitHub repositories identified at a csv file (/input/projects.csv) does contain Gherkin files.
     * The first column must identify the repository's url and the second column must identify its master branch.
     *
     */
    public void searchGherkinProjects(){
        CSVWriter writer;
        ArrayList<Repository> repositories;
        resetCounters();

        try {
            repositories = extractRepositories();

            writer = new CSVWriter(new FileWriter(Util.SELECTED_PROJECTS_FILE));
            writer.writeNext(new String[]{"index", "repository_url"});

            for(int i=0; i<repositories.size(); i++){
                searchGherkinFile(repositories.get(i), writer, i + 2);
            }

            writer.close();

            System.out.printf("Number of analyzed projects: %d%n", counter);
            System.out.printf("Number of selected projects: %d (%.2f%%)%n", selected.size(),((double)selected.size()/counter)*100);
            listSelectedRepositories();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Verifies if the GitHub repositories identified at a csv file (/input/projects.csv) does contain Gherkin files.
     * The first column must identify the repository's url. It considers the default name for master branch (master)
     *
     */
    public void searchGherkinProjectsDefaultBranch(){
        CSVWriter writer;
        ArrayList<Repository> repositories;
        resetCounters();

        try {
            repositories = extractRepositoriesDefaultBranch();

            writer = new CSVWriter(new FileWriter(Util.SELECTED_PROJECTS_FILE));
            writer.writeNext(new String[]{"index", "repository_url"});

            for(int i=0; i<repositories.size(); i++){
                searchGherkinFile(repositories.get(i), writer, i + 2);
            }

            writer.close();

            System.out.printf("Number of analyzed projects: %d%n", counter);
            System.out.printf("Number of selected projects: %d (%.2f%%)%n", selected.size(),((double)selected.size()/counter)*100);
            listSelectedRepositories();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
