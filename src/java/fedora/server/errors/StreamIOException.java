/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */

package fedora.server.errors;

/**
 *
 * <p><b>Title:</b> StreamIOException.java</p>
 * <p><b>Description:</b> Superclass for low-level stream i/o problems.</p>
 *
 * @author cwilper@cs.cornell.edu
 * @version $Id$
 */
public class StreamIOException
        extends ServerException {

	private static final long serialVersionUID = 1L;
	
    /**
     * Creates a StreamIOException.
     *
     * @param message An informative message explaining what happened and
     *                (possibly) how to fix it.
     */
    public StreamIOException(String message) {
        super(null, message, null, null, null);
    }


    public StreamIOException(String message, Throwable cause) {
        super(null, message, null, null, cause);
    }

    public StreamIOException(String bundleName, String code, String[] values,
            String[] details, Throwable cause) {
        super(bundleName, code, values, details, cause);
    }

}
