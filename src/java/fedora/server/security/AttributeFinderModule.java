package fedora.server.security;

import java.util.Hashtable;
import java.util.Iterator;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.ServletContext;
import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.BagAttribute;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.cond.EvaluationResult;
import com.sun.xacml.ctx.Status;

/*package*/ abstract class AttributeFinderModule extends com.sun.xacml.finder.AttributeFinderModule {
	
	private ServletContext servletContext = null;
	
	protected void setServletContext(ServletContext servletContext) {
		if (this.servletContext == null) {
			this.servletContext = servletContext;
		}
	}

	protected AttributeFinderModule() {
		
		URI temp;

		try {
			temp = new URI(StringAttribute.identifier);
		} catch (URISyntaxException e1) {
			temp = null;
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		STRING_ATTRIBUTE_URI = temp;
	}
	
	private Boolean instantiatedOk = null;
	protected final void setInstantiatedOk(boolean value) {
		log("setInstantiatedOk() " + value);
		if (instantiatedOk == null) {
			instantiatedOk = new Boolean(value);
		}
	}
	
	public boolean isDesignatorSupported() {
		log("isDesignatorSupported() will return " + iAm() + " " + ((instantiatedOk != null) && instantiatedOk.booleanValue()));
		return (instantiatedOk != null) && instantiatedOk.booleanValue();
	}

	private final boolean parmsOk(
			URI attributeType,
			URI attributeId,
			int designatorType) {
		log("in parmsOk "  + iAm());
		if (! getSupportedDesignatorTypes().contains(new Integer(designatorType))) {
			log("AttributeFinder:parmsOk" + iAm() + " exit on " + "target not supported");
			return false;
		}

		if (attributeType == null) {
			log("AttributeFinder:parmsOk" + iAm() + " exit on " + "null attributeType");
			return false;
		}

		if (attributeId == null) {
			log("AttributeFinder:parmsOk" + iAm() + " exit on " + "null attributeId");
			return false;		}

		log("AttributeFinder:parmsOk" + iAm() + " looking for " + attributeId.toString());
		showRegisteredAttributes();
		
		if (hasAttribute(attributeId.toString())) {
			if (! (getAttributeType(attributeId.toString()).equals(attributeType.toString()))) {
				log("AttributeFinder:parmsOk" + iAm() + " exit on " + "attributeType incorrect for attributeId");
				return false;
			}
		} else {
			if (! (StringAttribute.identifier).equals(attributeType.toString())) {
				log("AttributeFinder:parmsOk" + iAm() + " exit on " + "attributeType incorrect for attributeId");
				return false;
			}			
		}
		log("exiting parmsOk normally " + iAm());
		return true;
	}
	
	protected String iAm() {
		return this.getClass().getName();
	}
	
	protected final Object getAttributeFromEvaluationCtx(EvaluationResult attribute /*URI type, URI id, URI category, EvaluationCtx context*/) {
		if (attribute.indeterminate()) {
			log("AttributeFinder:getAttributeFromEvaluationCtx" + iAm() + " exit on " + "couldn't get resource attribute from xacml request " + "indeterminate");
			return null;			
		}

		if ((attribute.getStatus() != null) && ! Status.STATUS_OK.equals(attribute.getStatus())) { 
			log("AttributeFinder:getAttributeFromEvaluationCtx" + iAm() + " exit on " + "couldn't get resource attribute from xacml request " + "bad status");
			return null;
		} // (resourceAttribute.getStatus() == null) == everything is ok

		AttributeValue attributeValue = attribute.getAttributeValue();
		if (! (attributeValue instanceof BagAttribute)) {
			log("AttributeFinder:getAttributeFromEvaluationCtx" + iAm() + " exit on " + "couldn't get resource attribute from xacml request " + "no bag");
			return null;
		}

		BagAttribute bag = (BagAttribute) attributeValue;
		if (1 != bag.size()) {
			log("AttributeFinder:getAttributeFromEvaluationCtx" + iAm() + " exit on " + "couldn't get resource attribute from xacml request " + "wrong bag n=" + bag.size());
			return null;
		} 
			
		Iterator it = bag.iterator();
		Object element = it.next();
		
		if (element == null) {
			log("AttributeFinder:getAttributeFromEvaluationCtx" + iAm() + " exit on " + "couldn't get resource attribute from xacml request " + "null returned");
			return null;
		}
		
		if (it.hasNext()) {
			log("AttributeFinder:getAttributeFromEvaluationCtx" + iAm() + " exit on " + "couldn't get resource attribute from xacml request " + "too many returned");
			log(element.toString());
			while(it.hasNext()) {
				log((it.next()).toString());									
			}
			return null;
		}
		
		log("AttributeFinder:getAttributeFromEvaluationCtx " + iAm() + " returning " + element.toString());
		return element;
	}
		
	protected final HashSet attributesDenied = new HashSet();
	
	private final Hashtable attributeIdUris = new Hashtable();	
	private final Hashtable attributeTypes = new Hashtable();
	private final Hashtable attributeTypeUris = new Hashtable();
	protected final void registerAttribute(String id, String type) throws URISyntaxException {
		log("registering attribute " + iAm() + " " +  id);
		attributeIdUris.put(id, new URI(id));
		attributeTypeUris.put(id, new URI(type));
		attributeTypes.put(id, type);			
	}

	protected final URI getAttributeIdUri(String id) {
		return (URI) attributeIdUris.get(id);	
	}
	
	protected final boolean hasAttribute(String id) {
		return attributeIdUris.containsKey(id);
	}

	private final void showRegisteredAttributes() {
		Iterator it = attributeIdUris.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			log("another registered attribute  = " + iAm() + " "  + key);
		}
	}

	
	protected final String getAttributeType(String id) {
		return (String) attributeTypes.get(id);
	}
	
	protected final URI getAttributeTypeUri(String id) {
		return (URI) attributeTypeUris.get(id);
	}
	
	private static final Set NULLSET = new HashSet();
	private final Set supportedDesignatorTypes = new HashSet();
	protected final void registerSupportedDesignatorType(int designatorType) {
		log("registerSupportedDesignatorType() "  + iAm());
		supportedDesignatorTypes.add(new Integer(designatorType));
	}
	
	public Set getSupportedDesignatorTypes() {
		if ((instantiatedOk != null) && instantiatedOk.booleanValue()) {
			log("getSupportedDesignatorTypes() will return "+ iAm() +" set of elements, n=" + supportedDesignatorTypes.size());
			return supportedDesignatorTypes;			
		}
		log("getSupportedDesignatorTypes() will return "  + iAm() +  "NULLSET");
		return NULLSET;
	}

	protected abstract boolean canHandleAdhoc();
	
	private final boolean willService(URI attributeId) {
		String temp = attributeId.toString();
		if (hasAttribute(temp)) {
			log("willService() " + iAm() + " accept this known serviced attribute");
			return true;
		}
		if (! canHandleAdhoc()) {
			log("willService() " + iAm() + " deny any adhoc attribute");
			return false;								
		}
		if (attributesDenied.contains(temp)) {
			log("willService() " + iAm() + " deny this known adhoc attribute");
			return false;					
		}
		log("willService() " + iAm() + " allow this unknown adhoc attribute");
		return true;
	}
	
	public EvaluationResult findAttribute(
		URI attributeType,
		URI attributeId,
		URI issuer,
		URI category,
		EvaluationCtx context,
		int designatorType) {
		log("AttributeFinder:findAttribute " + iAm());
		log("attributeType=[" + attributeType + "], attributeId=[" + attributeId + "]" + iAm());

		if (! parmsOk(attributeType, attributeId, designatorType)) {
			log("AttributeFinder:findAttribute" + " exit on " + "parms not ok" + iAm());
			if (attributeType == null) {
				try {
					attributeType = new URI(StringAttribute.identifier);
				} catch (URISyntaxException e) {
					//we tried
				}
			}
			return new EvaluationResult(BagAttribute.createEmptyBag(attributeType));
		}
		
		if (! willService(attributeId)) {
			log("AttributeFinder:willService() " + iAm() + " returns false" + iAm());
			return new EvaluationResult(BagAttribute.createEmptyBag(attributeType));			
		}

		if (category != null) {
			log("++++++++++ AttributeFinder:findAttribute " + iAm() + " category=" + category.toString());
		}
		log("++++++++++ AttributeFinder:findAttribute " + iAm() + " designatorType="  + designatorType);

		
		log("about to get temp " + iAm());
		Object temp = getAttributeLocally(designatorType, attributeId.toASCIIString(), category, context);
		log(iAm() + " got temp=" + temp);

		if (temp == null) {
			log("AttributeFinder:findAttribute" + " exit on " + "attribute value not found" + iAm());
			return new EvaluationResult(BagAttribute.createEmptyBag(attributeType));			
		}

		Set set = new HashSet();
		if (temp instanceof String) {
			log("AttributeFinder:findAttribute" + " will return a " + "String " + iAm());
			set.add(new StringAttribute((String)temp));			
		} else if (temp instanceof String[]) {
			log("AttributeFinder:findAttribute" + " will return a " + "String[] " + iAm());
			for (int i = 0; i < ((String[])temp).length; i++) {
				set.add(new StringAttribute(((String[])temp)[i]));			
			}
		} 
		return new EvaluationResult(new BagAttribute(attributeType, set));
				
	}
	
	protected final URI STRING_ATTRIBUTE_URI;
	
	abstract protected Object getAttributeLocally(int designatorType, String attributeId, URI resourceCategory, EvaluationCtx context);
	
	public static boolean log = false; 
	
	protected final void log(String msg) {
		if (! log) return;
		msg = this.getClass().getName() + ": " + msg;
		if (servletContext != null) {
			servletContext.log(msg);
		} else {
			System.err.println(msg);			
		}
	}
}

