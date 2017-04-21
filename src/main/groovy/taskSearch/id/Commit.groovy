package taskSearch.id

class Commit {

    String hash
    String message
    List<String> files
    String author
    long date

    String toString() {
        "$hash*${new Date(date * 1000)}*$author*$message*${files.toListString()}"
    }

}
