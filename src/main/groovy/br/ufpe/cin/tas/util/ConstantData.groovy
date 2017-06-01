package br.ufpe.cin.tas.util

abstract class ConstantData {

    public static final String PROPERTIES_FILE_NAME = "configuration.properties"

    /* Project search */
    public static final String PROP_GITHUB_LOGIN = "spgroup.github.user"
    public static final String PROP_GITHUB_PASSWORD = "spgroup.github.password"
    public static final String PROP_BIGQUERY_ID = "spgroup.bigquery.project.id"
    public static final String PROP_LANGUAGE_FILTER = "spgroup.project.search.language"
    public static final String PROP_STARS_FILTER = "spgroup.project.search.stars"
    public static final String PROP_YEAR_FILTER = "spgroup.project.search.year"
    public static final String PROP_COMMIT_MESSAGE_FILTER = "spgroup.project.search.commit.message"

    /* Project filter */
    public static final String PROP_FILE_TYPE_FILTER = "spgroup.project.filter.file"
    public static final String PROP_GEMS_FILTER = "spgroup.project.filter.gems"

    /* Tool */
    public static final String PROP_SEARCH_PROJECTS = "spgroup.search.projects"
    public static final String PROP_FILTER_PROJECTS = "spgroup.filter.projects"
    public static final String PROP_SEARCH_TASKS = "spgroup.search.tasks"

    /* Defaults */
    public static final String DEFAULT_LANGUAGE = "ruby"
    public static final String DEFAULT_STARS = "500"
    public static final String DEFAULT_YEAR = "2010"
    public static final boolean DEFAULT_SEARCH_PROJECTS = true
    public static final boolean DEFAULT_FILTER_PROJECTS = true
    public static final boolean DEFAULT_SEARCH_TASKS = true

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
    public static final String SELECTED_REPOSITORIES_FILE = "${TASKS_FOLDER}selected-projects.csv"
    public static final String OUTPUT_FOLDER = "output${File.separator}"
    public static final String LOG_FILE = "${OUTPUT_FOLDER}execution.log"
    public static final String REPOSITORY_FOLDER = "repositories${File.separator}"

    /* Files */
    public static final String GIT_EXTENSION = ".git"
    public static final String FILE_EXTENSION = ".zip"
    public static final String GITHUB_URL = "https://github.com/"

    /* Rails files */
    public static final String GEM_FILE = "Gemfile"
    public static final String RAILS_GEM = "rails"
    public static final String UNIT_TEST_FILES_RELATIVE_PATH = "spec${File.separator}"
    public static final String GHERKIN_FILES_RELATIVE_PATH = "features${File.separator}"
    public static final String STEPS_FILES_RELATIVE_PATH = "${GHERKIN_FILES_RELATIVE_PATH}step_definitions${File.separator}"
    public static final String PRODUCTION_FILES_RELATIVE_PATH = "app${File.separator}"
    public static final String LIB_RELATIVE_PATH = "lib${File.separator}"
    public static final List<String> VALID_FOLDERS = [GHERKIN_FILES_RELATIVE_PATH, UNIT_TEST_FILES_RELATIVE_PATH,
                                               PRODUCTION_FILES_RELATIVE_PATH, LIB_RELATIVE_PATH]
    public static final List<String> VALID_EXTENSIONS = [".rb", ".html", ".html.haml", ".html.erb", ".html.slim", ".feature"]

    /* Tasks */
    public static int TASK_LIMIT = 100
}
