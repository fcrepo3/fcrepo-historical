package fedora.server.security;

import java.util.Date;
import fedora.server.Context;
import fedora.server.errors.NotAuthorizedException;
import fedora.server.storage.types.Property;
import fedora.server.utilities.DateUtility;

public interface Authorization {
	
	/*
	public void enforceResourceIndexVisibility(Context context, String pid) 
	throws NotAuthorizedException;
	public void enforceSearchVisibility(Context context, String pid) 
	throws NotAuthorizedException;
	*/
	
	public static final String UNDEFINED = "UNDEFINED";

	//subject
	public static final String SUBJECT_CATEGORY = "urn:oasis:names:tc:xacml:1.0:subject";
	public static final String SUBJECT_CATEGORY_ACCESS = "urn:oasis:names:tc:xacml:1.0:subject-category:access-subject";
	public static final String SUBJECT_ID_URI_STRING = "urn:oasis:names:tc:xacml:1.0:subject:subject-id";
                                     
	//action	
	public static final String ACTION_CATEGORY = "urn:oasis:names:tc:xacml:1.0:action";
	public static final String ACTION_CATEGORY_ACCESS = "urn:oasis:names:tc:xacml:1.0:action-category:access-action";
	public static final String ACTION_ID_URI_STRING = "urn:oasis:names:tc:xacml:1.0:action:action-id";


	public static final String ACTION_NEW_STATE_URI_STRING = "urn:oasis:names:tc:xacml:1.0:action:fedora-newState";	
	public static final String ACTION_DATASTREAM_NEW_STATE_URI_STRING = "urn:oasis:names:tc:xacml:1.0:action:fedora-datastreamNewState";
	public static final String ACTION_DISSEMINATOR_NEW_STATE_URI_STRING = "urn:oasis:names:tc:xacml:1.0:action:fedora-disseminatorNewState";

	//resource
	public static final String RESOURCE_CATEGORY = "urn:oasis:names:tc:xacml:1.0:resource";
	public static final String RESOURCE_CATEGORY_ACCESS = "urn:oasis:names:tc:xacml:1.0:resource-category:access-resource";
	public static final String RESOURCE_ID_URI_STRING = "urn:oasis:names:tc:xacml:1.0:resource:resource-id";
	
	//pid is given as resource-id
	public static final String RESOURCE_NAMESPACE_URI_STRING = "urn:oasis:names:tc:xacml:1.0:resource:fedora-namespace";
	public static final String RESOURCE_AS_OF_DATE_URI_STRING = "urn:oasis:names:tc:xacml:1.0:resource:fedora-asOfDate";
	
	//public static final String RESOURCE_OBJECT_STATE_URI_STRING = "info:fedora/fedora-system:def/model#state";
	//public static final String RESOURCE_DATASTREAM_ID_URI_STRING = "urn:oasis:names:tc:xacml:1.0:resource:fedora-datastream-id";	
	// no namespace for datastream id, which is not a pid	
	//public static final String RESOURCE_DATASTREAM_STATE_URI_STRING = "info:fedora/fedora-system:def/model:datastream-state"; // <<feed back	
	public static final String RESOURCE_DATASTREAM_LOCATION_URI_STRING = "urn:oasis:names:tc:xacml:1.0:resource:fedora-datastreamLocation";
	public static final String RESOURCE_DATASTREAM_CONTROL_GROUP_URI_STRING = "urn:oasis:names:tc:xacml:1.0:resource:fedora-datastreamControlGroup";
	public static final String RESOURCE_BDEF_PID_URI_STRING = "urn:oasis:names:tc:xacml:1.0:resource:fedora-bdefPid";
	public static final String RESOURCE_BDEF_NAMESPACE_URI_STRING = "urn:oasis:names:tc:xacml:1.0:resource:fedora-bdefNamespace";
	public static final String RESOURCE_BMECH_PID_URI_STRING = "urn:oasis:names:tc:xacml:1.0:resource:fedora-bmechPid";
	public static final String RESOURCE_BMECH_NAMESPACE_URI_STRING = "urn:oasis:names:tc:xacml:1.0:resource:fedora-bmechNamespace";
	public static final String RESOURCE_DISSEMINATOR_PID_URI_STRING = "urn:oasis:names:tc:xacml:1.0:resource:fedora-disseminatorId";
	public static final String RESOURCE_DISSEMINATOR_NAMESPACE_URI_STRING = "urn:oasis:names:tc:xacml:1.0:resource:fedora-disseminatorNamespace";	
	public static final String RESOURCE_DISSEMINATOR_STATE_URI_STRING = "urn:oasis:names:tc:xacml:1.0:resource:fedora-disseminatorState";
	public static final String RESOURCE_DISSEMINATOR_METHOD_URI_STRING = "urn:oasis:names:tc:xacml:1.0:resource:fedora-disseminatorMethod";
	public static final String RESOURCE_DISSEMINATED_PID_URI_STRING = "urn:oasis:names:tc:xacml:1.0:resource:fedora-disseminatedPid";
	public static final String RESOURCE_DISSEMINATED_NAMESPACE_URI_STRING = "urn:oasis:names:tc:xacml:1.0:resource:fedora-disseminatedNamespace";
	public static final String RESOURCE_N_NEW_PIDS_URI_STRING = "urn:oasis:names:tc:xacml:1.0:resource:fedora-nNewPids";
	
	//environment
	public static final String ENVIRONMENT_CATEGORY = "urn:oasis:names:tc:xacml:1.0:environment";
	public static final String ENVIRONMENT_CATEGORY_ACCESS = "urn:oasis:names:tc:xacml:1.0:environment-category:access-environment";

	//APIM 

	public void enforceAddDatastream(Context context, String pid, String dsId, String dsLocation, String controlGroup, String dsState) 
	throws NotAuthorizedException;

	public void enforceAddDisseminator(Context context, String pid) 
	throws NotAuthorizedException;
	
	public void enforceExportObject(Context context, String pid) 
	throws NotAuthorizedException;

	public void enforceGetDatastream(Context context, String pid, String datastreamId) 
	throws NotAuthorizedException;

	public void enforceGetDatastreamHistory(Context context, String pid, String datastreamId) 
	throws NotAuthorizedException;

	public void enforceGetDatastreams(Context context, String pid, Date asOfDate, String state) 
	throws NotAuthorizedException;
	
	public void enforceGetDisseminator(Context context, String pid, String disseminatorId, Date asOfDateTime) 
	throws NotAuthorizedException;
	
	public void enforceGetDisseminators(Context context, String pid, Date asOfDate, String disseminatorState) 
	throws NotAuthorizedException;

	public void enforceGetDisseminatorHistory(Context context, String pid, String disseminatorPid) 
	throws NotAuthorizedException;
	
	public void enforceGetNextPid(Context context, String namespace, int nNewPids) 
	throws NotAuthorizedException; 
	
	public void enforceGetObjectProperties(Context context, String pid) 
	throws NotAuthorizedException;
	
	public void enforceGetObjectXML(Context context, String pid) 
	throws NotAuthorizedException;

	public void enforceIngestObject(Context context, String pid) 
	throws NotAuthorizedException;
	
	public void enforceListObjectInFieldSearchResults(Context context, String pid) 
	throws NotAuthorizedException;
	
	public void enforceListObjectInResourceIndexResults(Context context, String pid) 
	throws NotAuthorizedException;

	public void enforceModifyDatastreamByReference(Context context, String pid, String datastreamId, String datastreamLocation, String datastreamState) 
	throws NotAuthorizedException;
	
	public void enforceModifyDatastreamByValue(Context context, String pid, String datastreamId, String datastreamState) 
	throws NotAuthorizedException;
	
	public void enforceModifyDisseminator(Context context, String pid, String disseminatorId, String mechanismPid, String disseminatorState) 
	throws NotAuthorizedException;
	
	public void enforceModifyObject(Context context, String pid, String objectState) 
	throws NotAuthorizedException;
	
	public void enforcePurgeDatastream(Context context, String pid, String datastreamId) 
	throws NotAuthorizedException;

	public void enforcePurgeDisseminator(Context context, String pid, String disseminatorId) 
	throws NotAuthorizedException;

	public void enforcePurgeObject(Context context, String pid) 
	throws NotAuthorizedException;

	public void enforceSetDatastreamState(Context context, String pid, String datastreamId, String datastreamNewState) 
	throws NotAuthorizedException;

	public void enforceSetDisseminatorState(Context context, String pid, String disseminatorId, String disseminatorNewState) 
	throws NotAuthorizedException;

	//APIA

	public void enforceDescribeRepository(Context context) 
	throws NotAuthorizedException;

	public void enforceFindObjects(Context context) 
	throws NotAuthorizedException;
	
	public void enforceRIFindObjects(Context context) 
	throws NotAuthorizedException;

	public void enforceGetDatastreamDissemination(Context context, String pid, String datastreamId, Date asOfDate) 
	throws NotAuthorizedException;
	
	public void enforceGetDissemination(Context context, String pid, String bDefPID, String methodName, Date asOfDate) 
	throws NotAuthorizedException;

	public void enforceGetObjectHistory(Context context, String pid) 
	throws NotAuthorizedException;

	public void enforceGetObjectProfile(Context context, String pid, Date asOfDate) 
	throws NotAuthorizedException;

	public void enforceListDatastreams(Context context, String pid, Date asOfDate) 
	throws NotAuthorizedException;

	public void enforceListMethods(Context context, String pid, Date ofAsDate) 
	throws NotAuthorizedException;

}
