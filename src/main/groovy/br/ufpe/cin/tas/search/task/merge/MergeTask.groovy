package br.ufpe.cin.tas.search.task.merge

import br.ufpe.cin.tas.search.task.id.Commit
import br.ufpe.cin.tas.search.task.Task

class MergeTask extends Task {

    String base
    String merge
    boolean conflict
    List<String> conflictingFiles

    MergeTask(String url, String id, List<Commit> commits, String merge, String base, String last) {
        super(url, id, commits, last)
        this.base = base
        this.merge = merge
        conflictingFiles = []
    }

    MergeTask(String url, String id, List<Commit> commits, MergeScenario m, String last) {
        super(url, id, commits, last)
        this.base = m.base
        this.merge = m.merge
        conflictingFiles = []
    }

    MergeTask(String url, String id, List<Commit> commits, MergeScenario m, String last, List<String> conflictingFiles) {
        super(url, id, commits, last)
        this.base = m.base
        this.merge = m.merge
        this.conflictingFiles = conflictingFiles
        this.conflict = !conflictingFiles.empty
    }

    @Override
    String toString() {
        def msg = super.toString()
        msg += "Merge commit: $merge\n"
        msg += "Base commit: ${base}\n"
        return msg
    }

}
