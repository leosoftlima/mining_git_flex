package util

class Util {

    static deleteFolder(String folder) {
        def dir = new File(folder)
        def files = dir.listFiles()
        if (files != null) {
            files.each { f ->
                if (f.isDirectory()) emptyFolder(f.getAbsolutePath())
                else f.delete()
            }
        }
        dir.deleteDir()
    }

    static emptyFolder(String folder) {
        def dir = new File(folder)
        def files = dir.listFiles()
        if (files != null) {
            files.each { f ->
                if (f.isDirectory()) emptyFolder(f.getAbsolutePath())
                else f.delete()
            }
        }
    }

    static String configureGitRepositoryName(String url) {
        String name = url - DataProperties.GITHUB_URL - DataProperties.GIT_EXTENSION
        return name.replaceAll(RegexUtil.FILE_SEPARATOR_REGEX, "_")
    }

    static createFolder(String folder) {
        File zipFolder = new File(folder - File.separator)
        if (!zipFolder.exists()) {
            zipFolder.mkdir()
        }
    }

}
