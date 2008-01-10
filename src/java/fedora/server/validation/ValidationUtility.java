/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */

package fedora.server.validation;

import java.net.MalformedURLException;
import java.net.URL;

import fedora.server.errors.ValidationException;

/**
 * Misc validation-related functions.
 * 
 * @author Chris Wilper
 */
public abstract class ValidationUtility {

    public static void validateURL(String url, boolean canBeRelative)
            throws ValidationException {
        try {
            URL goodURL = new URL(url);
        } catch (MalformedURLException murle) {
            if (url.startsWith("copy://") || url.startsWith("uploaded://")
                    || url.startsWith("temp://")) {
                return;
            }
            throw new ValidationException("Malformed URL: " + url, murle);
        }
    }

}
