package br.ufpe.cin.tas.search.task

import br.ufpe.cin.tas.search.task.id.Commit
import br.ufpe.cin.tas.util.Util

class Task {

    String repositoryUrl
    String id
    List<Commit> commits
    List<String> productionFiles
    List<String> testFiles
    List<String> changedFiles
    List changedFilesWithLines //[file:"", lines:[]]
    String newestCommit

    Task() {

    }

    Task(String url, String id) {
        repositoryUrl = url
        this.id = id
        commits = []
        productionFiles = []
        testFiles = []
        changedFiles = []
        changedFilesWithLines = []
    }

    Task(String url, String id, List<Commit> commits, String newestCommit) {
        this(url, id)
        this.commits = commits
        this.newestCommit = newestCommit
        organizeFiles()
    }

    private organizeFiles(){
        changedFiles = commits*.files?.flatten()?.unique()
        changedFiles?.each { file ->
            if (Util.isTestFile(file)) testFiles += file
            else if(Util.isProductionFile(file)) productionFiles += file
        }
    }

    @Override
    String toString() {
        def msg = "Task ${id}:\n"
        msg += "Commits: ${commits.size()}\n"
        commits.each{ msg+= "${it.hash}\n" }
        msg += "Newest commit: $newestCommit\n"
        msg += "Production files: ${productionFiles.size()}\n"
        msg += "Test files: ${testFiles.size()}\n"
        return msg
    }

    Commit getNewestCommitObject(){
        commits.find{ it.hash == newestCommit }
    }

}
