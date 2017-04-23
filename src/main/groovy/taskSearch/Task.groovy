package taskSearch

import taskSearch.id.Commit
import util.Util

class Task {

    String repositoryIndex
    String repositoryUrl
    String id
    List<Commit> commits
    List<String> productionFiles
    List<String> testFiles

    Task() {

    }

    Task(String index, String url, String id) {
        repositoryIndex = index
        repositoryUrl = url
        this.id = id
        commits = []
        productionFiles = []
        testFiles = []
    }

    Task(String index, String url, String id, List<Commit> commits) {
        this(index, url, id)
        this.commits = commits
        commits*.files?.flatten()?.unique()?.each { file ->
            if (Util.isTestFile(file)) testFiles += file
            else productionFiles += file
        }
    }

    @Override
    String toString() {
        def msg = "Task ${id}:\n"
        msg += "Commits: ${commits.size()}\n"
        msg += "Production files: ${productionFiles.size()}\n"
        productionFiles.each{ msg+= "${it}\n" }
        msg += "Test files: ${testFiles.size()}\n"
        testFiles.each{ msg+= "${it}\n" }
        return msg
    }

}
