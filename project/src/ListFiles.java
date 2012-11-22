import java.io.File;

public class ListFiles {
	
	//this method gets all files names inside a directory and is sub directories
	public void listFilesAndFilesSubDirectories(String directoryName){
		
		File directory = new File (directoryName);
		
		File[] fList = directory.listFiles();
		
		for(File file : fList){
			
			if(file.isFile()){
				
			} else if (file.isDirectory()){
				listFilesAndFilesSubDirectories(file.getAbsolutePath());
			}
			
		}
		
		
	}

}
