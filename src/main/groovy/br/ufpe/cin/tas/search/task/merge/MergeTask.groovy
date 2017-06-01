package br.ufpe.cin.tas.search.task.merge

import br.ufpe.cin.tas.search.task.id.Commit
import br.ufpe.cin.tas.search.task.Task

class MergeTask extends Task {

    MergeScenario mergeScenario

    MergeTask(String url, String id, List<Commit> commits, MergeScenario m) {
        super(url, id, commits)
        mergeScenario = m
    }

}
