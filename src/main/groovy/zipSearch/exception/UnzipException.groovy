package zipSearch.exception


class UnzipException extends Exception {

    UnzipException(String message){
        super("Problem during unzipping: "+message)
    }

}
