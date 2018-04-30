package br.ufpe.cin.tas.search.task.merge

import br.ufpe.cin.tas.search.task.id.Commit
import br.ufpe.cin.tas.search.task.Task

class MergeTask extends Task {

    MergeScenario mergeScenario
    boolean conflict
    List<String> conflictingFiles

    MergeTask(String url, String id, List<Commit> commits, MergeScenario m, String last) {
        super(url, id, commits, last)
        mergeScenario = m
        conflict = false
        conflictingFiles = []
    }

    MergeTask(String url, String id, List<Commit> commits, MergeScenario m, String last, List<String> conflictingFiles) {
        super(url, id, commits, last)
        mergeScenario = m
        this.conflictingFiles = conflictingFiles
        this.conflict = !conflictingFiles.empty
    }

}
