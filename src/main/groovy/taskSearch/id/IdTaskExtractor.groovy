package taskSearch.id

import groovy.util.logging.Slf4j
import taskSearch.GitRepository
import taskSearch.Task
import util.DataProperties
import util.RegexUtil

@Slf4j
class IdTaskExtractor {

    GitRepository repository
    String repositoryIndex
    List<Commit> commits

    IdTaskExtractor(String index, String url){
        repositoryIndex = index
        repository = GitRepository.getRepository(url)
        if (DataProperties.FILTER_BY_PIVOTAL_TRACKER)
            commits = repository.searchByComment(RegexUtil.PIVOTAL_TRACKER_ID_REGEX)
        else if (DataProperties.FILTER_BY_DEFAULT_MESSAGE)
            commits = repository.searchByComment(RegexUtil.GENERAL_ID_REGEX)
    }

    def findLinkTaskChangeset() {
        List<Task> tasks = []
        def organizedCommits = []

        commits?.each { commit ->
            def idsFromCommit = commit.message.findAll(/#\d+/).unique().collect { it - "#" }
            organizedCommits += [commit: commit, code: idsFromCommit]
        }

        def ids = (organizedCommits*.code)?.unique()?.flatten()
        ids.each { id ->
            def commitsWithId = organizedCommits.findAll { id in it.code }
            Task task = new Task(repositoryIndex, repository.url, id, commitsWithId*.commit)
            tasks += task
        }

        log.info "Total tasks: ${tasks.size()}"

        [tasks:tasks, repository:repository.name]
    }

}
