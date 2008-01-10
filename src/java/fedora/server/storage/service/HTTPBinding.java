/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */

package fedora.server.storage.service;

/**
 * A data structure for holding a WSDL HTTP Binding for a set of abstract
 * operations.
 * 
 * @author Sandy Payette
 */
public class HTTPBinding
        extends Binding {

    public String bindingVerb;

    public HTTPOperation[] operations;
}
