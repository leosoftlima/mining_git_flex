package repositorySearch

import au.com.bytecode.opencsv.CSVWriter
import groovy.util.logging.Slf4j
import org.eclipse.egit.github.core.Repository
import org.eclipse.egit.github.core.SearchRepository
import org.eclipse.egit.github.core.client.GitHubClient
import org.eclipse.egit.github.core.service.RepositoryService
import util.ConstantData
import util.DataProperties

@Slf4j
class GithubAPIQueryService implements QueryService {

    GitHubClient client
    RepositoryService repositoryService
    Map query
    def pagesLimit
    FileWriter file

    GithubAPIQueryService(){
        file = new FileWriter(ConstantData.REPOSITORIES_TO_DOWNLOAD_FILE)
        pagesLimit = 10
        client = new GitHubClient()
        client.setCredentials(DataProperties.GITHUB_LOGIN, DataProperties.GITHUB_PASSWORD)
        repositoryService = new RepositoryService(client)
        query = [language:'ruby', created:DataProperties.FILTER_YEAR, stars:DataProperties.FILTER_STARS, sort:'stars']
    }

    private exportGitHubSearchResult(List<SearchRepository> repositories) throws IOException {
        CSVWriter writer = new CSVWriter(file)
        String[] header = ["URL", "MASTER_BRANCH", "CREATED_AT", "STARS", "SIZE", "DESCRIPTION"]
        writer.writeNext(header)
        repositories?.each { //in fact, watchers are stars
            Repository repo = repositoryService.getRepository(it)
            String[] entry = [it.url, repo.masterBranch, it.createdAt, it.watchers, it.size, it.description]
            writer.writeNext(entry)
        }
        writer.close()
    }

    @Override
    def searchProjects() throws IOException {
        List<SearchRepository> repositories = []
        (1..pagesLimit).each {
            def rep = repositoryService.searchRepositories(query, it)
            if(rep) repositories += rep
        }
        log.info "GitHub API found ${repositories.size()} repositories based on search criteria."
        exportGitHubSearchResult(repositories.unique())
        log.info "Repositories found by GitHub API are saved in ${ConstantData.REPOSITORIES_TO_DOWNLOAD_FILE}"
    }

}
