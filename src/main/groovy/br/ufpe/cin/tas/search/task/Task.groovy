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
    List<String> gems

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
        gems = []
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

        GitRepository gitRepository = GitRepository.getRepository(repositoryUrl)
        def firstCommit = commits?.last()
        def lastCommit = commits?.first()
        if(firstCommit && lastCommit){
            changedFilesWithLines += gitRepository.diff(firstCommit.hash, lastCommit.hash)
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
        msg += "Gems: ${gems}\n"
        return msg
    }

    boolean hasCoverageAndTests(){
        def hasGems = false
        if(gems.contains("rails") && gems.contains("cucumber-rails") && (gems.contains("simplecov")
                || gems.contains("coveralls"))) {
            hasGems = true
        }
        if(commits.size()<=500 && hasGems) true
        else false
    }

    boolean usesCucumber(){
        def hasGems = false
        if(gems.contains("cucumber-rails")) {
            hasGems = true
        }
        hasGems
    }

    Commit getNewestCommitObject(){
        commits.find{ it.hash == newestCommit }
    }

}
