package br.ufpe.cin.tas.util

import groovy.util.logging.Slf4j
import java.util.regex.Matcher

@Slf4j
class DataProperties {

    public static final Properties properties
    public static final String BIGQUERY_PROJECT_ID
    public static final String LANGUAGE
    public static final String GITHUB_LOGIN
    public static final String GITHUB_PASSWORD
    public static final boolean SIMPLE_GITHUB_SEARCH

    public static final String FILE_TYPE
    public static final boolean FILTER_BY_DEFAULT_MESSAGE
    public static final boolean FILTER_BY_PIVOTAL_TRACKER
    public static final boolean FILTER_BY_FILE
    public static final boolean FILTER_RAILS
    public static final List<String> GEMS
    public static final String FILTER_STARS
    public static final String FILTER_YEAR
    public static final boolean FILTER_BY_LAST_UPDATE

    public static final boolean SEARCH_PROJECTS
    public static final boolean FILTER_PROJECTS
    public static final boolean SEARCH_TASKS
    public static final boolean SEARCH_MERGES
    public static final boolean CONFLICT_ANALYSIS

    static {
        try {
            properties = new Properties()
            loadProperties()

            createFolders()

            BIGQUERY_PROJECT_ID = configBigQuery()

            def searchConfig = configureSearchMechanism()
            FILTER_BY_DEFAULT_MESSAGE = searchConfig.default
            FILTER_BY_PIVOTAL_TRACKER = searchConfig.pivotal

            SIMPLE_GITHUB_SEARCH = !FILTER_BY_DEFAULT_MESSAGE && !FILTER_BY_PIVOTAL_TRACKER
            LANGUAGE = configureLanguage()

            def loginData = configGithubLogin()
            GITHUB_LOGIN = loginData.login
            GITHUB_PASSWORD = loginData.password

            def fileFilter = configureFileFilter()
            FILE_TYPE = fileFilter.fileType
            FILTER_BY_FILE = fileFilter.filter

            def result = configureGemsFilter()
            GEMS = result.gems
            FILTER_RAILS = result.filter
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
            CONFLICT_ANALYSIS = configureMandatoryBooleanProperties(properties.(ConstantData.PROP_CONFLICT_ANALYSIS),
                    ConstantData.DEFAULT_CONFLICT_ANALYSIS)

        } catch (Exception ex) {
            log.info ex.message
            ex.stackTrace.each{ log.info it.toString() }
        }

        log.info "SEARCH_PROJECTS: ${SEARCH_PROJECTS}"
        log.info "FILTER_PROJECTS: ${FILTER_PROJECTS}"
        log.info "SEARCH_TASKS: ${SEARCH_TASKS}"
        log.info "FILTER_BY_FILE: ${FILTER_BY_FILE}; FILE_TYPE: ${FILE_TYPE}"
        log.info "FILTER_RAILS: ${FILTER_RAILS}"
        log.info "FILTER_BY_DEFAULT_MESSAGE: ${FILTER_BY_DEFAULT_MESSAGE}"
        log.info "FILTER_BY_PIVOTAL_TRACKER: ${FILTER_BY_PIVOTAL_TRACKER}"
        log.info "FILTER_STARS: ${FILTER_STARS}"
        log.info "FILTER_YEAR: ${FILTER_YEAR}"

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

    private static configureGemsFilter(){
        def gems = properties.(ConstantData.PROP_GEMS_FILTER)
        def gemSet
        def filter
        if(!gems || gems.empty) {
            gemSet = []
            filter = false
        }
        else {
            gemSet = gems?.split(",")*.replaceAll(" ", "")
            filter = gemSet.size() > 0
        }
        [gems:gemSet, filter:filter]
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
        def login = properties.(ConstantData.PROP_GITHUB_LOGIN)
        def password = properties.(ConstantData.PROP_GITHUB_PASSWORD)
        if(SIMPLE_GITHUB_SEARCH && (!login || !password || login?.empty || password?.empty)) {
            throw new Exception("Please, inform a valid GitHub username and password!")
        }
        [login:login, password:password]
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
