package taskSearch.merge

import org.jruby.embed.ScriptingContainer
import util.ConstantData
import util.DataProperties
import util.Util

class MergeScenarioExtractor {
    ScriptingContainer container
    Object receiver

    MergeScenarioExtractor() {
        container = new ScriptingContainer()
        container.loadPaths.add(DataProperties.GEMS_PATH)
        container.loadPaths.add(DataProperties.GEM_REQUIRE_ALL)
        ClassLoader loader = Thread.currentThread().getContextClassLoader()
        InputStream is = loader.getResourceAsStream(ConstantData.MERGES_EXTRACTOR_FILE)
        receiver = container.runScriptlet(is, ConstantData.MERGES_EXTRACTOR_FILE)
    }

    String extract(){
        container.callMethod(receiver, "extract_merges", ConstantData.MERGE_ANALYSIS_TEMP_FILE,
                DataProperties.GITHUB_LOGIN, DataProperties.GITHUB_PASSWORD, ConstantData.MERGES_FOLDER,
                DataProperties.REPOSITORY_FOLDER)
        Util.deleteFile(ConstantData.MERGE_ANALYSIS_TEMP_FILE)
    }

}
