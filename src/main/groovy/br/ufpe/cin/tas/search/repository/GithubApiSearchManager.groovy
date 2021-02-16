package br.ufpe.cin.tas.search.repository

import groovy.util.logging.Slf4j
import org.eclipse.egit.github.core.Repository
import org.eclipse.egit.github.core.SearchRepository
import org.eclipse.egit.github.core.client.GitHubClient
import org.eclipse.egit.github.core.service.RepositoryService
import br.ufpe.cin.tas.util.ConstantData
import br.ufpe.cin.tas.util.CsvUtil
import br.ufpe.cin.tas.util.DataProperties

@Slf4j
class GithubApiSearchManager implements RepositorySearchManager {

    GitHubClient client
    RepositoryService repositoryService
    Map query
    int pagesLimit

    GithubApiSearchManager(){
        pagesLimit = 10
        client = new GitHubClient()
        client.setOAuth2Token(DataProperties.GITHUB_TOKEN)
        repositoryService = new RepositoryService(client)
        if(DataProperties.FILTER_BY_LAST_UPDATE){
            query = [language:DataProperties.LANGUAGE, created:DataProperties.FILTER_YEAR, sort:'updated']
        } else {
            query = [language:DataProperties.LANGUAGE, created:DataProperties.FILTER_YEAR, stars:DataProperties.FILTER_STARS, sort:'stars']
        }
    }

    private exportGitHubSearchResult(List<SearchRepository> repositories) throws IOException {
        List<String[]> content = []
        String[] header = ["URL", "MASTER_BRANCH", "CREATED_AT", "STARS", "SIZE", "DESCRIPTION"]
        content += header
        repositories?.each { //in fact, watchers are stars
            Repository repo = repositoryService.getRepository(it)
            String[] entry = [it.url, repo.masterBranch, it.createdAt, it.watchers, it.size, it.description]
            content += entry
        }
        CsvUtil.write(ConstantData.REPOSITORIES_TO_DOWNLOAD_FILE, content)
    }

    @Override
    def search() throws IOException {
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
