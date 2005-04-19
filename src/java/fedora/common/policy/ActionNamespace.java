package fedora.common.policy;

import com.sun.xacml.attr.AnyURIAttribute;
import com.sun.xacml.attr.IntegerAttribute;
import com.sun.xacml.attr.StringAttribute;

import fedora.common.Constants;

public class ActionNamespace extends XacmlNamespace { 
	
	// Properties
	public final XacmlName ID;	
	public final XacmlName API;
	public final XacmlName CONTEXT_ID;
	public final XacmlName OBJECT_STATE;	
	public final XacmlName DISSEMINATOR_STATE;
	public final XacmlName BMECH_PID;
	public final XacmlName BMECH_NAMESPACE;
	public final XacmlName N_PIDS;
	public final XacmlName BDEF_PID;
	public final XacmlName BDEF_NAMESPACE;
	public final XacmlName DISSEMINATOR_METHOD;
	public final XacmlName USER_REPRESENTED;

    // Values of API
	public final XacmlName APIM;
	public final XacmlName APIA;
	
    // Values of ID   
	public final XacmlName ADD_DATASTREAM;	
	public final XacmlName ADD_DISSEMINATOR;	
	public final XacmlName ADMIN_PING;		
	public final XacmlName EXPORT_OBJECT;	
	public final XacmlName GET_DATASTREAM;	
	public final XacmlName GET_DATASTREAM_HISTORY;	
	public final XacmlName GET_DATASTREAMS;	
	public final XacmlName GET_DISSEMINATOR;
	public final XacmlName GET_DISSEMINATORS;	
	public final XacmlName GET_DISSEMINATOR_HISTORY;	
	public final XacmlName GET_NEXT_PID;
	public final XacmlName GET_OBJECT_PROPERTIES;	
	public final XacmlName GET_OBJECT_XML;	
	public final XacmlName INGEST_OBJECT;
	public final XacmlName MODIFY_DATASTREAM_BY_REFERENCE;	
	public final XacmlName MODIFY_DATASTREAM_BY_VALUE;
	public final XacmlName MODIFY_DISSEMINATOR;		
	public final XacmlName MODIFY_OBJECT;
	public final XacmlName PURGE_OBJECT;
	public final XacmlName PURGE_DATASTREAM;
	public final XacmlName PURGE_DISSEMINATOR;	
	public final XacmlName SET_DATASTREAM_STATE;	
	public final XacmlName SET_DISSEMINATOR_STATE;	
	public final XacmlName DESCRIBE_REPOSITORY;	
	public final XacmlName FIND_OBJECTS;
	public final XacmlName RI_FIND_OBJECTS;	
	public final XacmlName GET_DATASTREAM_DISSEMINATION;	
	public final XacmlName GET_DISSEMINATION;	
	public final XacmlName GET_OBJECT_HISTORY;	
	public final XacmlName GET_OBJECT_PROFILE;	
	public final XacmlName LIST_DATASTREAMS;	
	public final XacmlName LIST_METHODS;		
	public final XacmlName LIST_OBJECT_IN_FIELD_SEARCH_RESULTS;
	public final XacmlName LIST_OBJECT_IN_RESOURCE_INDEX_RESULTS;
	public final XacmlName SURROGATE_PING;
	public final XacmlName SERVER_SHUTDOWN;
	public final XacmlName SERVER_STATUS;
	public final XacmlName OAI;	
	public final XacmlName UPLOAD;	
	
	public final XacmlName FORMAT_URI;
	public final XacmlName CONTEXT;	
	public final XacmlName ENCODING;	
	public final XacmlName DATASTREAM_MIME_TYPE;	
	public final XacmlName DATASTREAM_FORMAT_URI;	
	public final XacmlName DATASTREAM_LOCATION;	
	public final XacmlName DATASTREAM_STATE;	
	

    private ActionNamespace(XacmlNamespace parent, String localName) {
    	super(parent, localName);
    	API = addName(new XacmlName(this, "api", StringAttribute.identifier));
    	APIM               = addName(new XacmlName(this, "api-m"));
    	APIA               = addName(new XacmlName(this, "api-a"));

    	ID = addName(new XacmlName(this, "id", StringAttribute.identifier));
    	// derived from respective Java methods in Access.java or Management.java    	
    	ADD_DATASTREAM               = addName(new XacmlName(this, "id-addDatastream"));	
    	ADD_DISSEMINATOR               = addName(new XacmlName(this, "id-addDisseminator"));	
    	ADMIN_PING               = addName(new XacmlName(this, "id-adminPing"));    	
    	EXPORT_OBJECT               = addName(new XacmlName(this, "id-exportObject"));	
    	GET_DATASTREAM               = addName(new XacmlName(this, "id-getDatastream"));	
    	GET_DATASTREAM_HISTORY               = addName(new XacmlName(this, "id-getDatastreamHistory"));	
    	GET_DATASTREAMS               = addName(new XacmlName(this, "id-getDatastreams"));	
    	GET_DISSEMINATOR               = addName(new XacmlName(this, "id-getDisseminator"));
    	GET_DISSEMINATORS               = addName(new XacmlName(this, "id-getDisseminators"));	
    	GET_DISSEMINATOR_HISTORY               = addName(new XacmlName(this, "id-getDisseminatorHistory"));	
    	GET_NEXT_PID               = addName(new XacmlName(this, "id-getNextPid"));
    	GET_OBJECT_PROPERTIES               = addName(new XacmlName(this, "id-getObjectProperties"));	
    	GET_OBJECT_XML               = addName(new XacmlName(this, "id-getObjectXML"));	
    	INGEST_OBJECT               = addName(new XacmlName(this, "id-ingestObject"));
    	MODIFY_DATASTREAM_BY_REFERENCE               = addName(new XacmlName(this, "id-modifyDatastreamByReference"));	
    	MODIFY_DATASTREAM_BY_VALUE               = addName(new XacmlName(this, "id-modifyDatastreamByValue"));
    	MODIFY_DISSEMINATOR               = addName(new XacmlName(this, "id-modifyDisseminator"));		
    	MODIFY_OBJECT               = addName(new XacmlName(this, "id-modifyObject"));
    	PURGE_OBJECT               = addName(new XacmlName(this, "id-purgeObject"));
    	PURGE_DATASTREAM               = addName(new XacmlName(this, "id-purgeDatastream"));
    	PURGE_DISSEMINATOR               = addName(new XacmlName(this, "id-purgeDisseminator"));	
    	SET_DATASTREAM_STATE               = addName(new XacmlName(this, "id-setDatastreamState"));	
    	SET_DISSEMINATOR_STATE               = addName(new XacmlName(this, "id-setDisseminatorState"));	
    	DESCRIBE_REPOSITORY               = addName(new XacmlName(this, "id-describeRepository"));	
    	FIND_OBJECTS               = addName(new XacmlName(this, "id-findObjects"));	
    	RI_FIND_OBJECTS               = addName(new XacmlName(this, "id-riFindObjects"));	
    	GET_DATASTREAM_DISSEMINATION               = addName(new XacmlName(this, "id-getDatastreamDissemination"));	
    	GET_DISSEMINATION               = addName(new XacmlName(this, "id-getDissemination"));	
    	GET_OBJECT_HISTORY               = addName(new XacmlName(this, "id-getObjectHistory"));	
    	GET_OBJECT_PROFILE               = addName(new XacmlName(this, "id-getObjectProfile"));	
    	LIST_DATASTREAMS               = addName(new XacmlName(this, "id-listDatastreams"));	
    	LIST_METHODS               = addName(new XacmlName(this, "id-listMethods"));		
    	LIST_OBJECT_IN_FIELD_SEARCH_RESULTS               = addName(new XacmlName(this, "id-listObjectInFieldSearchResults"));
    	LIST_OBJECT_IN_RESOURCE_INDEX_RESULTS               = addName(new XacmlName(this, "id-listObjectInResourceIndexResults"));
    	SURROGATE_PING = addName(new XacmlName(this, "id-actAsSurrogateFor"));
    	SERVER_SHUTDOWN = addName(new XacmlName(this, "id-serverShutdown")); 
    	SERVER_STATUS = addName(new XacmlName(this, "id-serverStatus"));    
    	OAI = addName(new XacmlName(this, "id-oai"));     	
    	UPLOAD = addName(new XacmlName(this, "id-upload"));     	
    	
    	
    	DATASTREAM_FORMAT_URI = addName(new XacmlName(this, "datastreamFormatUri", AnyURIAttribute.identifier));    	
    	DATASTREAM_LOCATION = addName(new XacmlName(this, "datastreamLocation", AnyURIAttribute.identifier));
    	DATASTREAM_MIME_TYPE = addName(new XacmlName(this, "datastreamMimeType", StringAttribute.identifier));    	
    	DATASTREAM_STATE = addName(new XacmlName(this, "datastreamState", StringAttribute.identifier));
    	DISSEMINATOR_METHOD = addName(new XacmlName(this, "disseminatorMethod", StringAttribute.identifier));    	
       	DISSEMINATOR_STATE = addName(new XacmlName(this, "disseminatorState", StringAttribute.identifier));    	
       	BDEF_NAMESPACE = addName(new XacmlName(this, "bdefNamespace", StringAttribute.identifier));
    	BDEF_PID = addName(new XacmlName(this, "bdefPid", StringAttribute.identifier));
    	BMECH_NAMESPACE = addName(new XacmlName(this, "bmechNamespace", StringAttribute.identifier));    	
       	BMECH_PID = addName(new XacmlName(this, "bmechPid", StringAttribute.identifier));
    	CONTEXT = addName(new XacmlName(this, "context", StringAttribute.identifier));
    	ENCODING = addName(new XacmlName(this, "encoding", StringAttribute.identifier));
    	FORMAT_URI = addName(new XacmlName(this, "formatUri", AnyURIAttribute.identifier));    	    	    	
    	OBJECT_STATE = addName(new XacmlName(this, "objectState", StringAttribute.identifier));
    	N_PIDS = addName(new XacmlName(this, "nPids", IntegerAttribute.identifier));
    	USER_REPRESENTED = addName(new XacmlName(this, "subjectRepresented", StringAttribute.identifier));    	
  	
    	CONTEXT_ID = addName(new XacmlName(this, "contextId", StringAttribute.identifier)); //internal callback support
    	// Values of CONTEXT_ID are sequential numerals, hence not enumerated here.
    	

    }

	public static ActionNamespace onlyInstance = new ActionNamespace(Release2_1Namespace.getInstance(), "action");
	
	public static final ActionNamespace getInstance() {
		return onlyInstance;
	}

}
