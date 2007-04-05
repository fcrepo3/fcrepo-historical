/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */

package fedora.server.errors.authorization;

import fedora.server.errors.ServerException;

/**
 *
 * <p><b>Title:</b> AuthzException.java</p>
 * <p><b>Description:</b> Thrown when functional processing should discontinue.</p>
 *
 * @author wdn5e@virginia.edu
 * @version $Id$
 */
public abstract class AuthzException
        extends ServerException {
	
	public static final String BRIEF_DESC = "Used for authorization signaling";

    public AuthzException(String message) {
        super(null, message, null, null, null);
    }

    public AuthzException(String bundleName, String code,
            String[] replacements, String[] details, Throwable cause) {
        super(bundleName, code, replacements, details, cause);
    }

}