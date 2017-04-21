package taskSearch.merge

import taskSearch.id.Commit
import taskSearch.Task

class MergeTask extends Task {

    MergeScenario mergeScenario

    MergeTask(String index, String url, String id, List<Commit> commits, MergeScenario m) {
        super(index, url, id, commits)
        mergeScenario = m
    }

}
