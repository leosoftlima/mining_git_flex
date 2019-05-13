package br.ufpe.cin.tas.search.task.merge

import br.ufpe.cin.tas.util.DataProperties
import groovy.util.logging.Slf4j
import br.ufpe.cin.tas.search.task.GitRepository
import br.ufpe.cin.tas.util.ConstantData
import br.ufpe.cin.tas.util.CsvUtil
import br.ufpe.cin.tas.util.Util

@Slf4j
class MergeScenarioExtractor {
    GitRepository repository
    List<String> urls
    List<String> fastForwardMerges
    List<String> problematicMerges

    MergeScenarioExtractor() {
        urls = []
        fastForwardMerges = []
        problematicMerges = []
    }

    private updateUrls(){
        def aux = CsvUtil.read(ConstantData.CANDIDATE_REPOSITORIES_FILE)
        if(aux && aux.size()>1) {
            aux?.removeAt(0)
            urls = aux?.collect{ it[0] }
        } else urls = []
    }

    private exportResult(List<MergeScenario> merges){
        def csv = ConstantData.MERGES_FOLDER+repository.name.replaceAll("/", "_")+ConstantData.MERGE_TASK_SUFIX
        List<String[]> content = []
        String[] header1 = [repository.url]
        content += header1
        String[] header2 = ["MERGE", "LEFT", "RIGHT", "BASE", "LEFT_COMMITS", "RIGHT_COMMITS"]
        content += header2
        merges?.each{
            String[] line = [it.merge, it.left, it.right, it.base, it.leftCommits, it.rightCommits]
            content += line
        }
        CsvUtil.write(csv, content)
    }

    private exportFastForwardMerges(){
        def csv = ConstantData.MERGES_FOLDER+repository.name.replaceAll("/", "_")+ConstantData.FASTFORWARD_MERGE_TASK_SUFIX
        List<String[]> content = []
        fastForwardMerges.each { content += [it] as String[] }
        CsvUtil.write(csv, content)
    }

    private exportProblematicMerges(){
        def csv = ConstantData.MERGES_FOLDER+repository.name.replaceAll("/", "_")+ConstantData.PROBLEMATIC_MERGE_TASK_SUFIX
        List<String[]> content = []
        problematicMerges.each { content += [it] as String[] }
        CsvUtil.write(csv, content)
    }

    private searchMergeCommits(String url){
        List<MergeScenario> merges = []
        fastForwardMerges = []
        problematicMerges = []

        if(url==null || url.empty) {
            log.warn "It is not possible to extract merge commits for invalid project: url is empty!"
        }
        else{
            try{
                log.info "Extracting merge commit from project '$url'"
                repository = GitRepository.getRepository(url)
                def result = repository.searchMergeCommits()
                result.each{
                    MergeScenario mergeScenario = configureMergeScenario(it.left, it.right)
                    if(mergeScenario){
                        if(mergeScenario.base){
                            mergeScenario.merge = it.merge
                            merges += mergeScenario
                        } else { //If base is null, there is a problem to analyse merge scenario
                            problematicMerges += it.merge
                        }
                    } else { //If it is null, it is fast-forward
                        fastForwardMerges += it.merge
                    }
                }
                exportResult(merges)
                exportFastForwardMerges()
                exportProblematicMerges()
                log.info "All merge commits: ${merges.size()+fastForwardMerges.size()}"
                log.info "Fast-fowarding merges: ${fastForwardMerges.size()}"
                log.info "Selected merges: ${merges.size()}"
            } catch(Exception ex){
                log.error "Error while searching merge commits."
                ex.stackTrace.each{ log.error it.toString() }
            }
        }
    }

    private configureMergeScenario(String left, String right){
        def base = repository.findBase(left, right)
        def leftHash = repository.commits.find{ it.hash.contains(left) }?.hash
        def rightHash = repository.commits.find{ it.hash.contains(right) }?.hash

        if(!base || base.empty ) { //problem to analyse merge scenario
            return new MergeScenario(left: leftHash, right: rightHash, leftCommits: null, rightCommits: null, base: null)
        } else if(base==leftHash || base==rightHash) { //fast-forward
            return null
        }
        def leftCommits = repository.getCommitSetBetween(base, left)
        def rightCommits = repository.getCommitSetBetween(base, right)
        new MergeScenario(left: leftHash, right: rightHash, leftCommits: leftCommits, rightCommits: rightCommits, base: base)
    }

    def generateMergeFiles(){
        updateUrls()
        urls?.each{ searchMergeCommits(it) }
        repository = null
    }

    def getMergeFiles(){
        if(DataProperties.SEARCH_MERGES) generateMergeFiles()
        def mergeFiles = Util.findFilesFromFolder(ConstantData.MERGES_FOLDER)?.findAll{
            it.endsWith(ConstantData.MERGE_TASK_SUFIX) && !it.endsWith(ConstantData.FASTFORWARD_MERGE_TASK_SUFIX) &&
                    !it.endsWith(ConstantData.PROBLEMATIC_MERGE_TASK_SUFIX)
        }
        mergeFiles
    }

}
