package fedora.common.policy;

public class ObjectNamespace extends XacmlNamespace {
	
	// Properties
	public final XacmlName PID;
	public final XacmlName NAMESPACE;	
	public final XacmlName STATE;	
	public final XacmlName CONTROL_GROUP;	

    // Values
	
    private ObjectNamespace(XacmlNamespace parent, String localName) {
    	super(parent, localName);

        // Properties
    	PID = addName(new XacmlName(this, "pid")); 
    	NAMESPACE = addName(new XacmlName(this, "namespace")); 
    	STATE = addName(new XacmlName(this, "state")); 
    	this.CONTROL_GROUP = addName(new XacmlName(this, "controlGroup"));    	

    	// Values
    	
    }

	public static ObjectNamespace onlyInstance = new ObjectNamespace(ResourceNamespace.getInstance(), "object");
	
	public static final ObjectNamespace getInstance() {
		return onlyInstance;
	}


}
