package br.ufpe.cin.tas.search.task

import br.ufpe.cin.tas.search.task.id.Commit
import br.ufpe.cin.tas.util.Util

class Task {

    String repositoryUrl
    String id
    List<Commit> commits
    List<String> productionFiles
    List<String> testFiles
    String newestCommit

    Task() {

    }

    Task(String url, String id) {
        repositoryUrl = url
        this.id = id
        commits = []
        productionFiles = []
        testFiles = []
    }

    Task(String url, String id, List<Commit> commits, String newestCommit) {
        this(url, id)
        this.commits = commits
        this.newestCommit = newestCommit
        organizeFiles()
    }

    private organizeFiles(){
        commits*.files?.flatten()?.unique()?.each { file ->
            if (Util.isTestFile(file)) testFiles += file
            else if(Util.isProductionFile(file)) productionFiles += file
        }
    }

    @Override
    String toString() {
        def msg = "Task ${id}:\n"
        msg += "Commits: ${commits.size()}\n"
        msg += "Newest commit: $newestCommit\n"
        msg += "Production files: ${productionFiles.size()}\n"
        productionFiles.each{ msg+= "${it}\n" }
        msg += "Test files: ${testFiles.size()}\n"
        testFiles.each{ msg+= "${it}\n" }
        return msg
    }

}
