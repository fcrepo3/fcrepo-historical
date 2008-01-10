/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */

package fedora.server.journal;

/**
 * An Exception type for use by the Journaller and its associated classes.
 * 
 * @author Jim Blake
 */
public class JournalException
        extends Exception {

    private static final long serialVersionUID = 1L;

    public JournalException() {
        super();
    }

    public JournalException(String message) {
        super(message);
    }

    public JournalException(String message, Throwable cause) {
        super(message, cause);
    }

    public JournalException(Throwable cause) {
        super(cause);
    }

}
