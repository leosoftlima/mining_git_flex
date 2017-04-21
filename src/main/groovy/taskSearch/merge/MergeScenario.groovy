package taskSearch.merge

class MergeScenario {

    String url
    String merge
    String left
    String right
    String base
    List<String> leftCommits
    List<String> rightCommits

    @Override
    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false
        MergeScenario that = (MergeScenario) o
        if (merge != that.merge) return false
        return true
    }

    @Override
    int hashCode() {
        return merge.hashCode()
    }

    @Override
    String toString() {
        "merge: ${merge}; base: ${base}; left: ${left}; right: ${right}; #left:${leftCommits.size()}; #right:${rightCommits.size()}"
    }
}
