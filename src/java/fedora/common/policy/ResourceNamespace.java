/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */

package fedora.common.policy;

import com.sun.xacml.attr.DateTimeAttribute;

public class ResourceNamespace extends XacmlNamespace {
	
	// Properties
	public final XacmlName AS_OF_DATETIME;	
	public final XacmlName TICKET_ISSUED_DATETIME;	

    private ResourceNamespace(XacmlNamespace parent, String localName) {
    	super(parent, localName);
    	AS_OF_DATETIME = new XacmlName(this, "asOfDateTime", DateTimeAttribute.identifier); 
    	TICKET_ISSUED_DATETIME = addName(new XacmlName(this, "ticketIssuedDateTime", DateTimeAttribute.identifier));
    	
    }

	public static ResourceNamespace onlyInstance = new ResourceNamespace(Release2_1Namespace.getInstance(), "resource");
	static {
		onlyInstance.addNamespace(ObjectNamespace.getInstance()); 
		onlyInstance.addNamespace(DatastreamNamespace.getInstance()); 
		onlyInstance.addNamespace(DisseminatorNamespace.getInstance()); 
		onlyInstance.addNamespace(BDefNamespace.getInstance()); 
		onlyInstance.addNamespace(BMechNamespace.getInstance()); 
		
		
	}
	
	public static final ResourceNamespace getInstance() {
		return onlyInstance;
	}

}
