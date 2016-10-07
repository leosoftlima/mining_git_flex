package util

import groovy.util.logging.Slf4j

import java.util.regex.Matcher

@Slf4j
class DataProperties {

    public static final String INPUT_FOLDER = "commits${File.separator}"
    public static final String BIGQUERY_COMMITS_FILE = "${INPUT_FOLDER}commits.csv"
    public static final String REPOSITORIES_TO_DOWNLOAD_FILE = "${INPUT_FOLDER}projects.csv"

    public static final String OUTPUT_FOLDER = "tasks${File.separator}"
    public static final String TASKS_FILE = "${OUTPUT_FOLDER}tasks.csv"
    public static final String SELECTED_REPOSITORIES_FILE = "${OUTPUT_FOLDER}selected-projects.csv"
    public static final String CANDIDATE_REPOSITORIES_FILE = "${OUTPUT_FOLDER}candidate-projects.csv"

    public static final String FILE_EXTENSION = ".zip"
    public static final String ZIP_FILE_URL = "/archive/"
    public static final String ZIPPED_FILES_FOLDER = "zipped${File.separator}"
    public static final String UNZIPPED_FILES_FOLDER = "unzipped${File.separator}"
    public static final String DOWNLOAD_PROBLEMS_FILE = "${ZIPPED_FILES_FOLDER}download-problems.txt"

    public static final String GITHUB_URL = "https://github.com/"
    public static final String GIT_EXTENSION = ".git"
    public static final String REPOSITORY_FOLDER

    public static final File configFile
    public static final Properties configProperties

    public static final String FILE_TYPE
    public static final boolean FILTER_BY_DEFAULT_MESSAGE
    public static final boolean FILTER_BY_PIVOTAL_TRACKER
    public static final String BIGQUERY_PROJECT_ID
    public static final String LANGUAGE

    public static final TEST_CODE_REGEX

    static {
        try {
            configFile = new File("configuration.properties")
            FileInputStream resourceStream = new FileInputStream(configFile)
            configProperties = new Properties()
            configProperties.load(resourceStream)

            def folder = configProperties.getProperty("spgroup.repositories.folder")
            if(folder == null || folder=="") REPOSITORY_FOLDER = "repositories${File.separator}"
            else REPOSITORY_FOLDER = "$folder${File.separator}"

            createFolders()

            BIGQUERY_PROJECT_ID = configProperties.getProperty("spgroup.bigquery.project.id")

            def language = configProperties.getProperty("spgroup.language")
            LANGUAGE = language?.charAt(0)?.toString()?.toUpperCase() + language?.substring(1)
            //capitalizes the first letter

            String filterMessage = configProperties.getProperty("spgroup.search.commit.message").toLowerCase()
            if (filterMessage == null | filterMessage == "") filterMessage = "false"
            switch (filterMessage) {
                case "default":
                    FILTER_BY_DEFAULT_MESSAGE = true
                    FILTER_BY_PIVOTAL_TRACKER = false
                    break
                case "pivotal":
                    FILTER_BY_DEFAULT_MESSAGE = false
                    FILTER_BY_PIVOTAL_TRACKER = true
                    break
                default:
                    FILTER_BY_DEFAULT_MESSAGE = false
                    FILTER_BY_PIVOTAL_TRACKER = false
            }

            def fileType = "." + configProperties.getProperty("spgroup.search.file.extension").toLowerCase()
            if (fileType.length() == 1) fileType = ""
            FILE_TYPE = fileType

            TEST_CODE_REGEX = configTestCodeRegex()

        } catch (Exception ex) {
            log.info ex.message
        }

    }

    private static createFolders() {
        Util.createFolder(INPUT_FOLDER)
        Util.createFolder(OUTPUT_FOLDER)
        Util.createFolder(ZIPPED_FILES_FOLDER)
        Util.createFolder(UNZIPPED_FILES_FOLDER)
        Util.createFolder(REPOSITORY_FOLDER)
    }

    private static configTestCodeRegex() {
        def testPath = configProperties.getProperty("spgroup.task.interface.path.test").split(",")*.replaceAll(" ", "")

        def regex
        if (testPath.size() > 1) {
            regex = ".*("
            testPath.each { dir ->
                regex += dir + "|"
            }
            regex = regex.substring(0, regex.lastIndexOf("|"))
            regex += ").*"
        } else {
            regex = ".*${testPath.get(0)}.*"
        }

        regex = regex.replaceAll(RegexUtil.FILE_SEPARATOR_REGEX,
                Matcher.quoteReplacement(File.separator) + Matcher.quoteReplacement(File.separator))
    }

}
