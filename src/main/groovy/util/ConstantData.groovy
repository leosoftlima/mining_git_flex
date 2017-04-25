package util

interface ConstantData {

    final String PROPERTIES_FILE_NAME = "configuration.properties"

    /* Project search */
    final String PROP_GITHUB_LOGIN = "spgroup.github.user"
    final String PROP_GITHUB_PASSWORD = "spgroup.github.password"
    final String PROP_BIGQUERY_ID = "spgroup.bigquery.project.id"
    final String PROP_LANGUAGE_FILTER = "spgroup.project.search.language"
    final String PROP_STARS_FILTER = "spgroup.project.search.stars"
    final String PROP_YEAR_FILTER = "spgroup.project.search.year"
    final String PROP_COMMIT_MESSAGE_FILTER = "spgroup.project.search.commit.message"

    /* Project filter */
    String PROP_FILE_TYPE_FILTER = "spgroup.project.filter.file"
    String PROP_GEMS_FILTER = "spgroup.project.filter.gems"

    /* Task search */
    final String PROP_TASK_LIMIT = "spgroup.task.search.limit"

    final String PROP_SEARCH_PROJECTS = "spgroup.search.projects"
    final String PROP_FILTER_PROJECTS = "spgroup.filter.projects"
    final String PROP_SEARCH_TASKS = "spgroup.search.tasks"

    String DEFAULT_LANGUAGE = "ruby"
    String DEFAULT_STARS = "500"
    String DEFAULT_YEAR = "2010"
    String DEFAULT_TASK_LIMIT = "10"
    boolean DEFAULT_SEARCH_PROJECTS = true
    boolean DEFAULT_FILTER_PROJECTS = true
    boolean DEFAULT_SEARCH_TASKS = true

    String ZIP_FILE_URL = "/archive/"
    String ZIPPED_FILES_FOLDER = "zipped${File.separator}"
    String UNZIPPED_FILES_FOLDER = "unzipped${File.separator}"
    String DOWNLOAD_PROBLEMS_FILE = "${ZIPPED_FILES_FOLDER}download-problems.txt"
    String GITHUB_SEARCH_RESULT_FOLDER = "1-github${File.separator}"
    String BIGQUERY_COMMITS_FILE = "${GITHUB_SEARCH_RESULT_FOLDER}commits.csv"
    String REPOSITORIES_TO_DOWNLOAD_FILE = "${GITHUB_SEARCH_RESULT_FOLDER}projects.csv"
    String FILTERED_RESULT_FOLDER = "2-filtered${File.separator}"
    String CANDIDATE_REPOSITORIES_FILE = "${FILTERED_RESULT_FOLDER}candidate-projects.csv"
    String TASKS_FOLDER = "3-tasks${File.separator}"
    String TASKS_FILE = "${TASKS_FOLDER}tasks.csv"
    String MERGES_FOLDER = "${TASKS_FOLDER}merges${File.separator}"
    String MERGE_TASK_SUFIX = "_merges.csv"
    String SELECTED_REPOSITORIES_FILE = "${TASKS_FOLDER}selected-projects.csv"
    String OUTPUT_FOLDER = "output${File.separator}"
    String LOG_FILE = "${OUTPUT_FOLDER}execution.log"
    String REPOSITORY_FOLDER = "repositories${File.separator}"

    String GIT_EXTENSION = ".git"
    String FILE_EXTENSION = ".zip"
    String GITHUB_URL = "https://github.com/"
    String GEM_FILE = "Gemfile"
    String RAILS_GEM = "rails"
    String UNIT_TEST_FILES_RELATIVE_PATH = "spec${File.separator}"
    String GHERKIN_FILES_RELATIVE_PATH = "features${File.separator}"
    String STEPS_FILES_RELATIVE_PATH = "features/step_definitions${File.separator}"
}
