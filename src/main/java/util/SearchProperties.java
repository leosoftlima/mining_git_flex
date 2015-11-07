package util;


import java.io.File;

public class SearchProperties {


    public static final String BIGQUERY_COMMITS_FILE = "input"+File.separator+"commits.csv";
    public static final String REPOSITORIES_TO_DOWNLOAD_FILE = "input"+File.separator+"projects.csv";

    public static final String TASKS_FILE = "output"+File.separator+"tasks.csv";
    public static final String SELECTED_REPOSITORIES_FILE = "output"+File.separator+"selected-projects.csv";
    public static final String CANDIDATE_REPOSITORIES_FILE = "output"+File.separator+"candidate-projects.csv";

    public static final String DOWNLOAD_PROBLEMS_FILE = "zipped"+File.separator+"download-problems.txt";
    public static final String FILE_EXTENSION = ".zip";
    public static final String ZIP_FILE_URL = "/archive/";
    public static final String ZIPPED_FILES_DIR = "zipped"+File.separator;
    public static final String UNZIPPED_FILES_DIR = "unzipped"+File.separator;

    public static final String GITHUB_URL = "https://github.com/";

}
