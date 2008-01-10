/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */

package fedora.common.policy;

import com.sun.xacml.attr.AnyURIAttribute;
import com.sun.xacml.attr.StringAttribute;

/**
 * The Fedora BMech XACML namespace.
 * 
 * <pre>
 * Namespace URI    : urn:fedora:names:fedora:2.1:resource:bmech
 * </pre>
 */
public class BMechNamespace
        extends XacmlNamespace {

    public final XacmlName PID;

    public final XacmlName NAMESPACE;

    public final XacmlName STATE;

    public final XacmlName LOCATION;

    public final XacmlName NEW_PID;

    public final XacmlName NEW_NAMESPACE;

    private BMechNamespace(XacmlNamespace parent, String localName) {
        super(parent, localName);
        PID = addName(new XacmlName(this, "pid", StringAttribute.identifier));
        NEW_PID =
                addName(new XacmlName(this,
                                      "newPid",
                                      StringAttribute.identifier));
        NAMESPACE =
                addName(new XacmlName(this,
                                      "namespace",
                                      StringAttribute.identifier));
        NEW_NAMESPACE =
                addName(new XacmlName(this,
                                      "newNamespace",
                                      StringAttribute.identifier));
        LOCATION =
                addName(new XacmlName(this,
                                      "location",
                                      AnyURIAttribute.identifier));
        STATE =
                addName(new XacmlName(this, "state", StringAttribute.identifier));
    }

    public static BMechNamespace onlyInstance =
            new BMechNamespace(ResourceNamespace.getInstance(), "bmech");

    public static final BMechNamespace getInstance() {
        return onlyInstance;
    }

}
