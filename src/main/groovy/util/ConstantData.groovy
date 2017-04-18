package util

import java.util.regex.Matcher

interface ConstantData {

    String PROPERTIES_FILE_NAME = "configuration.properties"
    String PROP_GITHUB_LOGIN = "spgroup.github.user"
    String PROP_GITHUB_PASSWORD = "spgroup.github.password"
    String PROP_BIGQUERY_ID = "spgroup.bigquery.project.id"
    String PROP_TEST_PATH = "spgroup.task.interface.path.test"
    String PROP_REPOSITORY = "spgroup.repositories.folder"
    String PROP_GEMS = "spgroup.gems.path"
    String PROP_GEM_REQUIREALL = "spgroup.gems.requireall.folder"

    String PROP_LANGUAGE_FILTER = "spgroup.language"
    String PROP_FILE_TYPE_FILTER = "spgroup.search.file.extension"
    String PROP_COMMIT_MESSAGE_FILTER = "spgroup.search.commit.message"
    String PROP_GEMS_FILTER = "spgroup.search.gems"
    String PROP_STARS_FILTER = "spgroup.stars.filter"
    String PROP_YEAR_FILTER = "spgroup.year.filter"

    String DEFAULT_LANGUAGE = "ruby"
    String DEFAULT_GEM_REQUIREALL_FOLDER = "require_all-1.4.0"
    String DEFAULT_REPOSITORY_FOLDER = "repositories"
    String DEFAULT_STARS = "50"
    String DEFAULT_YEAR = "2010"

    String GITHUB_URL = "https://github.com/"
    String GIT_EXTENSION = ".git"
    String GEM_FILE = "Gemfile"
    String RAILS_GEM = "rails"
    String FILE_EXTENSION = ".zip"
    String ZIP_FILE_URL = "/archive/"
    String ZIPPED_FILES_FOLDER = "zipped${File.separator}"
    String UNZIPPED_FILES_FOLDER = "unzipped${File.separator}"
    String DOWNLOAD_PROBLEMS_FILE = "${ZIPPED_FILES_FOLDER}download-problems.txt"
    String MERGES_EXTRACTOR_FILE = "merge_extractor.rb"
    String GITHUB_SEARCH_RESULT_FOLDER = "1-github${File.separator}"
    String BIGQUERY_COMMITS_FILE = "${GITHUB_SEARCH_RESULT_FOLDER}commits.csv"
    String REPOSITORIES_TO_DOWNLOAD_FILE = "${GITHUB_SEARCH_RESULT_FOLDER}projects.csv"
    String FILTERED_RESULT_FOLDER = "2-filtered${File.separator}"
    String CANDIDATE_REPOSITORIES_FILE = "${FILTERED_RESULT_FOLDER}candidate-projects.csv"
    String TASKS_FOLDER = "3-tasks${File.separator}"
    String TASKS_FILE = "${TASKS_FOLDER}tasks.csv"
    String MERGE_ANALYSIS_TEMP_FILE = CANDIDATE_REPOSITORIES_FILE - ".csv"
    String MERGES_FOLDER = "${TASKS_FOLDER}merges${File.separator}"
    String MERGE_TASK_SUFIX = "_merges.csv"
    String SELECTED_REPOSITORIES_FILE = "${TASKS_FOLDER}selected-projects.csv"
    String GEM_SUFFIX = Matcher.quoteReplacement(File.separator) + "lib"
}
