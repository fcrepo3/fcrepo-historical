package fedora.server.resourceIndex;

import fedora.server.Logging;
import fedora.server.StdoutLogging;
import fedora.server.errors.ServerException;
import fedora.server.errors.ResourceIndexException;
import fedora.server.storage.service.ServiceMapper;
import fedora.server.storage.types.*;
import fedora.server.utilities.DCFields;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import org.xml.sax.InputSource;

import com.hp.hpl.jena.rdql.Value;

/**
 * Implementation of the ResourceIndex interface.
 * 
 * @author Edwin Shin
 *
 */
public class ResourceIndexImpl extends StdoutLogging implements ResourceIndex {
    // TODO how are we going to handle different indexing levels?
    // 		- initial index
    //		- latent indexing: "tagging" objects with current index level to support querying what needs indexing later
    //		- subsequent insert/edits/deletes
    //		- changes in levels
    //		- distinct levels or discrete mix & match (e.g., combinations of DC, REP & REP-DEP, RELS, etc.)
    private int m_indexLevel;
	private RIStore m_store;
	private static final String FEDORA_URI_SCHEME = "info:fedora/";
	private static final String DC_URI_PREFIX = "http://purl.org/dc/elements/1.1/";
	
	/**
	 * @param target
	 */
	public ResourceIndexImpl(int indexLevel, RIStore store, Logging target) {
		super(target);
		m_indexLevel = indexLevel;
		m_store = store;
	}
	
	/* (non-Javadoc)
	 * @see fedora.server.resourceIndex.ResourceIndex#query(fedora.server.resourceIndex.RIQuery)
	 */
	public RIResultIterator executeQuery(RIQuery query) throws ResourceIndexException {
		return m_store.executeQuery(query);
	}

	/* (non-Javadoc)
	 * @see fedora.server.resourceIndex.ResourceIndex#addDigitalObject(fedora.server.storage.types.DigitalObject)
	 */
	public void addDigitalObject(DigitalObject digitalObject) throws ResourceIndexException {
	    String pid = digitalObject.getPid();
		String doIdentifier = getDOURI(digitalObject);
		
		// Insert basic system metadata
		m_store.insertLiteral(doIdentifier, LABEL_URI, digitalObject.getLabel());
		m_store.insertLiteral(doIdentifier, DATE_CREATED_URI, getDate(digitalObject.getCreateDate()));
		m_store.insertLiteral(doIdentifier, DATE_LAST_MODIFIED_URI, getDate(digitalObject.getLastModDate()));
		
		if (digitalObject.getOwnerId() != null) {
		    m_store.insertLiteral(doIdentifier, OWNER_ID_URI, digitalObject.getOwnerId());
		}
		m_store.insertLiteral(doIdentifier, CONTENT_MODEL_ID_URI, digitalObject.getContentModelId());
		m_store.insertLiteral(doIdentifier, STATE_URI, digitalObject.getState());
		
		// handle type specific duties
		// TODO: if it turns out rdfType is the only "special" thing to do,
		// then we may as well use a getRDFType(fedoraObjectType) method instead
		int fedoraObjectType = digitalObject.getFedoraObjectType();
		String rdfType;
		switch (fedoraObjectType) {
			case DigitalObject.FEDORA_BDEF_OBJECT: 
				addBDef(digitalObject);
				break;
			case DigitalObject.FEDORA_BMECH_OBJECT: 
				addBMech(digitalObject);
				break;
			case DigitalObject.FEDORA_OBJECT: 
				addDataObject(digitalObject);
				break;
			default: throw new ResourceIndexException("Unknown DigitalObject type: " + fedoraObjectType);	
		}
		
		// Add datastreams
		Iterator it;
	    it = digitalObject.datastreamIdIterator();
		while (it.hasNext()) {
		    addDatastream(digitalObject, (String)it.next());
		}
		
		// Add disseminators
		it = digitalObject.disseminatorIdIterator();
		while (it.hasNext()) {
		    addDisseminator(digitalObject, (String)it.next());		    
		}
		
		// just for testing
		//try {
		//    m_store.write(new FileOutputStream("/tmp/rdf/" + pid + ".xml"));
		//} catch (Exception e) {
		//    e.printStackTrace();
		//}
		
	}

	/* (non-Javadoc)
	 * @see fedora.server.resourceIndex.ResourceIndex#addDatastream(fedora.server.storage.types.Datastream)
	 */
	public void addDatastream(DigitalObject digitalObject, String datastreamID) throws ResourceIndexException {
	    Datastream ds = getLatestDatastream(digitalObject.datastreams(datastreamID));
	    String doURI = getDOURI(digitalObject);
	    String datastreamURI;
	    if (ds.DatastreamURI != null && !ds.DatastreamURI.equals("")) {
	        datastreamURI = ds.DatastreamURI;
	    } else {
	        datastreamURI = doURI + "/" + datastreamID;
	    }
	    
	    m_store.insert(doURI, HAS_REPRESENTATION_URI, datastreamURI);
	    m_store.insertLiteral(datastreamURI, DATE_LAST_MODIFIED_URI, getDate(ds.DSCreateDT));
	    
		// handle special system datastreams: DC, METHODMAP, RELS-EXT
		if (datastreamID.equalsIgnoreCase("DC")) {
			addDublinCoreDatastream(digitalObject, ds);
		} else if (datastreamID.equalsIgnoreCase("METHODMAP")) { 
			addMethodMapDatastream(digitalObject, ds);
		} else if (datastreamID.equalsIgnoreCase("RELS-EXT")) {
		    addRelsDatastream(ds);
		} else if (datastreamID.equalsIgnoreCase("EXT_PROPERTIES")) { // props
		    // Do nothing
		} else if (datastreamID.equalsIgnoreCase("DSINPUTSPEC")) { // which objs have this?
//		    // representationURI = doURI + "/" + bdefPID + "/" + bdefmethodname
//		    // doURI, represents, representationURI
//		    // mimetype = 
//		    // representationURI, hasMimeType, mimetype
//		    DatastreamXMLMetadata dsInSpecDS = (DatastreamXMLMetadata)ds;
//		    ServiceMapper serviceMapper = new ServiceMapper(digitalObject.getPid());
//		    BMechDSBindSpec dsBindSpec;
//		    try {
//		        dsBindSpec = serviceMapper.getDSInputSpec(new InputSource(new ByteArrayInputStream(dsInSpecDS.xmlContent)));
//		    } catch (Throwable t) {
//		        throw new ResourceIndexException(t.getMessage());
//		    }
//		    String bDefPID = dsBindSpec.bDefPID;
//		    
//		    String bMechPID = dsBindSpec.bMechPID;
//		    
//		    BMechDSBindRule[] x = dsBindSpec.dsBindRules;
//		    
//		    
		} else if (datastreamID.equalsIgnoreCase("WSDL")) { 
		    
		} else if (datastreamID.equalsIgnoreCase("SERVICE-PROFILE")) { 
		    
		} else {

		}
		
	}

	/* (non-Javadoc)
	 * @see fedora.server.resourceIndex.ResourceIndex#addDisseminator(fedora.server.storage.types.Dissemination)
	 */
	public void addDisseminator(DigitalObject digitalObject, String disseminatorID) throws ResourceIndexException {
	    Disseminator diss = getLatestDisseminator(digitalObject.disseminators(disseminatorID));
	    String doIdentifier = getDOURI(digitalObject);
	    m_store.insert(doIdentifier, USES_BMECH_URI, getDOURI(diss.bMechID));
		
	    DSBindingMap m = diss.dsBindMap; // is this needed???
	    String bDefPID = diss.bDefID;
	    
	    // insert representations
	    if (digitalObject.getFedoraObjectType() == DigitalObject.FEDORA_OBJECT) {
		    List methods = getMethodNames(bDefPID);
		    Iterator it = methods.iterator();
		    String rep;
		    while (it.hasNext()) {
		    	rep = doIdentifier + "/" + bDefPID + "/" + (String)it.next();
		    	m_store.insert(doIdentifier, HAS_REPRESENTATION_URI, rep);
		    }
	    }
	    
	    // for what object types are we going to query
	    

	    //digitalObject.
		//m_store.insert(doIdentifier, HAS_REPRESENTATION_URI, disseminatorIdentifier);
		
		
		//m_store.insert(disseminatorIdentifier, DISSEMINATION_DIRECT_URI, diss.?); // not going against a service, i.e. datastreams (true/false)
		//m_store.insertLiteral(disseminatorIdentifier, DATE_LAST_MODIFIED_URI, getDate(diss.dissCreateDT));
		//m_store.insert(disseminatorIdentifier, DISSEMINATION_MEDIA_TYPE_URI, diss.?);
		//m_store.insertLiteral(disseminatorIdentifier, STATE_URI, diss.dissState); // change to uri #active/#inactive
		//m_store.insert(disseminatorIdentifier, DISSEMINATION_TYPE_URI, diss.?);
		//m_store.insert(disseminatorIdentifier, DISSEMINATION_VOLATILE_URI, diss.?); // redirect, external, based on diss that depends on red/ext (true/false)
		
		//m_store.insert("info:fedora/" + diss.bMechID, IMPLEMENTS_BDEF_URI, "info:fedora/" + diss.bDefID);
	}

	/* (non-Javadoc)
	 * @see fedora.server.resourceIndex.ResourceIndex#modifyDigitalObject(fedora.server.storage.types.DigitalObject)
	 */
	public void modifyDigitalObject(DigitalObject digitalObject) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see fedora.server.resourceIndex.ResourceIndex#modifyDatastream(fedora.server.storage.types.Datastream)
	 */
	public void modifyDatastream(Datastream ds) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see fedora.server.resourceIndex.ResourceIndex#modifyDissemination(fedora.server.storage.types.Dissemination)
	 */
	public void modifyDisseminator(Disseminator diss) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see fedora.server.resourceIndex.ResourceIndex#deleteDigitalObject(java.lang.String)
	 */
	public void deleteDigitalObject(String pid) {
		// TODO Auto-generated method stub
	}

	/* (non-Javadoc)
	 * @see fedora.server.resourceIndex.ResourceIndex#deleteDatastream(fedora.server.storage.types.Datastream)
	 */
	public void deleteDatastream(Datastream ds) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see fedora.server.resourceIndex.ResourceIndex#deleteDissemination(fedora.server.storage.types.Dissemination)
	 */
	public void deleteDisseminator(Disseminator diss) {
		// TODO Auto-generated method stub
		
	}

	private void addBDef(DigitalObject bDef) throws ResourceIndexException {
		String doURI = getDOURI(bDef);
		m_store.insert(doURI, RDF_TYPE_URI, BDEF_RDF_TYPE_URI);
		
		Datastream ds = getLatestDatastream(bDef.datastreams("METHODMAP"));
		MethodDef[] mdef = getMethodDefs(bDef.getPid(), ds);
		for (int i = 0; i < mdef.length; i++) {
	        m_store.insertLiteral(doURI, DEFINES_METHOD_URI, mdef[i].methodName);
	        // m_store.insertLiteral(doIdentifier, "foo:methodLabel", mdef[i].methodLabel);
	    }
	}
	
	private void addBMech(DigitalObject bMech) throws ResourceIndexException {
		String doURI = getDOURI(bMech);
		m_store.insert(doURI, RDF_TYPE_URI, BMECH_RDF_TYPE_URI);
		
		Datastream ds;
		ds = getLatestDatastream(bMech.datastreams("DSINPUTSPEC"));
		BMechDSBindSpec dsBindSpec = getDSBindSpec(bMech.getPid(), ds);
		String bDefPID = dsBindSpec.bDefPID;
		m_store.insert(doURI, IMPLEMENTS_BDEF_URI, getDOURI(bDefPID));
		
	}
	
	private void addDataObject(DigitalObject digitalObject) {
		String identifier = getDOURI(digitalObject);
		m_store.insert(identifier, RDF_TYPE_URI, DATA_OBJECT_RDF_TYPE_URI);
		
//		Datastream ds = getLatestDatastream(digitalObject.datastreams("METHODMAP"));
//		MethodDef[] mdef = getMethodDefs(digitalObject.getPid(), ds);
//		List methodNames = new ArrayList();
//		for (int i = 0; i < mdef.length; i++) {
//	        methodNames.add(mdef[i].methodName);
//	    }
		
		
	}
	
	private void addDublinCoreDatastream(DigitalObject digitalObject, Datastream ds) throws ResourceIndexException {
	    String doURI = getDOURI(digitalObject);
	    DatastreamXMLMetadata dc = (DatastreamXMLMetadata)ds;
		DCFields dcf;
		try {
			dcf = new DCFields(dc.getContentStream());
		} catch (Throwable t) {
			throw new ResourceIndexException(t.getMessage());
		}
		Iterator it;
		it = dcf.titles().iterator();
		while (it.hasNext()) {
			m_store.insertLiteral(doURI, DC_URI_PREFIX + "title", (String)it.next());
		}
		it = dcf.creators().iterator();
		while (it.hasNext()) {
			m_store.insertLiteral(doURI, DC_URI_PREFIX + "creator", (String)it.next());
		}
		it = dcf.subjects().iterator();
		while (it.hasNext()) {
			m_store.insertLiteral(doURI, DC_URI_PREFIX + "subject", (String)it.next());
		}
		it = dcf.descriptions().iterator();
		while (it.hasNext()) {
			m_store.insertLiteral(doURI, DC_URI_PREFIX + "description", (String)it.next());
		}
		it = dcf.publishers().iterator();
		while (it.hasNext()) {
			m_store.insertLiteral(doURI, DC_URI_PREFIX + "publisher", (String)it.next());
		}
		it = dcf.contributors().iterator();
		while (it.hasNext()) {
			m_store.insertLiteral(doURI, DC_URI_PREFIX + "contributor", (String)it.next());
		}
		it = dcf.dates().iterator();
		while (it.hasNext()) {
			m_store.insertLiteral(doURI, DC_URI_PREFIX + "date", (String)it.next());
		}
		it = dcf.types().iterator();
		while (it.hasNext()) {
			m_store.insertLiteral(doURI, DC_URI_PREFIX + "type", (String)it.next());
		}
		it = dcf.formats().iterator();
		while (it.hasNext()) {
			m_store.insertLiteral(doURI, DC_URI_PREFIX + "format", (String)it.next());
		}
		it = dcf.identifiers().iterator();
		while (it.hasNext()) {
			m_store.insertLiteral(doURI, DC_URI_PREFIX + "identifier", (String)it.next());
		}
		it = dcf.sources().iterator();
		while (it.hasNext()) {
			m_store.insertLiteral(doURI, DC_URI_PREFIX + "source", (String)it.next());
		}
		it = dcf.languages().iterator();
		while (it.hasNext()) {
			m_store.insertLiteral(doURI, DC_URI_PREFIX + "language", (String)it.next());
		}
		it = dcf.relations().iterator();
		while (it.hasNext()) {
			m_store.insertLiteral(doURI, DC_URI_PREFIX + "relation", (String)it.next());
		}
		it = dcf.coverages().iterator();
		while (it.hasNext()) {
			m_store.insertLiteral(doURI, DC_URI_PREFIX + "coverage", (String)it.next());
		}
		it = dcf.rights().iterator();
		while (it.hasNext()) {
			m_store.insertLiteral(doURI, DC_URI_PREFIX + "rights", (String)it.next());
		}
	}
	
	private void addRelsDatastream(Datastream ds) {
    	// TODO ristore can take the rdfxml straight... need test case for this method
	    DatastreamXMLMetadata rels = (DatastreamXMLMetadata)ds;
	    m_store.read(rels.getContentStream(), "");
	    
	}
	
	private void addMethodMapDatastream(DigitalObject digitalObject, Datastream ds) throws ResourceIndexException {
	    // only bdefs & bmechs have mmaps, and we only add when we see bdefs.
	    if (digitalObject.getFedoraObjectType() != DigitalObject.FEDORA_BDEF_OBJECT) {
	        return;
	    }

        String doURI = getDOURI(digitalObject);
	    DatastreamXMLMetadata mmapDS = (DatastreamXMLMetadata)ds;
	    ServiceMapper serviceMapper = new ServiceMapper(digitalObject.getPid());
	    MethodDef[] mdef;
	    try {
	        mdef = serviceMapper.getMethodDefs(new InputSource(new ByteArrayInputStream(mmapDS.xmlContent)));
	    } catch (Throwable t) {
	        throw new ResourceIndexException(t.getMessage());
	    }
	    
	    // we only track
	    // 1. unparameterized disseminations
	    // 2. disseminations with fixed parameters
	    String methodName;
	    boolean noRequiredParms;
	    for (int i = 0; i < mdef.length; i++) {
	    	methodName = mdef[i].methodName;
	    	MethodParmDef[] mparms = mdef[i].methodParms;
	    	if (mparms.length == 0) { // no method parameters
	    		m_store.insertLiteral(doURI, DEFINES_METHOD_URI, methodName);
	    	} else {
	    	    Arrays.sort(mparms, new MethodParmDefParmNameComparator());
	    		noRequiredParms = true;
	    		for (int j = 0; j < mparms.length; j++) {
	    			if (noRequiredParms && mparms[j].parmRequired) {
	    			    noRequiredParms = false;
    			    }
	    		}
	    		if (noRequiredParms) {
	    			m_store.insertLiteral(doURI, DEFINES_METHOD_URI, methodName);
	    		} else {
	    		    // FIXME need to get methods with fixed parameters
	    		    m_store.insertLiteral(doURI, DEFINES_METHOD_URI, mdef[i].methodName);
	    		    
	    		    
	    		    
	    		}
	    		
	    	}
	    	
	    	
	    	// FIXME just for testing
	        // get method parms
	        for (int j = 0; j < mparms.length; j++) {
	            System.out.println(methodName + " *parmName: " + mparms[j].parmName);
	            System.out.println(methodName + " *parmPassBy: " + mparms[j].parmPassBy);
	            System.out.println(methodName + " *parmType: " + mparms[j].parmType);
	            System.out.print(methodName + " *parmDomainValues: ");
	            for (int k = 0; k < mparms[j].parmDomainValues.length; k++) {
	            	System.out.print(mparms[j].parmDomainValues[k]);
	            	if (k < mparms[j].parmDomainValues.length -1) {
	            		System.out.print(", ");
	            	}
	            }
	            System.out.print("\n");
	            System.out.println(methodName + " *parmRequired: " + mparms[j].parmRequired);
	            System.out.println(methodName + " *parmDefaultValue: " + mparms[j].parmDefaultValue);
	        }
	    }
		
	}
	
	private Datastream getLatestDatastream(List datastreams) {
	    Iterator it = datastreams.iterator();
	    long latestDSCreateDT = -1;
	    Datastream ds, latestDS = null;
	    while (it.hasNext()) {
	        ds = (Datastream)it.next();
	        if (ds.DSCreateDT.getTime() > latestDSCreateDT) {
	            latestDS = ds;
	        }
	    }
	    return latestDS;
	}
	
	private MethodDef[] getMethodDefs(String pid, Datastream ds) throws ResourceIndexException {
	    DatastreamXMLMetadata mmapDS = (DatastreamXMLMetadata)ds;
	    ServiceMapper serviceMapper = new ServiceMapper(pid);
	    try {
	        return serviceMapper.getMethodDefs(new InputSource(new ByteArrayInputStream(mmapDS.xmlContent)));
	    } catch (Throwable t) {
	        throw new ResourceIndexException(t.getMessage());
	    }
	}
	
	private List getMethodNames(String bdefPID) throws ResourceIndexException {
		RDQLQuery query = new RDQLQuery("SELECT ?o WHERE (<" + getDOURI(bdefPID) + "> <" + DEFINES_METHOD_URI + "> ?o)");
		
		System.out.println("*** query: " + query.getQuery());
		JenaResultIterator results = (JenaResultIterator)executeQuery(query);
		
		Value v;
		List methods = new ArrayList();
		while (results.hasNext()) {
			v = (Value)results.next().get("o");
			methods.add(v.getString());
			System.out.println("*** " + v.getString());
		}
		return methods;
	}
	
	private BMechDSBindSpec getDSBindSpec(String pid, Datastream ds) throws ResourceIndexException {
	    DatastreamXMLMetadata dsInSpecDS = (DatastreamXMLMetadata)ds;
	    ServiceMapper serviceMapper = new ServiceMapper(pid);
	    BMechDSBindSpec dsBindSpec;
	    try {
	        return serviceMapper.getDSInputSpec(new InputSource(new ByteArrayInputStream(dsInSpecDS.xmlContent)));
	    } catch (Throwable t) {
	        throw new ResourceIndexException(t.getMessage());
	    }
	}
	
	private Disseminator getLatestDisseminator(List disseminators) {
	    Iterator it = disseminators.iterator();
	    long latestDISSCreateDT = -1;
	    Disseminator diss, latestDISS = null;
	    while (it.hasNext()) {
	        diss = (Disseminator)it.next();
	        if (diss.dissCreateDT.getTime() > latestDISSCreateDT) {
	            latestDISS = diss;
	        }
	    }
	    return latestDISS;
	}
	
	private String getDOURI(DigitalObject digitalObject) {
	    String identifier;
	    logFinest("ResourceIndex digitalObject.getPid(): " + digitalObject.getPid());
	    if (digitalObject.getURI() != null && !digitalObject.getURI().equals("")) {
	        return digitalObject.getURI();
	    } else {
	        return getDOURI(digitalObject.getPid());
	    }
	}
	
	private String getDOURI(String pid) {
	    return FEDORA_URI_SCHEME + pid;
	}
	
	/**
	 * 
	 * @param date
	 * @return UTC Date as ISO 8601 formatted string (e.g. 2004-04-20T16:20:00Z)
	 */
	private static String getDate(Date date) {
	    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	    df.setTimeZone(TimeZone.getTimeZone("GMT"));
	    return df.format(date);
	}

    /* (non-Javadoc)
     * @see fedora.server.resourceIndex.ResourceIndex#getIndexLevel()
     */
    public int getIndexLevel() {
        return m_indexLevel;
    }
	
	/*
	Notes:

	called from out of DefaultDOManager.doCommit()

	*/
    
    /**
     * 
     * Case insensitive sort by parameter name
     */
    protected class MethodParmDefParmNameComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            MethodParmDef p1 = (MethodParmDef)o1;
            MethodParmDef p2 = (MethodParmDef)o2;
            return p1.parmName.toUpperCase().compareTo(p2.parmName.toUpperCase());
        }
        
    }
    
}
