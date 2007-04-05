/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */

package fedora.server.security;

public class DefaultRoleConfig extends AbstractRoleConfig {

    public static final String ROLE = "default";

    public DefaultRoleConfig() {
        super(null);
    }

    public String getRole() {
        return ROLE;
    }

}