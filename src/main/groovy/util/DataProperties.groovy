package util


class DataProperties {

    static final String BIGQUERY_COMMITS_FILE = "input"+File.separator+"commits.csv"
    static final String REPOSITORIES_TO_DOWNLOAD_FILE = "input"+File.separator+"projects.csv"

    static final String TASKS_FILE = "output"+File.separator+"tasks.csv"
    static final String SELECTED_REPOSITORIES_FILE = "output"+File.separator+"selected-projects.csv"
    static final String CANDIDATE_REPOSITORIES_FILE = "output"+File.separator+"candidate-projects.csv"

    static final String DOWNLOAD_PROBLEMS_FILE = "zipped"+File.separator+"download-problems.txt"
    static final String FILE_EXTENSION = ".zip"
    static final String ZIP_FILE_URL = "/archive/"
    static final String ZIPPED_FILES_DIR = "zipped"+File.separator
    static final String UNZIPPED_FILES_DIR = "unzipped"+File.separator

    static final String GITHUB_URL = "https://github.com/"
}
