package search;

import au.com.bytecode.opencsv.CSVWriter;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.bigquery.Bigquery;
import com.google.api.services.bigquery.BigqueryScopes;
import com.google.api.services.bigquery.model.*;
import util.Util;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class BigQueryServiceManager {

    /**
     * Creates an authorized Bigquery client service using Application Default Credentials.
     *
     * @return an authorized Bigquery client
     * @throws IOException if there's an error getting the default credentials.
     */
    public static Bigquery createAuthorizedClient() throws IOException {
        // Create the credential
        HttpTransport transport = new NetHttpTransport();
        JsonFactory jsonFactory = new JacksonFactory();
        GoogleCredential credential = GoogleCredential.getApplicationDefault(transport, jsonFactory);

        // Depending on the environment that provides the default credentials (e.g. Compute Engine, App
        // Engine), the credentials may require us to specify the scopes we need explicitly.
        // Check for this case, and inject the Bigquery scope if required.
        if (credential.createScopedRequired()) {
            credential = credential.createScoped(BigqueryScopes.all());
        }

        return new Bigquery.Builder(transport, jsonFactory, credential)
                .setApplicationName("Bigquery Samples").build();
    }

    /**
     * Executes the given query synchronously.
     *
     * @param querySql the query to execute.
     * @param bigquery the Bigquery service object.
     * @param projectId the id of the project under which to run the query.
     * @return a list of the results of the query.
     * @throws IOException if there's an error communicating with the API.
     */
    private static List<TableRow> executeQuery(String querySql, Bigquery bigquery, String projectId)
            throws IOException {
        QueryResponse query = bigquery.jobs().query(
                projectId,
                new QueryRequest().setQuery(querySql))
                .execute();

        // Execute it
        GetQueryResultsResponse queryResult = bigquery.jobs().getQueryResults(
                query.getJobReference().getProjectId(),
                query.getJobReference().getJobId()).execute();

        return queryResult.getRows();
    }


    /**
     * Saves the results in a csv file.
     *
     * @param rows the rows to save. Each row contains 7 columns: "repository_url", "repository_master_branch",
     *             "payload_commit_msg", "commit_link", "payload_commit_id", "created_at" e "repository_watchers"
     */
    private static void exportResults(List<TableRow> rows) throws IOException {
        CSVWriter writer = new CSVWriter(new FileWriter(Util.PROJECTS_FILE));
        writer.writeNext(new String[]{"repository_url", "repository_master_branch", "payload_commit_msg", "commit_link",
                "payload_commit_id", "created_at", "repository_watchers"});

        for (TableRow row : rows) {
            List<TableCell> fields = row.getF();
            String[] entry = new String[fields.size()]; //url, branch, msg, commit link, commit id, date, watchers
            for(int i=0; i<entry.length; i++) {
                entry[i] = fields.get(i).getV().toString();
            }
            writer.writeNext(entry);
        }
        writer.close();
    }

    /**
     *
     * Searches for GitHub projects according to the
     *
     * @param projectId the id of the project to run the search under. If no valid value is given, it will prompt for it.
     * @param query the query to guide the searching.
     * @throws IOException if there's an error communicating with the API.
     */
    public static void searchProjects(String projectId, String query) throws IOException {
        Scanner sc;
        if (projectId==null || projectId.isEmpty()) {
            // Prompt the user to enter the id of the project to run the queries under
            System.out.print("Enter the project ID: ");
            sc = new Scanner(System.in);
        } else {
            sc = new Scanner(projectId);
        }
        projectId = sc.nextLine();

        // Create a new Bigquery client authorized via Application Default Credentials.
        Bigquery bigquery = createAuthorizedClient();

        // Configure the query
        if(query==null || query.isEmpty()) {
            query = "SELECT repository_url, repository_master_branch, payload_commit_msg, "
                    + "(repository_url + '/commit/' + payload_commit_id) AS commit_link, payload_commit_id, created_at, repository_watchers "
                    + "FROM [githubarchive:github.timeline] "
                    + "WHERE PARSE_UTC_USEC(repository_created_at) <= PARSE_UTC_USEC ('2015-08-31 23:59:59') "
                    + "AND PARSE_UTC_USEC(repository_created_at) >= PARSE_UTC_USEC ('2010-08-31 23:59:59') "
                    + "AND type = 'PushEvent' "
                    + "AND repository_language = 'Groovy' "
                    + "AND ( (LOWER(payload_commit_msg) LIKE '[#%]%') "
                    + "OR (LOWER(payload_commit_msg) LIKE '[fix% #%]%') "
                    + "OR (LOWER(payload_commit_msg) LIKE '[complet% #%]%') "
                    + "OR (LOWER(payload_commit_msg) LIKE '[finish% #%]%') ) "
                    + "GROUP BY repository_url, repository_master_branch, payload_commit_msg, commit_link, payload_commit_id, created_at, repository_watchers "
                    + "ORDER BY repository_url";
        }

        // Run query
        List<TableRow> rows = executeQuery(query, bigquery, projectId);
        System.out.println("Result size: "+rows.size());

        // Save result
        exportResults(rows);
        System.out.printf("The search result is saved at %s%n", Util.PROJECTS_FILE);
    }

}
