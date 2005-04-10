package fedora.server.security;
import java.util.Date;
import java.net.URI;
import java.net.URISyntaxException;
import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.attr.AnyURIAttribute;
import com.sun.xacml.attr.AttributeDesignator;
import com.sun.xacml.attr.DateTimeAttribute;
import com.sun.xacml.attr.IntegerAttribute;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.cond.EvaluationResult;

import fedora.common.Constants;
import fedora.common.policy.XacmlName;
import fedora.server.ReadOnlyContext;
import fedora.server.errors.ServerException;
import fedora.server.storage.DOManager;
import fedora.server.storage.DOReader;
import fedora.server.storage.types.Datastream;
import fedora.server.utilities.DateUtility;

/*package*/ class ResourceAttributeFinderModule extends AttributeFinderModule {
	
	protected boolean canHandleAdhoc() {
		return false;
	}

	//protected boolean adhoc() { return false; }

	static private final ResourceAttributeFinderModule singleton = new ResourceAttributeFinderModule();
 
	private ResourceAttributeFinderModule() {
		super();
		try {
			registerAttribute(Constants.OBJECT.STATE.uri, Constants.OBJECT.STATE.datatype);
			registerAttribute(Constants.OBJECT.OBJECT_TYPE.uri, Constants.OBJECT.OBJECT_TYPE.datatype);			
			registerAttribute(Constants.OBJECT.OWNER.uri, Constants.OBJECT.OWNER.datatype);
			registerAttribute(Constants.OBJECT.CONTENT_MODEL.uri, Constants.OBJECT.CONTENT_MODEL.datatype);
			registerAttribute(Constants.OBJECT.CREATED_DATETIME.uri, Constants.OBJECT.CREATED_DATETIME.datatype);
			registerAttribute(Constants.OBJECT.LAST_MODIFIED_DATETIME.uri, Constants.OBJECT.LAST_MODIFIED_DATETIME.datatype);				

			registerAttribute(Constants.DATASTREAM.STATE.uri, Constants.DATASTREAM.STATE.datatype);			
			registerAttribute(Constants.DATASTREAM.CONTROL_GROUP.uri, Constants.DATASTREAM.CONTROL_GROUP.datatype);			
			registerAttribute(Constants.DATASTREAM.CREATED_DATETIME.uri, Constants.DATASTREAM.CREATED_DATETIME.datatype);			
			registerAttribute(Constants.DATASTREAM.INFO_TYPE.uri, Constants.DATASTREAM.INFO_TYPE.datatype);			
			registerAttribute(Constants.DATASTREAM.LOCATION_TYPE.uri, Constants.DATASTREAM.LOCATION_TYPE.datatype);			
			registerAttribute(Constants.DATASTREAM.MIME_TYPE.uri, Constants.DATASTREAM.MIME_TYPE.datatype);			
			registerAttribute(Constants.DATASTREAM.CONTENT_LENGTH.uri, Constants.DATASTREAM.CONTENT_LENGTH.datatype);			
			registerAttribute(Constants.DATASTREAM.FORMAT_URI.uri, Constants.DATASTREAM.FORMAT_URI.datatype);			
			registerAttribute(Constants.DATASTREAM.LOCATION.uri, Constants.DATASTREAM.LOCATION.datatype);			
			
			registerSupportedDesignatorType(AttributeDesignator.RESOURCE_TARGET);
			setInstantiatedOk(true);
		} catch (URISyntaxException e1) {
			setInstantiatedOk(false);
		}
	}

	static public final ResourceAttributeFinderModule getInstance() {
		return singleton;
	}

	private DOManager doManager = null;
	
	protected void setDOManager(DOManager doManager) {
		if (this.doManager == null) {
			this.doManager = doManager;
		}
	}
	
	private final String getResourceId(EvaluationCtx context) {
		URI resourceIdType = null;
		URI resourceIdId = null;
		try {
			resourceIdType = new URI(StringAttribute.identifier);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			resourceIdId = new URI(EvaluationCtx.RESOURCE_ID);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		EvaluationResult attribute = context.getResourceAttribute(resourceIdType, resourceIdId, null);

		Object element = getAttributeFromEvaluationResult(attribute);
		if (element == null) {
			log("ResourceAttributeFinder:findAttribute" + " exit on " + "can't get resource-id on request callback");
			return null;
		}

		if (! (element instanceof StringAttribute)) {
			log("ResourceAttributeFinder:findAttribute" + " exit on " + "couldn't get resource-id from xacml request " + "non-string returned");
			return null;			
		}
 
		String resourceId = ((StringAttribute) element).getValue();			
		
		if (resourceId == null) {
			log("ResourceAttributeFinder:findAttribute" + " exit on " + "null resource-id");
			return null;			
		}

		if (! validResourceId(resourceId)) {
			log("ResourceAttributeFinder:findAttribute" + " exit on " + "invalid resource-id");
			return null;			
		}
		
		return resourceId;			
	}

	private final boolean validResourceId(String resourceId) {
		if (resourceId == null)
			return false;		
		// "" is a valid resource id, for it represents a don't-care condition
		if (" ".equals(resourceId))
			return false;
		return true;
	}
	
	private final String getDatastreamId(EvaluationCtx context) {
		URI datastreamIdUri = null;
		try {
			datastreamIdUri = new URI(Constants.DATASTREAM.ID.uri);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		EvaluationResult attribute = context.getResourceAttribute(STRING_ATTRIBUTE_URI, datastreamIdUri, null);

		Object element = getAttributeFromEvaluationResult(attribute);
		if (element == null) {
			log("getDatastreamId: " + " exit on " + "can't get resource-id on request callback");
			return null;
		}

		if (! (element instanceof StringAttribute)) {
			log("getDatastreamId: " + " exit on " + "couldn't get resource-id from xacml request " + "non-string returned");
			return null;			
		}
 
		String datastreamId = ((StringAttribute) element).getValue();			
		
		if (datastreamId == null) {
			log("getDatastreamId: " + " exit on " + "null resource-id");
			return null;			
		}

		if (! validDatastreamId(datastreamId)) {
			log("getDatastreamId: " + " exit on " + "invalid resource-id");
			return null;			
		}
		
		return datastreamId;			
	}

	private final boolean validDatastreamId(String datastreamId) {
		if (datastreamId == null)
			return false;		
		// "" is a valid resource id, for it represents a don't-care condition
		if (" ".equals(datastreamId))
			return false;
		return true;
	}

	
	protected final Object getAttributeLocally(int designatorType, String attributeId, URI resourceCategory, EvaluationCtx context) {
		String pid = getPid(context);		
		if ("".equals(pid)) {
			log("no pid");
			return null;
		}
		log("getResourceAttribute, pid=" + pid);
		DOReader reader = null;
		try {
			log("pid="+pid);			
			reader = doManager.getReader(ReadOnlyContext.EMPTY, pid);
		} catch (ServerException e) {
			log("couldn't get object reader");
			return null;
		}
		String[] values = null;
		if (Constants.OBJECT.STATE.uri.equals(attributeId)) {
			try {
				values = new String[1];
				values[0] = reader.GetObjectState();
				log("got " + Constants.OBJECT.STATE.uri + "=" + values[0]);
			} catch (ServerException e) {
				log("failed getting " + Constants.OBJECT.STATE.uri);
				return null;					
			}
		} else if (Constants.OBJECT.OBJECT_TYPE.uri.equals(attributeId)) { 
			try {
				values = new String[1];
				values[0] = reader.getOwnerId();
				log("got " + Constants.OBJECT.OBJECT_TYPE.uri + "=" + values[0]);
			} catch (ServerException e) {
				log("failed getting " + Constants.OBJECT.OBJECT_TYPE.uri);
				return null;					
			}			
		} else if (Constants.OBJECT.OWNER.uri.equals(attributeId)) { 
				try {
					values = new String[1];
					values[0] = reader.getOwnerId();
					log("got " + Constants.OBJECT.OWNER.uri + "=" + values[0]);
				} catch (ServerException e) {
					log("failed getting " + Constants.OBJECT.OWNER.uri);
					return null;					
				}
		} else if (Constants.OBJECT.CONTENT_MODEL.uri.equals(attributeId)) { 
			try {
				values = new String[1];
				values[0] = reader.getContentModelId();
				log("got " + Constants.OBJECT.CONTENT_MODEL.uri + "=" + values[0]);
			} catch (ServerException e) {
				log("failed getting " + Constants.OBJECT.CONTENT_MODEL.uri);
				return null;					
			}			
		} else if (Constants.OBJECT.CREATED_DATETIME.uri.equals(attributeId)) { 
			try {
				values = new String[1];
				values[0] = DateUtility.convertDateToString(reader.getCreateDate());
				log("got " + Constants.OBJECT.CREATED_DATETIME.uri + "=" + values[0]);
			} catch (ServerException e) {
				log("failed getting " + Constants.OBJECT.CREATED_DATETIME.uri);
				return null;					
			}		
		} else if (Constants.OBJECT.LAST_MODIFIED_DATETIME.uri.equals(attributeId)) { 
			try {
				values = new String[1];
				values[0] = DateUtility.convertDateToString(reader.getLastModDate());
				log("got " + Constants.OBJECT.LAST_MODIFIED_DATETIME.uri + "=" + values[0]);
			} catch (ServerException e) {
				log("failed getting " + Constants.OBJECT.LAST_MODIFIED_DATETIME.uri);
				return null;					
			}			
		} else if ((Constants.DATASTREAM.STATE.uri.equals(attributeId))
		       ||  (Constants.DATASTREAM.CONTROL_GROUP.uri.equals(attributeId))
		       ||  (Constants.DATASTREAM.FORMAT_URI.uri.equals(attributeId))
		       ||  (Constants.DATASTREAM.CREATED_DATETIME.uri.equals(attributeId))
		       ||  (Constants.DATASTREAM.INFO_TYPE.uri.equals(attributeId))
		       ||  (Constants.DATASTREAM.LOCATION.uri.equals(attributeId))
		       ||  (Constants.DATASTREAM.LOCATION_TYPE.uri.equals(attributeId))
		       ||  (Constants.DATASTREAM.MIME_TYPE.uri.equals(attributeId))
		       ||  (Constants.DATASTREAM.CONTENT_LENGTH.uri.equals(attributeId)) ) {			
			String datastreamId = getDatastreamId(context);
			if ("".equals(datastreamId)) {
				log("no datastreamId");
				return null;
			}
			log("datastreamId=" + datastreamId);
			Datastream datastream;
			try {
				datastream = reader.GetDatastream(datastreamId, new Date()); //right import (above)?
			} catch (ServerException e) {
				log("couldn't get datastream");
				return null;					
			}
			if (datastream == null) {
				log("got null datastream");
				return null;
			}
			
			if (Constants.DATASTREAM.STATE.uri.equals(attributeId)) {
				values = new String[1];
				values[0] = datastream.DSState;				
			} else if (Constants.DATASTREAM.CONTROL_GROUP.uri.equals(attributeId)) {
				values = new String[1];
				values[0] = datastream.DSControlGrp;
			} else if (Constants.DATASTREAM.FORMAT_URI.uri.equals(attributeId)) {
				values = new String[1];
				values[0] = datastream.DSFormatURI;
			} else if (Constants.DATASTREAM.CREATED_DATETIME.uri.equals(attributeId)) {
				values = new String[1];
				values[0] = DateUtility.convertDateToString(datastream.DSCreateDT);  
			} else if (Constants.DATASTREAM.INFO_TYPE.uri.equals(attributeId)) {
				values = new String[1];
				values[0] = datastream.DSInfoType;
			} else if (Constants.DATASTREAM.LOCATION.uri.equals(attributeId)) {
				values = new String[1];
				values[0] = datastream.DSLocation;
			} else if (Constants.DATASTREAM.LOCATION_TYPE.uri.equals(attributeId)) {
				values = new String[1];
				values[0] = datastream.DSLocationType;
			} else if (Constants.DATASTREAM.MIME_TYPE.uri.equals(attributeId)) {
				values = new String[1];
				values[0] = datastream.DSMIME;
			} else if (Constants.DATASTREAM.CONTENT_LENGTH.uri.equals(attributeId)) {
				values = new String[1];
				values[0] = Long.toString(datastream.DSSize);
			} else {
				log("looking for unknown resource attribute=" + attributeId);							
			}
		} else {
			log("looking for unknown resource attribute=" + attributeId);			
		}
		return values;
	}

    private final String getPid(EvaluationCtx context) {
		URI resourceIdType = null;
		URI resourceIdId = null;
		try {
			resourceIdType = new URI(StringAttribute.identifier);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			resourceIdId = new URI(Constants.OBJECT.PID.uri);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		EvaluationResult attribute = context.getResourceAttribute(resourceIdType, resourceIdId, null);    
		Object element = getAttributeFromEvaluationResult(attribute);
		if (element == null) {
			log("PolicyFinderModule:getPid" + " exit on " + "can't get contextId on request callback");
			return null;
		}

		if (! (element instanceof StringAttribute)) {
			log("PolicyFinderModule:getPid" + " exit on " + "couldn't get contextId from xacml request " + "non-string returned");
			return null;			
		}
 
		String pid = ((StringAttribute) element).getValue();			
		
		if (pid == null) {
			log("PolicyFinderModule:getPid" + " exit on " + "null contextId");
			return null;			
		}

		return pid;				
    }	
	
}

