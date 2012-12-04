
public class BaseCommit {
	
	private String name;
	
	private String SHAKey;
	

	private Directory directory;
	
	
	
	public BaseCommit(String name, String sha){
	
		this.name = name;
		this.SHAKey = sha;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSHAKey() {
		return SHAKey;
	}

	public void setSHAKey(String sHAKey) {
		SHAKey = sHAKey;
	}

	public Directory getDirectory() {
		return directory;
	}

	public void setDirectory() {
		this.directory.listFilesAndFilesSubDirectories(this.name);
	}


	public void loadDirectory(){
		
	}
	



}
