package ch.papers.objectstorage;

/**
 * Created by ale on 06/03/16.
 */
public class UuidObjectStorageException extends Exception{
    public UuidObjectStorageException(String message) {
        super(message);
    }

    public UuidObjectStorageException(Throwable cause) {
        super(cause);
    }
}
