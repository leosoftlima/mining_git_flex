package taskSearch

import taskSearch.mergeScenario.MergeScenario

class MergeTask extends Task {

    MergeScenario mergeScenario

    MergeTask() {
        super ()
    }

    MergeTask(String index, String url, String id, MergeScenario m) {
        super(index, url, id)
        mergeScenario = m
    }

    MergeTask(String index, String url, String id, List<Commit> commits, MergeScenario m) {
        super(index, url, id, commits)
        mergeScenario = m
    }

}
