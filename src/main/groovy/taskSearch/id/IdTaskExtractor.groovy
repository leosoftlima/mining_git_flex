package taskSearch.id

import groovy.util.logging.Slf4j
import taskSearch.GitRepository
import taskSearch.Task
import util.ConstantData
import util.DataProperties
import util.RegexUtil
import util.Util

@Slf4j
class IdTaskExtractor {

    GitRepository repository
    List<Commit> commits
    String tasksCsv

    IdTaskExtractor(String url){
        repository = GitRepository.getRepository(url)
        if (DataProperties.FILTER_BY_PIVOTAL_TRACKER)
            commits = repository.searchByComment(RegexUtil.PIVOTAL_TRACKER_ID_REGEX)
        else if (DataProperties.FILTER_BY_DEFAULT_MESSAGE)
            commits = repository.searchByComment(RegexUtil.GENERAL_ID_REGEX)
        tasksCsv = "${ConstantData.TASKS_FOLDER}${repository.name}.csv"
    }

    private organizeCommitsById(){
        def organizedCommits = []
        commits?.each { commit ->
            def idsFromCommit = commit.message.findAll(/#\d+/).unique().collect { it - "#" }
            organizedCommits += [commit: commit, code: idsFromCommit]
        }
        organizedCommits
    }

    def extractTasks() {
        def tasks = []
        def organizedCommits = organizeCommitsById()
        def ids = (organizedCommits*.code)?.unique()?.flatten()
        ids?.each { id ->
            def commitsWithId = organizedCommits.findAll { id in it.code }
            Task task = new Task(repository.url, id, commitsWithId*.commit)
            tasks += task
        }
        def tasksPT = tasks.findAll { !it.productionFiles.empty && !it.testFiles.empty }
        Util.exportProjectTasks(tasks, tasksPT, tasksCsv, repository.url)
    }

}
