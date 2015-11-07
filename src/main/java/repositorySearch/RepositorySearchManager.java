package repositorySearch;


import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import input.InputManager;
import net.wagstrom.research.github.GithubProperties;
import util.SearchProperties;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

public class RepositorySearchManager {

    private int counter;
    private final ArrayList<Repository> candidates;
    private String fileType;

    public RepositorySearchManager(){
        candidates = new ArrayList<>();
        Properties props = GithubProperties.props();
        fileType = "."+props.getProperty("spgroup.search.file.extension");
        if(fileType.length() == 1) fileType = "";
    }

    private void resetCounters(){
        counter = 0;
        candidates.clear();
    }

    private void listCandidateRepositories(){
        for(Repository r: candidates){
            System.out.printf("url: %s, branch: %s, zip: %s%n", r.getUrl(), r.getBranch(), r.getLocalZipName());
        }
    }

    /**
     * Generates a list of repositories extracted from a csv file (/input/commits.csv). The first column must identify
     * the repository's url. It considers the default name for master branch (master).
     *
     * @throws IOException if there's an error reading the csv file.
     */
    private static ArrayList<Repository> extractRepositoriesDefaultBranch() throws IOException {
        ArrayList<Repository> repos = new ArrayList<>();

        InputManager.prepareInput();
        CSVReader reader = new CSVReader(new FileReader(SearchProperties.REPOSITORIES_TO_DOWNLOAD_FILE));
        List<String[]> entries = reader.readAll();
        reader.close();

        if(entries.size()>0) entries.remove(0); //ignore sheet header

        for(String[] line: entries){
            repos.add(new Repository(line[0], "master")); //url, branch
        }
        return repos;
    }

    /**
     * Generates a list of repositories extracted from a csv file (/input/commits.csv). The first column must identify
     * the repository's url and the second column must identify its master branch.
     *
     * @throws IOException if there's an error reading the csv file.
     */
    private static ArrayList<Repository> extractRepositories() throws IOException {
        ArrayList<Repository> repos = new ArrayList<>();

        InputManager.prepareInput();
        CSVReader reader = new CSVReader(new FileReader(SearchProperties.REPOSITORIES_TO_DOWNLOAD_FILE));
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
    private void searchFileType(Repository repository, CSVWriter writer, int index) {
        counter++;
        if (repository.hasFileType(fileType)) {
            candidates.add(repository);
            repository.deleteUnzipedDir();
            writer.writeNext(new String[]{String.valueOf(index), repository.getUrl()});
        } else{
            repository.deleteAll();
        }
    }

    /**
     * Verifies if the repository does contain files of a specific type (spgroup.search.file.extension at configuration.properties).
     *
     * @param repository the repository to analyse.
     */
    private void searchFileType(Repository repository) {
        if (repository.hasFileType(fileType)) {
            candidates.add(repository);
            repository.deleteUnzipedDir();
        } else{
            repository.deleteAll();
        }
    }

    /**
     * Verifies if a zip file contains files of a specific type (spgroup.search.file.extension at configuration.properties).
     *
     * @param zipUrl the url from GitHub repository's zip file.
     */
    public void analyseRepository(String zipUrl){
        Repository repository = new Repository(zipUrl);
        searchFileType(repository);
    }

    /**
     * Verifies if a GitHub repository contains files of a specific type (spgroup.search.file.extension at configuration.properties).
     *
     * @param url the url from GitHub repository.
     * @param branch the repository's main branch.
     */
    public void analyseRepository(String url, String branch){
        Repository repository = new Repository(url, branch);
        searchFileType(repository);
    }

    /**
     * Verifies if the GitHub repositories does contain files of a specific type (spgroup.search.file.extension at configuration.properties).
     * The repositories are identified at a text file by the url of its zip file.
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
        System.out.printf("Number of candidates projects: %d (%.2f%%)%n", candidates.size(),((double) candidates.size()/counter)*100);
        listCandidateRepositories();
    }

    /**
     * Verifies if the GitHub repositories identified at a csv file (/input/commits.csv) does contain files of a specific type.
     * The first column must identify the repository's url and the second column must identify its master branch.
     *
     */
    public void searchRepositoriesByFileType(){
        CSVWriter writer;
        ArrayList<Repository> repositories;
        resetCounters();

        try {
            repositories = extractRepositories();
            System.out.printf("The repositories to search for are saved in %s%n", SearchProperties.REPOSITORIES_TO_DOWNLOAD_FILE);

            writer = new CSVWriter(new FileWriter(SearchProperties.CANDIDATE_REPOSITORIES_FILE));
            writer.writeNext(new String[]{"index", "repository_url"});

            if(fileType.isEmpty()){
                for (int i = 0; i < repositories.size(); i++) {
                    candidates.add(repositories.get(i));
                    writer.writeNext(new String[]{String.valueOf(i + 2), repositories.get(i).getUrl()});
                }
            }
            else {
                for (int i = 0; i < repositories.size(); i++) {
                    searchFileType(repositories.get(i), writer, i + 2);
                }
                System.out.printf("Number of analyzed projects: %d%n", counter);
                System.out.printf("Number of candidates projects: %d (%.2f%%)%n", candidates.size(),((double) candidates.size()/counter)*100);
            }

            writer.close();
            listCandidateRepositories();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Verifies if the GitHub repositories identified at a csv file (/input/commits.csv) does contain Gherkin files.
     * The first column must identify the repository's url. It considers the default name for master branch (master)
     *
     */
    public void searchRepositoriesByFileTypeDefaultBranch(){
        CSVWriter writer;
        ArrayList<Repository> repositories;
        resetCounters();

        try {
            repositories = extractRepositoriesDefaultBranch();
            System.out.printf("The repositories to search for are saved in %s%n", SearchProperties.REPOSITORIES_TO_DOWNLOAD_FILE);

            writer = new CSVWriter(new FileWriter(SearchProperties.CANDIDATE_REPOSITORIES_FILE));
            writer.writeNext(new String[]{"index", "repository_url"});

            if(fileType.isEmpty()){
                for (int i = 0; i < repositories.size(); i++) {
                    candidates.add(repositories.get(i));
                    writer.writeNext(new String[]{String.valueOf(i + 2), repositories.get(i).getUrl()});
                }
            }
            else {
                for (int i = 0; i < repositories.size(); i++) {
                    searchFileType(repositories.get(i), writer, i + 2);
                }
                System.out.printf("Number of analyzed projects: %d%n", counter);
                System.out.printf("Number of candidate projects: %d (%.2f%%)%n", candidates.size(),((double) candidates.size()/counter)*100);
            }

            writer.close();
            listCandidateRepositories();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
