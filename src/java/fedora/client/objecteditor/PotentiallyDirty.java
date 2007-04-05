/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */

package fedora.client.objecteditor;

/**
 * Interface for containers that report on dirtiness of sub-components.
 *
 * @author cwilper@cs.cornell.edu
 * @version $Id$
 */
public interface PotentiallyDirty {

    /**
     * Have my editable components changed since being loaded from the server?
     */
    public boolean isDirty();

}