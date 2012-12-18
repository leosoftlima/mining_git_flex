import java.io.IOException;


public class FeatureCommit extends BaseCommit {
	
	private ChangeSet changeset;
	
public FeatureCommit(String name, String sha){
		
		super(name, sha);
		this.changeset = new ChangeSet();
		
	}


public void  computeChangeSet(BaseCommit base) throws IOException{
	
	this.changeset.loadChangeSet(this.getDirectory(), base.getDirectory());
		
}


public ChangeSet getChangeset() {
	return changeset;
}


public void setChangeset(ChangeSet changeset) {
	this.changeset = changeset;
}



}
