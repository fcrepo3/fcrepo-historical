/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */

package fedora.common.policy;

import com.sun.xacml.attr.StringAttribute;

public class SubjectNamespace extends XacmlNamespace {
	
	public final XacmlName LOGIN_ID;
	public final XacmlName USER_REPRESENTED;	
	
    private SubjectNamespace(XacmlNamespace parent, String localName) {
    	super(parent, localName);
    	LOGIN_ID = addName(new XacmlName(this, "loginId", StringAttribute.identifier)); 
    	USER_REPRESENTED = addName(new XacmlName(this, "subjectRepresented", StringAttribute.identifier));    	
    }

	public static SubjectNamespace onlyInstance = new SubjectNamespace(Release2_1Namespace.getInstance(), "subject");
	
	public static final SubjectNamespace getInstance() {
		return onlyInstance;
	}


}
