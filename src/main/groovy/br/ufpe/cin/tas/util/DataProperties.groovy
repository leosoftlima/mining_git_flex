package br.ufpe.cin.tas.util

import groovy.util.logging.Slf4j
import java.util.regex.Matcher

@Slf4j
class DataProperties {

    public static final Properties properties
    public static final String BIGQUERY_PROJECT_ID
    public static final String LANGUAGE
    public static final String GITHUB_TOKEN
    public static final boolean SIMPLE_GITHUB_SEARCH

    public static final String FILE_TYPE
    public static final boolean FILTER_BY_DEFAULT_MESSAGE
    public static final boolean FILTER_BY_PIVOTAL_TRACKER
    public static final boolean FILTER_BY_FILE
    public static final String FILTER_STARS
    public static final String FILTER_YEAR
    public static final boolean FILTER_BY_LAST_UPDATE

    public static final boolean SEARCH_PROJECTS
    public static final boolean FILTER_PROJECTS
    public static final boolean SEARCH_TASKS
    public static final boolean SEARCH_PT_TASKS
    public static final List<String> PRODUCTION_FOLDERS
    public static final List<String> VALID_PROD_FILES_EXTENSION
    public static final String UNIT_FOLDER
    public static final String GHERKIN_FOLDER
    public static final String STEPS_FOLDER
    public static final boolean SEARCH_MERGES

    static {
        try {
            properties = new Properties()
            loadProperties()

            createFolders()

            SIMPLE_GITHUB_SEARCH = !FILTER_BY_DEFAULT_MESSAGE && !FILTER_BY_PIVOTAL_TRACKER
            BIGQUERY_PROJECT_ID = configBigQuery()
            def searchConfig = configureSearchMechanism()
            FILTER_BY_DEFAULT_MESSAGE = searchConfig.default
            FILTER_BY_PIVOTAL_TRACKER = searchConfig.pivotal

            LANGUAGE = configureLanguage()

            GITHUB_TOKEN = configGithubLogin()

            def fileFilter = configureFileFilter()
            FILE_TYPE = fileFilter.fileType
            FILTER_BY_FILE = fileFilter.filter

            FILTER_STARS = configureStarsFilter()
            FILTER_BY_LAST_UPDATE = FILTER_STARS.empty
            FILTER_YEAR = configureYearFilter()

            SEARCH_PROJECTS = configureMandatoryBooleanProperties(properties.(ConstantData.PROP_SEARCH_PROJECTS),
                    ConstantData.DEFAULT_SEARCH_PROJECTS)
            FILTER_PROJECTS = configureMandatoryBooleanProperties(properties.(ConstantData.PROP_FILTER_PROJECTS),
                    ConstantData.DEFAULT_FILTER_PROJECTS)
            SEARCH_TASKS = configureMandatoryBooleanProperties(properties.(ConstantData.PROP_SEARCH_TASKS),
                    ConstantData.DEFAULT_SEARCH_TASKS)
            SEARCH_MERGES = configureMandatoryBooleanProperties(properties.(ConstantData.PROP_SEARCH_MERGES),
                    ConstantData.DEFAULT_SEARCH_MERGES)
            SEARCH_PT_TASKS = configureMandatoryBooleanProperties(properties.(ConstantData.PROP_SEARCH_PT_TASKS),
                    ConstantData.DEFAULT_SEARCH_PT_TASKS)
            PRODUCTION_FOLDERS = configureProductionFolders()
            VALID_PROD_FILES_EXTENSION = configureProductionFiles()
            UNIT_FOLDER = configureMandatoryProperties(properties.(ConstantData.PROP_UNIT_FOLDER),
                    ConstantData.DEFAULT_UNIT_FOLDER)
            if(!UNIT_FOLDER.endsWith(File.separator)) UNIT_FOLDER += File.separator
            GHERKIN_FOLDER = configureMandatoryProperties(properties.(ConstantData.PROP_GHERKIN_FOLDER),
                    ConstantData.DEFAULT_GHERKIN_FOLDER)
            if(!GHERKIN_FOLDER.endsWith(File.separator)) GHERKIN_FOLDER += File.separator
            STEPS_FOLDER = configureMandatoryProperties(properties.(ConstantData.PROP_STEPS_FOLDER),
                    ConstantData.DEFAULT_STEPS_FOLDER)
            if(!STEPS_FOLDER.endsWith(File.separator)) STEPS_FOLDER += File.separator

        } catch (Exception ex) {
            log.info ex.message
            ex.stackTrace.each{ log.info it.toString() }
        }

        log.info "SEARCH_PROJECTS: ${SEARCH_PROJECTS}"
        log.info "FILTER_PROJECTS: ${FILTER_PROJECTS}"
        log.info "SEARCH_TASKS: ${SEARCH_TASKS}"
        log.info "SEARCH_PT_TASKS: ${SEARCH_PT_TASKS}"
        log.info "FILTER_BY_FILE: ${FILTER_BY_FILE}; FILE_TYPE: ${FILE_TYPE}"
        log.info "FILTER_BY_DEFAULT_MESSAGE: ${FILTER_BY_DEFAULT_MESSAGE}"
        log.info "FILTER_BY_PIVOTAL_TRACKER: ${FILTER_BY_PIVOTAL_TRACKER}"
        log.info "FILTER_STARS: ${FILTER_STARS}"
        log.info "FILTER_YEAR: ${FILTER_YEAR}"

    }

    private static configureProductionFolders(){
        def folders = properties.(ConstantData.PROP_PRODUCTION_FOLDERS)
        def foldersSet
        if(!folders || folders.empty) {
            foldersSet = ConstantData.DEFAULT_PRODUCTION_FOLDERS
        }
        else {
            foldersSet = folders?.split(",")*.replaceAll(" ", "")
        }
        foldersSet
    }
    private static configureProductionFiles(){
        def folders = properties.(ConstantData.PROP_PROD_FILES_EXTENSIONS)
        def foldersSet
        if(!folders || folders.empty) {
            foldersSet = ConstantData.DEFAULT_PRODUCTION_FILES_EXTENSIONS
        }
        else {
            foldersSet = folders?.split(",")*.replaceAll(" ", "")
        }
        foldersSet
    }

    private static loadProperties() {
        File configFile = new File(ConstantData.PROPERTIES_FILE_NAME)
        FileInputStream resourceStream = new FileInputStream(configFile)
        properties.load(resourceStream)
    }

    private static configureMandatoryBooleanProperties(value, defaultValue) {
        if (value==null || value.empty || (value!="true" && value!="false")) value = defaultValue
        Boolean.parseBoolean(value)
    }

    private static configureMandatoryProperties(value, defaultValue) {
        if (!value || value.empty) value = defaultValue
        value.replaceAll(RegexUtil.FILE_SEPARATOR_REGEX, Matcher.quoteReplacement(File.separator))
    }

    private static configureLanguage(){
        def value = configureMandatoryProperties(properties.(ConstantData.PROP_LANGUAGE_FILTER), ConstantData.DEFAULT_LANGUAGE)
        if(SIMPLE_GITHUB_SEARCH) value.trim().toLowerCase()
        else value?.substring(0,1)?.toUpperCase() + value?.substring(1)
    }

    private static configureSearchMechanism() {
        def defaultOption
        def pivotalOption
        String filterMessage = properties.(ConstantData.PROP_COMMIT_MESSAGE_FILTER).toLowerCase()
        if (filterMessage == null | filterMessage == "") filterMessage = "false"
        switch (filterMessage) {
            case "default":
                defaultOption = true
                pivotalOption = false
                break
            case "pivotal":
                defaultOption = false
                pivotalOption = true
                break
            default:
                defaultOption = false
                pivotalOption = false
        }
        [default:defaultOption, pivotal:pivotalOption]
    }

    private static configureFileFilter(){
        def fileType = "." + properties.(ConstantData.PROP_FILE_TYPE_FILTER).toLowerCase()
        if (fileType.length() == 1) fileType = ""
        [fileType: fileType, filter:!fileType.empty]
    }

    private static createFolders() {
        Util.createFolder(ConstantData.GITHUB_SEARCH_RESULT_FOLDER)
        Util.createFolder(ConstantData.FILTERED_RESULT_FOLDER)
        Util.createFolder(ConstantData.TASKS_FOLDER)
        Util.createFolder(ConstantData.MERGES_FOLDER)
        Util.createFolder(ConstantData.ZIPPED_FILES_FOLDER)
        Util.createFolder(ConstantData.UNZIPPED_FILES_FOLDER)
        Util.createFolder(ConstantData.REPOSITORY_FOLDER)
        Util.createFolder(ConstantData.OUTPUT_FOLDER)
    }

    private static configGithubLogin() throws Exception {
        def token = properties.(ConstantData.PROP_GITHUB_TOKEN)
        if(SIMPLE_GITHUB_SEARCH && (!token || token?.empty)) {
            throw new Exception("Please, inform a valid GitHub token!")
        }
        token
    }

    private static configBigQuery()throws Exception {
        def projectID = properties.(ConstantData.PROP_BIGQUERY_ID)
        if(!SIMPLE_GITHUB_SEARCH && (!projectID || projectID?.empty)){
            throw new Exception("Please, inform a valid GitHub username and password!")
        }
       projectID
    }

    private static configureStarsFilter(){
        def value = properties.(ConstantData.PROP_STARS_FILTER)
        if (!value || value.empty) {
            value = ''
        } else {
            def userValue = configureMandatoryProperties(properties.(ConstantData.PROP_STARS_FILTER), ConstantData.DEFAULT_STARS)
            value = '<=' + userValue
        }
        value
    }

    private static configureYearFilter(){
        def value = configureMandatoryProperties(properties.(ConstantData.PROP_YEAR_FILTER), ConstantData.DEFAULT_YEAR)
        '>=' + value + '-01-01'
    }

}
