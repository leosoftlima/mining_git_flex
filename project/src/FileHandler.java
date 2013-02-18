import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;

import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import au.com.bytecode.opencsv.CSVReader;


/*the input must contain the following informations:
 * the repository user's id
 * the repository name
 * baseCommit, key
 * baseName, key
 * an so on
 * 
 * */

public class FileHandler {

	
	
	 GitMiner reader(String fileName)throws IOException{
		
		GitMiner result = new GitMiner(fileName);
		CSVReader reader = new CSVReader(new FileReader(fileName));
		List<String[]> entries = reader.readAll();
		
		//set GitMiner userId attribute
		String[] userLine = entries.get(0);
		String userId  = userLine[0];
		result.setUserId(userId);
		
		//set GitMiner repo attribute
		String[] repoLine = entries.get(1);
		String repo  = repoLine[0];
		result.setRepoName(repo);
		
		//set GitMiner base commit
		String[] baseLine = entries.get(2);
		String name = baseLine[0];
		String sha = baseLine[1];
		result.setBase(new BaseCommit(name, sha));
		
		//set GitMiner features commits
		List<FeatureCommit> features = new ArrayList<FeatureCommit>();
		int featuresIndex = 3;
		while(featuresIndex < entries.size()){
			
			String featureLine[] = entries.get(featuresIndex);
			name = featureLine[0];
			sha = featureLine[1];
			features.add(new FeatureCommit(name, sha));
			
			featuresIndex++;
			
		}
		result.setFeatures(features);
		
		return result;
		
	}
	
	public void downloader(GitMiner repo) throws MalformedURLException, IOException{
		
		String gitUrl = "https://github.com/" + repo.getUserId() + "/" + 
		repo.getRepoName() + "/archive/";
		
		//download base commit
		String url = gitUrl  + repo.getBase().getSHAKey() + ".zip";
		
		this.auxiliarDownload(url, repo.getBase().getName());
		System.out.println("end of " + repo.getBase().getName() + " download.");
		//download feature commits
		
		for(FeatureCommit f : repo.getFeatures()){
			String fUrl = gitUrl + f.getSHAKey() + ".zip";
			this.auxiliarDownload(fUrl, f.getName());
			System.out.println("end of " + f.getName() + " download.");
		}

		System.out.println("done downloading");
		
	}
	
	private void auxiliarDownload(String url, String name) throws MalformedURLException, IOException{
		
		BufferedInputStream in = new java.io.BufferedInputStream(new java.net.URL(url).openStream());
		FileOutputStream fos = new java.io.FileOutputStream(name + ".zip");
		BufferedOutputStream bout = new BufferedOutputStream(fos,8192);
		byte[] data = new byte[8192];
		int x=0;
		while((x=in.read(data,0,8192))>=0)
		{
		bout.write(data,0,x);
		}
		bout.close();
		in.close();
		}
	
	public void unzipper(GitMiner repo) throws IOException{
		
		
		
		//unzip base
		String zipFile = repo.getBase().getName() + ".zip";
		String outputFolder =  repo.getBase().getName();
		this.auxiliarUnzip(zipFile, outputFolder);
		System.out.println("done unzipping base commit.");
		
		//renames the root directory from rgms-shaKey to "soucecode"
		File oldName = new File (repo.getBase().getName() + File.separator + "rgms-" + repo.getBase().getSHAKey() );
		File newName = new File (repo.getBase().getName() + File.separator +"sourcecode");
		oldName.renameTo(newName);
		
		//unzip other features
		for(FeatureCommit f : repo.getFeatures()){
			
			String fZipFile =  f.getName() + ".zip";
			String fOutputFolder =  f.getName();
			this.auxiliarUnzip(fZipFile, fOutputFolder);
			System.out.println("done unzipping feature " + f.getName() + " commit.");
			
			//renames the root directory from rgms-shaKey to "soucecode"
			oldName = new File (f.getName() + File.separator + "rgms-" + f.getSHAKey());
			newName = new File (f.getName() +  File.separator + "sourcecode");
			oldName.renameTo(newName);
		}
		
		
	}
	
	private void auxiliarUnzip(String zipFile, String outputFolder) throws IOException{
	 
			byte[] buffer = new byte[1024];

	    	//create output directory is not exists
	    	File folder = new File(outputFolder);
	    	if(!folder.exists()){
	    		folder.mkdir();
	    	
	    	//get the zip file content
	    	ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
	    	//get the zipped file list entry
	    	ZipEntry ze = zis.getNextEntry();
	       
	    	while(ze!=null){
	    		
	 
	    	   String fileName = ze.getName();
	           File newFile = new File(outputFolder + File.separator + fileName);
	 
	           System.out.println("file unzip : "+ newFile.getAbsoluteFile());
	 
	            //create all non exists folders
	            //else you will hit FileNotFoundException for compressed folder
	 
	           if(ze.isDirectory()) 
	           {
	        	   new File(newFile.getParent()).mkdirs();
	           }
	           else
	           {
	        	FileOutputStream fos = null;
	 
	            new File(newFile.getParent()).mkdirs();
	 
	            fos = new FileOutputStream(newFile);             
	 
	            int len;
	            while ((len = zis.read(buffer)) > 0) 
	            {
	       		fos.write(buffer, 0, len);
	            }
	 
	            fos.close();   
	           }
	 
	           ze = zis.getNextEntry();
	    	}
	 
	        zis.closeEntry();
	    	zis.close();
	 
	    	System.out.println("Done unzipping");
	    	
	 
	    }
	   }    
	
	
	public void writer(String fileName, String fileContent){
		
		try {
			 BufferedWriter out = new BufferedWriter(new FileWriter(fileName + ".csv"));
			 out.write(fileContent);
			 out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	       
	}
	
	public boolean compareTwoFiles(String baseFile , String featureFile) throws IOException{
		boolean result;

	    String s1 = "";
	    String s2 = "";
	    String y = "", z = "";

	    BufferedReader bfr = new BufferedReader(new FileReader(baseFile));
	    BufferedReader bfr1 = new BufferedReader(new FileReader(featureFile));

	    while ((z = bfr1.readLine()) != null)
	      s2 += z;

	    while ((y = bfr.readLine()) != null)
	      s1 += y;


	    if (s2.equals(s1)) {
	     result = true;
	    } else {

	     result = false;
	    }
		
		return result;
	}

}
