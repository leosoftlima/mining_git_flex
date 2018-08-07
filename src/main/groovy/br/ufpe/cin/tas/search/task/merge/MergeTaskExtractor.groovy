package br.ufpe.cin.tas.search.task.merge

import br.ufpe.cin.tas.search.task.id.Commit
import br.ufpe.cin.tas.util.DataProperties
import groovy.util.logging.Slf4j
import br.ufpe.cin.tas.search.task.GitRepository
import br.ufpe.cin.tas.util.ConstantData
import br.ufpe.cin.tas.util.CsvUtil
import br.ufpe.cin.tas.util.Util

@Slf4j
class MergeTaskExtractor {

    String mergesCsv
    String fastforwardMergesCsv
    GitRepository repository
    List<MergeScenario> mergeScenarios
    String tasksCsv
    String cucumberConflictingTasksCsv
    private static int taskId
    List<String> fastForwardMerges

    MergeTaskExtractor(String mergeFile) throws Exception {
        taskId = 0
        mergesCsv = mergeFile
        fastforwardMergesCsv = mergeFile - ConstantData.MERGE_TASK_SUFIX + ConstantData.FASTFORWARD_MERGE_TASK_SUFIX
        this.fastForwardMerges = CsvUtil.read(fastforwardMergesCsv).collect{ it[0] }
        mergeScenarios = extractMergeScenarios()
        if(mergeScenarios.empty){
            throw new Exception("No merge commit was found!")
        }
        def url = mergeScenarios.first().url
        repository = GitRepository.getRepository(url)
        tasksCsv = "${ConstantData.TASKS_FOLDER}${repository.name}.csv"
        cucumberConflictingTasksCsv = "${ConstantData.TASKS_FOLDER}${repository.name}-conflict.csv"
    }

    private static printMergeInfo(MergeScenario merge){
        log.info "Extracting merge scenario"
        log.info "merge commit: ${merge.merge}"
        log.info "base commit: ${merge.base}"
        log.info "left commit: ${merge.left}"
        log.info "right commit: ${merge.right}\n"
    }

    private extractLastLeftTask(List<Commit> mergeCommits1, List<Commit> commits1, MergeScenario merge){
        def leftTask = null
        def intermediateMerge = mergeCommits1.get(0)
        def index = commits1.indexOf(intermediateMerge)
        def commitsOfInterest = commits1.subList(0, index)
        if(commitsOfInterest.empty){ //só há intermediateMerge
            log.info "commitsOfInterest é vazia"
            def found = fastForwardMerges.find{ it == intermediateMerge.hash }
            if(found){
                log.info "intermediateMerge é fast-forward merge: ${intermediateMerge.hash}"
                leftTask = new MergeTask(repository.url, ++taskId as String, [intermediateMerge], merge.merge, merge.base, merge.left)
            } else log.info "intermediateMerge não é fast-forward merge: ${intermediateMerge.hash}"
        } else if(!(commitsOfInterest.size()==1 && commitsOfInterest.get(0).isMerge) ) {
            leftTask = new MergeTask(repository.url, ++taskId as String, commitsOfInterest, merge.merge, merge.base, merge.left)
        }
        leftTask
    }

    private extractLastRightTask(List<Commit> mergeCommits2, List<Commit> commits2, MergeScenario merge){
        def rightTask = null
        def intermediateMerge = mergeCommits2.get(0)
        def index = commits2.indexOf(intermediateMerge)
        def commitsOfInterest = commits2.subList(0, index)
        if(commitsOfInterest.empty){ //só há intermediateMerge
            log.info "commitsOfInterest é vazia"
            def found = fastForwardMerges.find{ it == intermediateMerge.hash }
            if(found){
                log.info "intermediateMerge é fast-forward merge: ${intermediateMerge.hash}"
                rightTask = new MergeTask(repository.url, ++taskId as String, [intermediateMerge], merge.merge, merge.base, merge.right)
            } else log.info "intermediateMerge não é fast-forward merge: ${intermediateMerge.hash}"
        } else if(!(commitsOfInterest.size()==1 && commitsOfInterest.get(0).isMerge) ) {
            rightTask = new MergeTask(repository.url, ++taskId as String, commitsOfInterest,  merge.merge, merge.base, merge.right)
        }
        rightTask
    }

    private extractLeftTasks(List<Commit> mergeCommits1, List<Commit> commits1, MergeScenario merge){
        log.info "Ramo left possui merges intermediários: ${mergeCommits1.size()}"
        mergeCommits1.each{ log.info "${it.hash} (${new Date(it.date * 1000)})" }

        log.info "Ramo left possui total de commits: ${commits1.size()}"
        commits1.each{ log.info "${it.hash} (${new Date(it.date * 1000)})" }

        List<MergeTask> result = []
        if(mergeCommits1.size()==1){ //há apenas 1 merge
            log.info "há apenas 1 merge: ${mergeCommits1.get(0).hash}"
            def leftTask = extractLastLeftTask(mergeCommits1, commits1, merge)
            if(leftTask) result += leftTask
        } else{ // há múltiplos merges
            /* ramo entre merge commit mais atual da tarefa e left */
            def leftTask = extractLastLeftTask(mergeCommits1, commits1, merge)
            if(leftTask) result += leftTask

            /* ramo entre merges */
            def pairs = mergeCommits1.collate(2, 1, false)
            pairs.each{ pair ->
                log.info "par de merges: ${pair*.hash}"
                def index1 = commits1.indexOf(pair.get(0))
                def index2 = commits1.indexOf(pair.get(1))
                def commitsOfInterest = commits1.subList(index1+1, index2)
                if(!commitsOfInterest.empty && !(commitsOfInterest.size()==1 && commitsOfInterest.get(0).isMerge) ) {
                    leftTask = new MergeTask(repository.url, ++taskId as String, commitsOfInterest, pair.get(0).hash,
                            pair.get(1).hash, commitsOfInterest.first().hash)
                    result += leftTask
                }
            }
        }
        log.info "Tarefas do ramo left: ${result.size()}"
        result.each{ log.info it.toString() }
        result
    }

    private extractRightTasks(List<Commit> mergeCommits2, List<Commit> commits2, MergeScenario merge){
        log.info "Ramo right possui merges intermediários: ${mergeCommits2.size()}"
        mergeCommits2.each{ log.info "${it.hash} (${new Date(it.date * 1000)})" }

        log.info "Ramo right possui total de commits: ${commits2.size()}"
        commits2.each{ log.info "${it.hash} (${new Date(it.date * 1000)})" }

        List<MergeTask> result = []
        if(mergeCommits2.size()==1){ //há apenas 1 merge
            log.info "há apenas 1 merge: ${mergeCommits2.get(0).hash}"
            def leftTask = extractLastRightTask(mergeCommits2, commits2, merge)
            if(leftTask) result += leftTask
        } else{ // há múltiplos merges
            /* ramo entre merge commit mais atual da tarefa e right */
            def rightTask = extractLastRightTask(mergeCommits2, commits2, merge)
            if(rightTask) result += rightTask

            /* ramo entre merges */
            def pairs = mergeCommits2.collate(2, 1, false)
            pairs.each{ pair ->
                log.info "par de merges: ${pair*.hash}"
                def index1 = commits2.indexOf(pair.get(0))
                def index2 = commits2.indexOf(pair.get(1))
                def commitsOfInterest = commits2.subList(index1+1, index2)
                if(!commitsOfInterest.empty && !(commitsOfInterest.size()==1 && commitsOfInterest.get(0).isMerge) ) {
                    rightTask = new MergeTask(repository.url, ++taskId as String, commitsOfInterest, pair.get(0).hash,
                            pair.get(1).hash, commitsOfInterest.first().hash)
                    result += rightTask
                }
            }
        }
        log.info "Tarefas do ramo right: ${result.size()}"
        result.each{ log.info it.toString() }
        result
    }

    private configureMergeTask(MergeScenario merge){
        printMergeInfo(merge)

        List<MergeTask> result = []
        def commits1 = repository?.searchCommits(merge.leftCommits.reverse())
        def mergeCommits1 = commits1.findAll{ it.isMerge }
        def commits2 = repository?.searchCommits(merge.rightCommits.reverse())
        def mergeCommits2 = commits2.findAll{ it.isMerge }

        // Left
        log.info "Extração de left tasks a partir do merge: ${merge.merge}"
        if(!commits1.empty && mergeCommits1.empty){ //não possui merges intermediários
            def leftTask = new MergeTask(repository.url, ++taskId as String, commits1, merge, merge.left)
            result += leftTask
            log.info "Ramo left não possui merges intermediários"
            log.info leftTask.toString()
        } else if(!commits1.empty && !mergeCommits1.empty){ //possui merges intermediários
            result += extractLeftTasks(mergeCommits1, commits1, merge)
        }

        // Right
        log.info "Extração de right tasks a partir do merge: ${merge.merge}"
        if(!commits2.empty && mergeCommits2.empty){ //não possui merges intermediários
            def rightTask = new MergeTask(repository.url, ++taskId as String, commits2, merge, merge.right)
            result += rightTask
            log.info "Ramo right não possui merges intermediários"
            log.info rightTask.toString()
        } else if(!commits2.empty && !mergeCommits2.empty){ //possui merges intermediários
            result += extractRightTasks(mergeCommits2, commits2, merge)
        }

        log.info "Total final de tarefas: ${result.size()}"

        result
    }

    private configureMergeTaskWithConflictInfo(MergeScenario merge){
        printMergeInfo(merge)
        def result = []
        def commits1 = repository?.searchCommits(merge.leftCommits)
        def commits2 = repository?.searchCommits(merge.rightCommits)
        if(!commits1.empty && !commits2.empty){
            List conflictingFiles = repository.extractConflictingFiles(merge)
            if(conflictingFiles!=null){
                def leftTask = new MergeTask(repository.url, ++taskId as String, commits1, merge, merge.left, conflictingFiles)
                def rightTask = new MergeTask(repository.url, ++taskId as String, commits2, merge, merge.right, conflictingFiles)
                result = [leftTask, rightTask]
            }
        }
        result
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
        List<MergeTask> tasks = []
        mergeScenarios?.each{ tasks += configureMergeTask(it) }
        def tasksPT = tasks.findAll { !it.productionFiles.empty && !it.testFiles.empty }
        log.info "Found merge tasks: ${tasks.size()}"
        log.info "Found P&T tasks: ${tasksPT.size()}"

        def taskGroups = tasksPT.groupBy { it.newestCommit }
        log.info "SHAs: ${taskGroups.size()}"
        taskGroups.eachWithIndex{ group, index ->
            def sha = group.key as String
            def gems = extractGemsInfo(sha)
            log.info "${index} Extracted gems for commit '${sha}'"
            group.getValue().each{ task -> task.gems = gems }
        }
        Util.exportProjectTasks(tasksPT, tasksCsv, repository.url)
        if(DataProperties.CONFLICT_ANALYSIS){
            def cucumberTasks = tasksPT.findAll{ it.hasTests() }
            Util.exportTasksWithConflictInfo(cucumberTasks, cucumberConflictingTasksCsv)
        }
    }
    
    def extractGemsInfo(String sha){
        repository.clean()
        repository.reset(sha)
        repository.checkout(sha)
        def gems = Util.checkRailsVersionAndGems(repository.getLocalPath())
        repository.reset()
        repository.checkout()
        gems
    }

}
