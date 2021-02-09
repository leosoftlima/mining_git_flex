package br.ufpe.cin.tas.util

abstract class ConstantData {

    public static final String PROPERTIES_FILE_NAME = "configuration.properties"

    /* Project search */
    public static final String PROP_GITHUB_TOKEN = "spgroup.github.token"
    public static final String PROP_BIGQUERY_ID = "spgroup.bigquery.project.id"
    public static final String PROP_LANGUAGE_FILTER = "spgroup.project.search.language"
    public static final String PROP_STARS_FILTER = "spgroup.project.search.stars"
    public static final String PROP_YEAR_FILTER = "spgroup.project.search.year"
    public static final String PROP_COMMIT_MESSAGE_FILTER = "spgroup.project.search.commit.message"

    /* Project filter */
    public static final String PROP_FILE_TYPE_FILTER = "spgroup.project.filter.file"

    /* Tool */
    public static final String PROP_SEARCH_PROJECTS = "spgroup.search.projects"
    public static final String PROP_FILTER_PROJECTS = "spgroup.filter.projects"
    public static final String PROP_SEARCH_TASKS = "spgroup.search.tasks"
    public static final String PROP_SEARCH_PT_TASKS = "spgroup.search.pt.tasks"
    public static final String PROP_SEARCH_MERGES = "spgroup.search.merges"

    /* Defaults */
    public static final String DEFAULT_LANGUAGE = "ruby"
    public static final String DEFAULT_STARS = "500"
    public static final String DEFAULT_YEAR = "2010"
    public static final boolean DEFAULT_SEARCH_PROJECTS = true
    public static final boolean DEFAULT_FILTER_PROJECTS = true
    public static final boolean DEFAULT_SEARCH_TASKS = true
    public static final boolean DEFAULT_SEARCH_PT_TASKS = false
    public static final boolean DEFAULT_SEARCH_MERGES = true
    public static final String DEFAULT_UNIT_FOLDER = "spec${File.separator}"
    public static final String DEFAULT_GHERKIN_FOLDER = "features${File.separator}"
    public static final String DEFAULT_STEPS_FOLDER = "features${File.separator}step_definitions${File.separator}"
    public static final List<String> DEFAULT_PRODUCTION_FOLDERS = ["app", "lib"]
    public static final List<String> DEFAULT_PRODUCTION_FILES_EXTENSIONS = [".rb", ".html", ".html.haml",
                                                                            ".html.erb", ".html.slim", ".erb", ".haml", ".slim"]

    /* Files and folders */
    public static final String ZIP_FILE_URL = "/archive/"
    public static final String ZIPPED_FILES_FOLDER = "zipped${File.separator}"
    public static final String UNZIPPED_FILES_FOLDER = "unzipped${File.separator}"
    public static final String DOWNLOAD_PROBLEMS_FILE = "${ZIPPED_FILES_FOLDER}download-problems.txt"
    public static final String GITHUB_SEARCH_RESULT_FOLDER = "1-github${File.separator}"
    public static final String BIGQUERY_COMMITS_FILE = "${GITHUB_SEARCH_RESULT_FOLDER}commits.csv"
    public static final String REPOSITORIES_TO_DOWNLOAD_FILE = "${GITHUB_SEARCH_RESULT_FOLDER}projects.csv"
    public static final String FILTERED_RESULT_FOLDER = "2-filtered${File.separator}"
    public static final String CANDIDATE_REPOSITORIES_FILE = "${FILTERED_RESULT_FOLDER}candidate-projects.csv"
    public static final String TASKS_FOLDER = "3-tasks${File.separator}"
    public static final String TASKS_FILE = "${TASKS_FOLDER}tasks.csv"
    public static final String MERGES_FOLDER = "${TASKS_FOLDER}merges${File.separator}"
    public static final String MERGE_TASK_SUFIX = "_merges.csv"
    public static final String FASTFORWARD_MERGE_TASK_SUFIX = "_fastforward_merges.csv"
    public static final String PROBLEMATIC_MERGE_TASK_SUFIX = "_problematic_merges.csv"
    public static final String SELECTED_REPOSITORIES_FILE = "${TASKS_FOLDER}selected-projects.csv"
    public static final String OUTPUT_FOLDER = "output${File.separator}"
    public static final String LOG_FILE = "${OUTPUT_FOLDER}execution.log"
    public static final String REPOSITORY_FOLDER = "spg_repos${File.separator}"

    /* Files */
    public static final String GIT_EXTENSION = ".git"
    public static final String FILE_EXTENSION = ".zip"
    public static final String GITHUB_URL = "https://github.com/"

    /* FILTERING PRODUCTION AND TEST FILES  */
    public static final String PROP_UNIT_FOLDER = "spgroup.search.unit.folder"
    public static final String PROP_GHERKIN_FOLDER = "spgroup.search.gherkin.folder"
    public static final String PROP_STEPS_FOLDER = "spgroup.search.steps.folder"
    public static final String PROP_PRODUCTION_FOLDERS = "spgroup.search.production.folders"
    public static final String PROP_PROD_FILES_EXTENSIONS = "spgroup.search.production.extensions"
    public static final String VALID_TEST_EXTENSION = ".feature"

}
