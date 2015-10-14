package search.exception;


public class DownloadException extends Exception {

    public DownloadException(String message){
        super("Problem during download: "+message);
    }

}
