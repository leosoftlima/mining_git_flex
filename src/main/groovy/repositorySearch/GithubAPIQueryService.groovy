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
    def pages

    GithubAPIQueryService(){
        def stars = '>=' + DataProperties.FILTER_STARS
        pages = 20
        client = new GitHubClient()
        client.setCredentials(DataProperties.GITHUB_LOGIN, DataProperties.GITHUB_PASSWORD)
        repositoryService = new RepositoryService(client)
        query = [language:'ruby', created:'>=2013-01-01', stars:stars, sort:'stars']
    }

    private exportGitHubSearchResult(List<SearchRepository> candidates) throws IOException {
        def file = new File(ConstantData.REPOSITORIES_TO_DOWNLOAD_FILE)
        CSVWriter writer = new CSVWriter(new FileWriter(file))
        String[] header = ["URL", "MASTER_BRANCH", "CREATED_AT", "STARS", "SIZE", "DESCRIPTION"]
        writer.writeNext(header)

        candidates?.each { //in fact, watchers are stars
            Repository repo = repositoryService.getRepository(it)
            String[] entry = [it.url, repo.masterBranch, it.createdAt, it.watchers, it.size, it.description]
            writer.writeNext(entry)
        }

        writer.close()
    }

    @Override
    def searchProjects(){
        List<SearchRepository> candidates = []
        (1..pages).each {
            candidates += repositoryService.searchRepositories(query, it).sort{ it.watchers }
        }
        log.info "GitHub API found ${candidates.size()} repositories based on search criteria."
        exportGitHubSearchResult(candidates)
        log.info "Repositories found by GitHub API are saved in ${ConstantData.REPOSITORIES_TO_DOWNLOAD_FILE}"
    }

}
