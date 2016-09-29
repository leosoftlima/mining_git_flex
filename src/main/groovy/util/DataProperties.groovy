package util


class DataProperties {

    static final String INPUT_FOLDER = "input${File.separator}"
    static final String BIGQUERY_COMMITS_FILE = "${INPUT_FOLDER}commits.csv"
    static final String REPOSITORIES_TO_DOWNLOAD_FILE = "${INPUT_FOLDER}projects.csv"

    static final String OUTPUT_FOLDER = "output${File.separator}"
    static final String TASKS_FILE = "${OUTPUT_FOLDER}tasks.csv"
    static final String SELECTED_REPOSITORIES_FILE = "${OUTPUT_FOLDER}selected-projects.csv"
    static final String CANDIDATE_REPOSITORIES_FILE = "${OUTPUT_FOLDER}candidate-projects.csv"

    static final String FILE_EXTENSION = ".zip"
    static final String ZIP_FILE_URL = "/archive/"
    static final String ZIPPED_FILES_DIR = "zipped${File.separator}"
    static final String UNZIPPED_FILES_DIR = "unzipped${File.separator}"
    static final String DOWNLOAD_PROBLEMS_FILE = "${ZIPPED_FILES_DIR}download-problems.txt"

    static final String GITHUB_URL = "https://github.com/"

    static Properties props

    static {
        try {
            //if the file is in the classpath
            //ClassLoader loader = Thread.currentThread().getContextClassLoader()
            //InputStream resourceStream = loader.getResourceAsStream("configuration.properties")

            FileInputStream resourceStream = new FileInputStream("configuration.properties")
            props = new Properties()
            props.load(resourceStream)

            createFolder(INPUT_FOLDER)
            createFolder(OUTPUT_FOLDER)
            createFolder(ZIPPED_FILES_DIR)
            createFolder(UNZIPPED_FILES_DIR)

        }catch(Exception ex){
            println ex.message
        }
    }

    private static createFolder(String folder){
        File zipFolder = new File(folder - File.separator)
        if(!zipFolder.exists()){
            zipFolder.mkdir()
        }
    }

}
