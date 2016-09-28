package zipSearch.exception;


class DownloadException extends Exception {

    DownloadException(String message){
        super("Problem during download: "+message)
    }

}
