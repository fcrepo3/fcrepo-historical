
package fedora.server.errors.authorization;

import fedora.server.errors.ServerException;

/**
 * Thrown when authorization is denied.
 * 
 * @author Bill Niebel
 */
public class PasswordComparisonException
        extends ServerException {

    private static final long serialVersionUID = 1L;

    public static final String BRIEF_DESC = "Authorization Permitted";

    public PasswordComparisonException(String message) {
        super(null, message, null, null, null);
    }

    public PasswordComparisonException(String message, Throwable cause) {
        super(null, message, null, null, cause);
    }

    public PasswordComparisonException(String bundleName,
                                       String code,
                                       String[] replacements,
                                       String[] details,
                                       Throwable cause) {
        super(bundleName, code, replacements, details, cause);
    }

}
