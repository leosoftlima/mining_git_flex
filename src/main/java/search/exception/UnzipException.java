package search.exception;


public class UnzipException extends Exception {

    public UnzipException(String message){
        super("Problem during unzipping: "+message);
    }

}
