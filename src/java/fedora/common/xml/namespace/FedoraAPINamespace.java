/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */

package fedora.common.xml.namespace;

/**
 * The Fedora API XML namespace.
 *
 * <pre>
 * Namespace URI    : http://www.fedora.info/definitions/1/0/api/
 * Preferred Prefix : api
 * </pre>
 *
 * @author cwilper@cs.cornell.edu
 */
public class FedoraAPINamespace
        extends XMLNamespace {

    //---
    // Singleton instantiation
    //---

    /** The only instance of this class. */
    private static final FedoraAPINamespace ONLY_INSTANCE
            = new FedoraAPINamespace();

    /**
     * Constructs the instance.
     */
    private FedoraAPINamespace() {
        super("http://www.fedora.info/definitions/1/0/api/", "api");
    }

    /**
     * Gets the only instance of this class.
     *
     * @return the instance.
     */
    public static FedoraAPINamespace getInstance() {
        return ONLY_INSTANCE;
    }

}