package fedora.server.errors;

/**
 * Abstract superclass for storage-related exceptions.
 *
 * @author cwilper@cs.cornell.edu
 */
public abstract class StorageException 
        extends Exception {

    /**
     * Creates a StorageException.
     *
     * @param message An informative message explaining what happened and
     *                (possibly) how to fix it.
     */
    public StorageException(String message) {
        super(message);
    }

}