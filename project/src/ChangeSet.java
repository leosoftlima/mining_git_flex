import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/*changeset is the delta between the base commit and the feature commit, i.e. base + feature A
 * it records the names of the files that were added, deleted and modified.
 * Each changeset instance contains
 * */
public class ChangeSet {
	
	private HashMap<String, String> removedFiles;
	
	private List<String> addedFiles;
	
	private List<String> modifiedFiles;
	
	private List<String> addedDirectories;
	
	private List<String> removedDirectories;
	
	private List<ChangeSet> subChangeSets;
	
	private FeatureCommit feature;
	
	private String directoryName;
	



	private FileHandler fileHandler = new FileHandler();
	
	public ChangeSet(){
		this.removedFiles = new HashMap<>();
		this.addedFiles = new ArrayList<String>();
		this.modifiedFiles = new ArrayList<String>();
		this.addedDirectories = new ArrayList<String>();
		this.removedDirectories = new ArrayList<String>();
		this.subChangeSets = new ArrayList<ChangeSet>();
		
	}
	
public void loadChangeSet(Directory featureDir,Directory baseDir) throws IOException{
	
			String equalpath = "[s][o][u][r][c][e][c][o][d][e]";

			this.baseCase(featureDir, baseDir, equalpath);
			this.recursion(featureDir, baseDir, equalpath);
			
			
	
}
	
private void baseCase (Directory featureDir,Directory baseDir, String equalpath) throws IOException{
	
	
	//set directory name
	if(featureDir.getName().equals(this.feature.getName())){
		
		this.setDirectoryName(featureDir.getName());
		
		
	}else if(featureDir.getName().endsWith("sourcecode")){
		this.setDirectoryName("sourcecode");
	}else{
		this.setDirectoryName(featureDir.getName().split(equalpath)[1]);
		
	}
	//set directory name
	
	for( String featureFile : featureDir.getFilesNames()){
		
		
		
		boolean containsFeatureFile = false;
		
		for(String baseFile : baseDir.getFilesNames()){

			String shortFeatureFilename, shortBaseFilename, shortfFilename;
			shortFeatureFilename = featureFile.split(equalpath)[1];
			shortBaseFilename = baseFile.split(equalpath)[1];
			
			
			
			if(shortFeatureFilename.equals(shortBaseFilename)){
				containsFeatureFile = true;
				boolean equals = this.fileHandler.compareTwoFiles(baseFile, featureFile);
				if(!equals){
					//modified
					this.modifiedFiles.add(shortFeatureFilename);
				}
			}
			
			//removed file
			boolean removed = true;
			
			
			for(String fFile : featureDir.getFilesNames()){
				
				
				shortfFilename = fFile.split(equalpath)[1];
				if(shortBaseFilename.equals(shortfFilename)){
					removed = false;
					
				}
			}
			if(removed){
				
				this.removedFiles.put(shortBaseFilename, shortBaseFilename);
			}
			//end of removed file
			
		}
		
		if(!containsFeatureFile){
			//added
			this.addedFiles.add(featureFile.split(equalpath)[1]);
		}
		
		
	}
}

private void recursion(Directory featureDir,Directory baseDir, String equalpath) throws IOException{
	//recursion
	
	for(Directory subDirectory : featureDir.getSubDirectories()){
		String shortSubDirectoryName;
		
		try {
			shortSubDirectoryName = subDirectory.getName().split(equalpath)[1];
		} catch (Exception e) {
			shortSubDirectoryName = "";
		}
		
		boolean contains = false;
		
		for(Directory subBaseDirectory : baseDir.getSubDirectories() ){
			String shortSubBaseDirectoryName;
			
			try {
				shortSubBaseDirectoryName = subBaseDirectory.getName().split(equalpath)[1];
			} catch (Exception e) {
				shortSubBaseDirectoryName = "";
			}
			
			if(shortSubDirectoryName.equals(shortSubBaseDirectoryName)){
				contains = true;
				ChangeSet sub = new ChangeSet();
				sub.setFeature(this.feature);
				sub.loadChangeSet(subDirectory, subBaseDirectory);
				this.subChangeSets.add(sub);
			}
			
			//remove directory
			boolean removed = true;
			
			for(Directory fDirectory : featureDir.getSubDirectories()){
				String shortFDirectoryName;
				
				try {
					shortFDirectoryName = fDirectory.getName().split(equalpath)[1];
				} catch (Exception e) {
					shortFDirectoryName = "";
				}
				
				if(shortSubBaseDirectoryName.equals(shortFDirectoryName)){
					removed = false;
				}
				
			}
			
			if(removed){
				this.removedDirectories.add(subBaseDirectory.getName());
			}
			//end of removed directory

			
		}
		
		//added
		if(!contains){
			this.addedDirectories.add(subDirectory.getName());
		}
	}

	
}


	public String toString(){
		String result = "Directory name: " + this.directoryName + "\n";
		
		
		for(String addedFile : this.addedFiles){
			result = result + "Added: " + addedFile + "\n";
		}
		
		
		for (String modifiedFile : this.modifiedFiles){
			result = result + "Modified: " + modifiedFile + "\n";
		}
		
	
		for (String removedFile : this.removedFiles.values()){
			result = result + "Removed: " + removedFile + "\n";
		}
		
		
		for(String addedDirectory : this.addedDirectories){
			result = result + "Added: "+ addedDirectory + "\n";
		}
		
		
		for(String removedDirectory: this.removedDirectories){
			result = result + "Removed: " + removedDirectory + "\n";
		}
		
		for(ChangeSet change : this.subChangeSets){
			result = result + change.toString() + "\n";
		}
		return result;
	}
	
	public List<String> getRemovedFiles() {
		return new ArrayList<String>(removedFiles.values());
	}

	public void setRemovedFiles(HashMap<String, String> removedFiles) {
		this.removedFiles = removedFiles;
	}

	public List<String> getAddedFiles() {
		return addedFiles;
	}

	public void setAddedFiles(List<String> addedFiles) {
		this.addedFiles = addedFiles;
	}

	public List<String> getModifiedFiles() {
		return modifiedFiles;
	}

	public void setModifiedFiles(List<String> modifiedFiles) {
		this.modifiedFiles = modifiedFiles;
	}
	
	
	public List<ChangeSet> getSubChangeSets() {
		return subChangeSets;
	}

	public void setSubChangeSets(List<ChangeSet> subChangeSets) {
		this.subChangeSets = subChangeSets;
	}
	

	public FeatureCommit getFeature() {
		return feature;
	}

	public void setFeature(FeatureCommit feature) {
		this.feature = feature;
	}

	public String getDirectoryName() {
		return directoryName;
	}

/*	public void setDirectoryName(Directory featureDir, String equalpath) {
		
		//checks if there is at least one file in the directory
		if(featureDir.getFilesNames().size() >= 1){
			String file = featureDir.getFilesNames().get(0);
			File f = new File(file);
			this.directoryName = f.getParent().split(equalpath)[1];
			
		}else{
			this.directoryName = "no file";
		}
		
	}*/
	
	public void setDirectoryName(String directoryName){
		this.directoryName = directoryName;
	}

	public static void main(String[] args) {
		
		File f = new File("C:/Users/Paola2/Desktop/teste.txt");
	    System.out.println(f.getParent());  
		
	}
}
