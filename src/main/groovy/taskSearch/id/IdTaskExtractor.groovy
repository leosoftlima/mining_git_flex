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
    String index
    List<Commit> commits
    String tasksCsv

    IdTaskExtractor(String index, String url){
        this.index = index
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
        def result = null
        if(index){
            def tasks = []
            def organizedCommits = organizeCommitsById()
            def ids = (organizedCommits*.code)?.unique()?.flatten()
            ids?.each { id ->
                def commitsWithId = organizedCommits.findAll { id in it.code }
                Task task = new Task(index, repository.url, id, commitsWithId*.commit)
                tasks += task
            }
            def tasksPT = tasks.findAll { !it.productionFiles.empty && !it.testFiles.empty }
            result = Util.exportProjectTasks(tasks, tasksPT, tasksCsv, index, repository.url)
        }
        result
    }

}
