package util

import groovy.util.logging.Slf4j
import java.util.regex.Matcher

@Slf4j
class DataProperties {

    public static final Properties properties
    public static final String BIGQUERY_PROJECT_ID
    public static final TEST_CODE_REGEX
    public static final String LANGUAGE
    public static final String GITHUB_LOGIN
    public static final String GITHUB_PASSWORD
    public static final boolean SIMPLE_GITHUB_SEARCH

    public static final String REPOSITORY_FOLDER

    public static final String FILE_TYPE
    public static final boolean FILTER_BY_DEFAULT_MESSAGE
    public static final boolean FILTER_BY_PIVOTAL_TRACKER
    public static final boolean FILTER_BY_FILE
    public static final boolean FILTER_RAILS
    public static final List<String> GEMS
    public static final int FILTER_STARS
    public static final String GEMS_PATH
    public static final String GEM_REQUIRE_ALL

    static {
        try {
            properties = new Properties()
            loadProperties()

            REPOSITORY_FOLDER = configureRepositoryFolderPath()
            createFolders()

            TEST_CODE_REGEX = configTestCodeRegex()

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

            GEMS_PATH = (properties.(ConstantData.PROP_GEMS)).replace(File.separator, Matcher.quoteReplacement(File.separator))
            GEM_REQUIRE_ALL = configureGemRequireall()

            FILTER_STARS = configureStarsFilter()

        } catch (Exception ex) {
            log.info ex.message
            ex.stackTrace.each{ log.info it.toString() }
        }

        log.info "FILTER_BY_FILE: ${FILTER_BY_FILE}; FILE_TYPE: ${FILE_TYPE}"
        log.info "FILTER_RAILS: ${FILTER_RAILS}"
        log.info "FILTER_BY_DEFAULT_MESSAGE: ${FILTER_BY_DEFAULT_MESSAGE}"
        log.info "FILTER_BY_PIVOTAL_TRACKER: ${FILTER_BY_PIVOTAL_TRACKER}"
        log.info "FILTER_STARS: ${FILTER_STARS}"
    }

    private static loadProperties() {
        File configFile = new File(ConstantData.PROPERTIES_FILE_NAME)
        FileInputStream resourceStream = new FileInputStream(configFile)
        properties.load(resourceStream)
    }

    private static configureMandatoryProperties(value, int defaultValue) {
        if (!value || value.empty) value = defaultValue
        value
    }

    private static configureMandatoryProperties(value, defaultValue) {
        if (!value || value.empty) value = defaultValue
        value.replaceAll(RegexUtil.FILE_SEPARATOR_REGEX, Matcher.quoteReplacement(File.separator))
    }

    private static configureRepositoryFolderPath() {
        def value = configureMandatoryProperties(properties.(ConstantData.PROP_REPOSITORY), ConstantData.DEFAULT_REPOSITORY_FOLDER)
        if (!value.endsWith(File.separator)) value += File.separator
        value
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
        Util.createFolder(REPOSITORY_FOLDER)
    }

    private static configTestCodeRegex() {
        def testPath = properties.(ConstantData.PROP_TEST_PATH)
                .split(",")*.replaceAll(" ", "")

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
        configureMandatoryProperties(properties.(ConstantData.PROP_STARS_FILTER), ConstantData.DEFAULT_STARS) as int
    }

    private static configureGem(value, defaultValue){
        def folder = configureMandatoryProperties(value, defaultValue)
        GEMS_PATH + Matcher.quoteReplacement(File.separator) + folder + ConstantData.GEM_SUFFIX
    }

    private static configureGemRequireall(){
        configureGem(properties.(ConstantData.PROP_GEM_REQUIREALL), ConstantData.DEFAULT_GEM_REQUIREALL_FOLDER)
    }

}
