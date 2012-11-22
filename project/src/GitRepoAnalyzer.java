import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class GitRepoAnalyzer {
	
	private Commit base;
	private List<Commit> commits;
	
	
	
	public GitRepoAnalyzer(File input){		
		
	}
	
	
	private void readInput(){
		
	}
	public void downloadCommits(){
		
	}
	
	public void unzipCommits(){
		
	}
	
	public void loadDirectories(){
		
		
	}
	
	public void computeChangeSets(){
		
		
	}
	
	public void computeAllIntersections(){
		
	}
	
	private void computeIntersectionBetweenTwoFeatures(){
		
	}
	public String toString(){
		String result = "Base\n";
		
		for(Commit commit : this.commits){
			result = commit.getName() + "\n";
		}
		
		return result;
		
	}
	
public static void main(String[] args) {
	BufferedReader buffer = new BufferedReader(new InputStreamReader(System.in));
	System.out.println("Enter input file name:");
    try {
		
		File input = new File (buffer.readLine());
		GitRepoAnalyzer analyzer = new GitRepoAnalyzer(input);
		
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}
} 
