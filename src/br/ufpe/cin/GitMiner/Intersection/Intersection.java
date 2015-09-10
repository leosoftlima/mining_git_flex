package br.ufpe.cin.GitMiner.Intersection;

import br.ufpe.cin.GitMiner.ComputeChangeSet.ChangeSet;

import java.util.ArrayList;
import java.util.List;


public class Intersection {

    private List<String> filesWithPotentialConflicts;
    private ChangeSet changeSetA;
    private ChangeSet changeSetB;

    public Intersection() {
        this.filesWithPotentialConflicts = new ArrayList<>();
    }

    public void loadIntersection(ChangeSet a, ChangeSet b) {
        this.changeSetA = a;
        this.changeSetB = b;

        //base case
        for (String fileA : a.getModifiedFiles()) {
            boolean found = false;
            int indexB = 0;

            while ((!found) && (indexB < b.getModifiedFiles().size())) {
                if (fileA.equals(b.getModifiedFiles().get(indexB))) {
                    this.filesWithPotentialConflicts.add(fileA);
                    found = true;
                }
                indexB++;
            }
        }

        //recursion
        for (ChangeSet subChangeSetA : a.getSubChangeSets()) {
            int indexSubChangeSet = 0;
            boolean foundSubChangeSet = false;

            while ((!foundSubChangeSet) && (indexSubChangeSet < b.getSubChangeSets().size())) {
                if (subChangeSetA.getDirectoryName().equals(b.getSubChangeSets().get(indexSubChangeSet).getDirectoryName())) {
                    foundSubChangeSet = true;
                    this.loadIntersection(subChangeSetA, b.getSubChangeSets().get(indexSubChangeSet));
                }
                indexSubChangeSet++;
            }
        }
    }

    public String toString() {
        String result = "";
        for (String fileName : this.filesWithPotentialConflicts) {
            result = result + fileName + "\n";
        }
        return result;
    }

    public ChangeSet getChangeSetA() {
        return changeSetA;
    }

    public void setChangeSetA(ChangeSet changeSetA) {
        this.changeSetA = changeSetA;
    }

    public ChangeSet getChangeSetB() {
        return changeSetB;
    }

    public void setChangeSetB(ChangeSet changeSetB) {
        this.changeSetB = changeSetB;
    }

}
