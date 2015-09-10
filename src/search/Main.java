package search;

import java.io.IOException;
import java.util.ArrayList;


public class Main {

    public static void main(String[] args){

        //Downloading and unzipping a project
        Repository repo = new Repository("https://github.com/spgroup/rgms", "master");
        repo.downloadCommits();
        repo.unzipCommits();

        /* Downloading and unzipping projects from csv file --- DOES NOT WORK
        try {
            ArrayList<Repository> repositories = FileHandler.extractProjects();
            for(Repository repository: repositories){
                repository.downloadCommits();
                repository.unzipCommits();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }*/

    }

}
