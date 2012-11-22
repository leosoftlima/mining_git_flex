
public class Commit {
	
	private String name;
	
	private String SHAKey;
	
	private Directory directory;
	
	private boolean isBaseCommit;
	
	public Commit(){
		
		
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

	public void setDirectory(Directory directory) {
		this.directory = directory;
	}

	public boolean isBaseCommit() {
		return isBaseCommit;
	}

	public void setBaseCommit(boolean isBaseCommit) {
		this.isBaseCommit = isBaseCommit;
	}
	
	

}
