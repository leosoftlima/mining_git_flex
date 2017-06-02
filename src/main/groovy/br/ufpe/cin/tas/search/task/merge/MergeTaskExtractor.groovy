package br.ufpe.cin.tas.search.task.merge

import groovy.util.logging.Slf4j
import br.ufpe.cin.tas.search.task.GitRepository
import br.ufpe.cin.tas.util.ConstantData
import br.ufpe.cin.tas.util.CsvUtil
import br.ufpe.cin.tas.util.Util

@Slf4j
class MergeTaskExtractor {

    String mergesCsv
    GitRepository repository
    List<MergeScenario> mergeScenarios
    String tasksCsv
    private static int taskId = 0

    MergeTaskExtractor(String mergeFile){
        mergesCsv = mergeFile
        mergeScenarios = extractMergeScenarios()
        def url = mergeScenarios.first().url
        repository = GitRepository.getRepository(url)
        tasksCsv = "${ConstantData.TASKS_FOLDER}${repository.name}.csv"
    }

    private configureMergeTask(MergeScenario merge){
        def commits1 = repository?.searchCommits(merge.leftCommits)
        log.info "commits1.size: ${commits1.size()}"
        def commits2 = repository?.searchCommits(merge.rightCommits)
        log.info "commits2.size: ${commits2.size()}"
        if(!commits1.empty && !commits2.empty){
            def leftTask = new MergeTask(repository.url, ++taskId as String, commits1, merge)
            def rightTask = new MergeTask(repository.url, ++taskId as String, commits2, merge)
            return [leftTask, rightTask]
        } else return []
    }

    private List<MergeScenario> extractMergeScenarios(){
        def merges = []
        def url = ""
        List<String[]> entries = CsvUtil.read(mergesCsv)
        if (entries.size() > 2){
            url = entries.first()[0]
            entries.removeAt(0)
            entries.removeAt(0)
            entries?.each{ entry ->
                def v1, v2
                if(entry[4].size()>2) v1 = entry[4].substring(1, entry[4].size()-1).tokenize(', ')
                else v1 = []
                if(entry[5].size()>2) v2 = entry[5].substring(1, entry[5].size()-1).tokenize(', ')
                else v2 = []
                merges += new MergeScenario(url:url, merge:entry[0], left:entry[1], right:entry[2], base:entry[3],
                        leftCommits: v1 as List<String>, rightCommits: v2 as List<String>)
            }
        }
        merges
    }

    def extractTasks(){
        def tasks = []
        mergeScenarios?.each{ tasks += configureMergeTask(it) }
        log.info "Found merge tasks: ${tasks.size()}"
        tasks.each{ log.info it.toString() }
        Util.exportProjectTasks(tasks, tasksCsv, repository.url)
    }

}
