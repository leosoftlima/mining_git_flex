package repositorySearch

import au.com.bytecode.opencsv.CSVWriter
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.bigquery.Bigquery
import com.google.api.services.bigquery.BigqueryScopes
import com.google.api.services.bigquery.model.*
import groovy.time.TimeCategory
import groovy.util.logging.Slf4j
import util.ConstantData
import util.DataProperties

@Slf4j
class GoogleArchiveQueryService implements QueryService {

    Bigquery bigquery
    String query

    GoogleArchiveQueryService(){
        // Create a new Bigquery client authorized via Application Default Credentials.
        this.bigquery = createAuthorizedClient()
        this.query = configureQuery()
    }

    GoogleArchiveQueryService(String query){
        // Create a new Bigquery client authorized via Application Default Credentials.
        this.bigquery = createAuthorizedClient()
        this.query = query
    }

    /**
     * Creates an authorized Bigquery client repositoryService using Application Default Credentials.
     *
     * @return an authorized Bigquery client
     * @throws IOException if there's an error getting the default credentials.
     */
    private static Bigquery createAuthorizedClient() throws IOException {
        // Create the credential
        HttpTransport transport = new NetHttpTransport()
        JsonFactory jsonFactory = new JacksonFactory()
        GoogleCredential credential = GoogleCredential.getApplicationDefault(transport, jsonFactory)

        // Depending on the environment that provides the default credentials (e.g. Compute Engine, App
        // Engine), the credentials may require us to specify the scopes we need explicitly.
        // Check for this case, and inject the Bigquery scope if required.
        if (credential.createScopedRequired()) {
            credential = credential.createScoped(BigqueryScopes.all())
        }

        new Bigquery.Builder(transport, jsonFactory, credential).setApplicationName("Bigquery Samples").build()
    }

    private static String filterPivotalTracker() {
        """AND ( (LOWER(payload_commit_msg) LIKE '[#%]%')
        OR (LOWER(payload_commit_msg) LIKE '[fix% #%]%')
        OR (LOWER(payload_commit_msg) LIKE '[complet% #%]%')
        OR (LOWER(payload_commit_msg) LIKE '[finish% #%]%') ) """
    }

    private static String filterDefault() {
        "AND (REGEXP_MATCH(LOWER(payload_commit_msg), r'.*#[\\d]+.*')) "
    }

    private static String configureQuery() {
        def endDate = new Date()
        def initialDate
        use(TimeCategory) {
            initialDate = endDate - 5.years
        }

        String query = """SELECT repository_url, repository_master_branch, payload_commit_msg,
        (repository_url + '/commit/' + payload_commit_id) AS commit_link, payload_commit_id, created_at, repository_watchers
        FROM [bigquery-public-data:samples.github_timeline]
        WHERE PARSE_UTC_USEC(repository_created_at) <= PARSE_UTC_USEC ('""" + new java.sql.Date(endDate.getTime()) + """')
        AND PARSE_UTC_USEC(repository_created_at) >= PARSE_UTC_USEC ('""" + new java.sql.Date(initialDate.getTime()) + """')
        AND type = 'PushEvent'
        AND repository_language = '""" + DataProperties.LANGUAGE + "' "
        if (DataProperties.FILTER_BY_DEFAULT_MESSAGE) query += filterDefault()
        if (DataProperties.FILTER_BY_PIVOTAL_TRACKER) query += filterPivotalTracker()
        query += """ GROUP BY repository_url, repository_master_branch, payload_commit_msg, commit_link,
        payload_commit_id, created_at, repository_watchers ORDER BY repository_url LIMIT 5"""

        query
    }

    /**
     * Executes the given query synchronously.
     *
     * @param querySql the query to execute.
     * @param bigquery the Bigquery repositoryService object.
     * @param projectId the id of the repository under which to run the query.
     * @return a list of the results of the query.
     * @throws IOException if there's an error communicating with the API.
     */
    private List<TableRow> executeQuery(String projectId)
            throws IOException {
        QueryResponse query = bigquery.jobs().query(
                projectId,
                new QueryRequest().setQuery(query))
                .execute()

        GetQueryResultsResponse queryResult = bigquery.jobs().getQueryResults(
                query.getJobReference().getProjectId(),
                query.getJobReference().getJobId()).execute()

        queryResult.getRows()
    }

    /**
     * Saves the results in a csv file.
     *
     * @param rows the rows to save. Each row contains 7 columns: "repository_url", "repository_master_branch",
     *             "payload_commit_msg", "commit_link", "payload_commit_id", "created_at" e "repository_watchers"
     */
    private static saveQueryResult(List<TableRow> rows) throws IOException {
        def file = new File(ConstantData.BIGQUERY_COMMITS_FILE)
        CSVWriter writer = new CSVWriter(new FileWriter(file))
        String[] param = ["URL", "MASTER_BRANCH", "COMMIT_MSG", "COMMIT_LINK", "COMMIT_ID", "CREATED_AT", "WATCHERS"]
        writer.writeNext(param)

        if (rows != null) {
            for (TableRow row : rows) {
                List<TableCell> fields = row.getF()
                String[] entry = new String[fields.size()] //url, branch, msg, commit link, commit id, date, watchers
                for (int i = 0; i < entry.length; i++) {
                    entry[i] = fields.get(i).getV().toString()
                }
                writer.writeNext(entry)
            }
        } else {
            log.info "No repository was found!"
        }

        writer.close()
    }

    /**
     * Searches for GitHub projects according to the query.
     *
     * @throws IOException if there's an error communicating with the API.
     */
    @Override
    def searchProjects() throws IOException {
        String projectId = DataProperties.BIGQUERY_PROJECT_ID
        Scanner sc
        if (projectId == null || projectId.empty) { // Prompt the user to enter the id of the repository
            print "Enter the repository ID: "
            sc = new Scanner(System.in)
        } else {
            sc = new Scanner(projectId)
        }
        projectId = sc.nextLine()

        // Run query
        if (query != null && !query.empty) {
            List<TableRow> rows = executeQuery(projectId)
            saveQueryResult(rows)
            SearchResultManager.generateRepositoriesCsv()
            log.info "The repositories to search for are saved in ${ConstantData.REPOSITORIES_TO_DOWNLOAD_FILE}"
        }
    }

}
