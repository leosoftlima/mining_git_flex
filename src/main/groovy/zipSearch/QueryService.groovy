package zipSearch

import au.com.bytecode.opencsv.CSVWriter
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.bigquery.Bigquery;
import com.google.api.services.bigquery.BigqueryScopes
import com.google.api.services.bigquery.model.*
import util.DataProperties

public class QueryService {

    /**
     * Executes the given query synchronously.
     *
     * @param querySql the query to execute.
     * @param bigquery the Bigquery service object.
     * @param projectId the id of the repository under which to run the query.
     * @return a list of the results of the query.
     * @throws IOException if there's an error communicating with the API.
     */
    private static List<TableRow> executeQuery(String querySql, Bigquery bigquery, String projectId)
            throws IOException {
        QueryResponse query = bigquery.jobs().query(
                projectId,
                new QueryRequest().setQuery(querySql))
                .execute()

        // Execute it
        GetQueryResultsResponse queryResult = bigquery.jobs().getQueryResults(
                query.getJobReference().getProjectId(),
                query.getJobReference().getJobId()).execute()

        return queryResult.getRows()
    }


    /**
     * Saves the results in a csv file.
     *
     * @param rows the rows to save. Each row contains 7 columns: "repository_url", "repository_master_branch",
     *             "payload_commit_msg", "commit_link", "payload_commit_id", "created_at" e "repository_watchers"
     */
    private static exportResults(List<TableRow> rows) throws IOException {
        CSVWriter writer = new CSVWriter(new FileWriter(DataProperties.BIGQUERY_COMMITS_FILE));
        String[] param = ["repository_url", "repository_master_branch", "payload_commit_msg", "commit_link",
            "payload_commit_id", "created_at", "repository_watchers"]
        writer.writeNext(param)

        if(rows != null) {
            for (TableRow row : rows) {
                List<TableCell> fields = row.getF()
                String[] entry = new String[fields.size()] //url, branch, msg, commit link, commit id, date, watchers
                for (int i = 0; i < entry.length; i++) {
                    entry[i] = fields.get(i).getV().toString()
                }
                writer.writeNext(entry)
            }
        }
        else{
            println "No repository was found!"
        }

        writer.close()
    }

    /**
     *
     * Searches for GitHub projects according to the query.
     *
     * @param projectId the id of the repository to run the search under. If no valid value is given, it will prompt for it.
     * @param query the query to guide the searching.
     * @throws IOException if there's an error communicating with the API.
     */
    static searchProjects(String projectId, String query) throws IOException {
        Scanner sc;
        if (projectId==null || projectId.isEmpty()) {
            // Prompt the user to enter the id of the repository to run the queries under
            print "Enter the repository ID: "
            sc = new Scanner(System.in)
        } else {
            sc = new Scanner(projectId)
        }
        projectId = sc.nextLine()

        // Create a new Bigquery client authorized via Application Default Credentials.
        Bigquery bigquery = createAuthorizedClient()

        // Configure the query
        if(query!=null && !query.isEmpty()) {
            // Run query
            List<TableRow> rows = executeQuery(query, bigquery, projectId)
            exportResults(rows) // Save result
        }
    }

    /**
     * Creates an authorized Bigquery client service using Application Default Credentials.
     *
     * @return an authorized Bigquery client
     * @throws IOException if there's an error getting the default credentials.
     */
    static Bigquery createAuthorizedClient() throws IOException {
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

        return new Bigquery.Builder(transport, jsonFactory, credential)
                .setApplicationName("Bigquery Samples").build()
    }

}
