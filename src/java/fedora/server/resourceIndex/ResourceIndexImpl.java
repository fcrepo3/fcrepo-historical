package fedora.server.resourceIndex;

import fedora.server.Logging;
import fedora.server.StdoutLogging;
import fedora.server.errors.ResourceIndexException;
import fedora.server.storage.ConnectionPool;
import fedora.server.storage.service.ServiceMapper;
import fedora.server.storage.types.*;
import fedora.server.utilities.DCFields;

import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.mime.MIMEContent;
import javax.wsdl.factory.*;
import javax.wsdl.xml.*;
import javax.wsdl.*;
import javax.xml.namespace.QName;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.jrdf.graph.ObjectNode;
import org.jrdf.graph.PredicateNode;
import org.jrdf.graph.SubjectNode;

import org.openrdf.rio.Parser;
import org.openrdf.rio.rdfxml.RdfXmlParser;

import org.trippi.RDFFormat;
import org.trippi.TripleMaker;
import org.trippi.TripleIterator;
import org.trippi.TriplestoreConnector;
import org.trippi.TriplestoreReader;
import org.trippi.TriplestoreWriter;
import org.trippi.TrippiException;
import org.trippi.TupleIterator;
import org.trippi.io.RIOTripleIterator;

import org.xml.sax.InputSource;

/**
 * Implementation of the ResourceIndex interface.
 * 
 * @author Edwin Shin
 *
 */
public class ResourceIndexImpl extends StdoutLogging implements ResourceIndex {
    // TODO handling different indexing levels
    // 		- initial index
    //		- latent indexing: "tagging" objects with current index level to support querying what needs indexing later
    //		- subsequent insert/edits/deletes
    //		- changes in levels
    //		- distinct levels or discrete mix & match (e.g., combinations of DC, REP & REP-DEP, RELS, etc.)
    
    private int m_indexLevel;
    
    // For the database
    private ConnectionPool m_cPool;
    private Connection m_conn;
    private Statement m_statement;
    private ResultSet m_resultSet;
    
    // Triplestore (Trippi)
    private TriplestoreConnector m_connector;
    private TriplestoreReader m_reader;
    private TriplestoreWriter m_writer;
    private List m_tQueue;
    
    // RDF Prefix and Namespaces
    private Map namespaces; 
    
    public ResourceIndexImpl(int indexLevel, 
                             TriplestoreConnector connector, 
                             ConnectionPool cPool, 
                             Logging target) throws ResourceIndexException {
        super(target);
        m_indexLevel = indexLevel;
        namespaces = new HashMap();
        namespaces.put("dc", NS_DC);
        namespaces.put("fedora", NS_FEDORA);
        namespaces.put("fedora-ont", NS_FEDORA_MODEL);
        namespaces.put("rdf", NS_RDF);
        namespaces.put("xml-schema", NS_XML_SCHEMA);
        m_connector = connector;
        m_reader = m_connector.getReader();
        m_writer = m_connector.getWriter();
        try {
            m_reader.setAliasMap(namespaces);
            m_writer.setAliasMap(namespaces);
        } catch (TrippiException e) {
            throw new ResourceIndexException(e.getMessage(), e);
        }
        m_tQueue = new ArrayList();
        m_cPool = cPool;
        try {
            m_conn = m_cPool.getConnection();
        } catch (SQLException e) {
            throw new ResourceIndexException("ResourceIndex Connection Pool " +
                                             "was unable to get a connection", e);
        } finally {
            m_cPool.free(m_conn);
        }
    }

    /* (non-Javadoc)
     * @see fedora.server.resourceIndex.ResourceIndex#getIndexLevel()
     * 
     */
    public int getIndexLevel() {
        return m_indexLevel;
    }

	/* (non-Javadoc)
	 * @see fedora.server.resourceIndex.ResourceIndex#addDigitalObject(fedora.server.storage.types.DigitalObject)
	 */
	public void addDigitalObject(DigitalObject digitalObject) throws ResourceIndexException {
	    if (m_indexLevel == INDEX_LEVEL_OFF) {
	        return;
        }
        
        String pid = digitalObject.getPid();
		String doIdentifier = getDOURI(digitalObject);
		
		// Insert basic system metadata
        queuePlainLiteralTriple(doIdentifier, MODEL_LABEL, digitalObject.getLabel());
        queuePlainLiteralTriple(doIdentifier, MODEL_DATE_CREATED, getDate(digitalObject.getCreateDate()));
        queueTypedLiteralTriple(doIdentifier, MODEL_DATE_MODIFIED, getDate(digitalObject.getLastModDate()), XML_DATE);
		
		if (digitalObject.getOwnerId() != null) {
		    queuePlainLiteralTriple(doIdentifier, MODEL_OWNER, digitalObject.getOwnerId());
		}
		queuePlainLiteralTriple(doIdentifier, MODEL_CMODEL, digitalObject.getContentModelId());
		queueTriple(doIdentifier, MODEL_STATE, getStateURI(digitalObject.getState()));
		
        // Insert ExtProperties
        Map extProps = digitalObject.getExtProperties();
        Iterator epIt = extProps.keySet().iterator();
        String epKey;
        while (epIt.hasNext()) {
            epKey = (String)epIt.next();
            queuePlainLiteralTriple(doIdentifier, epKey, (String)extProps.get(epKey));
        }
        
        addQueue(false);

		// handle type specific duties
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
	}

	protected void addDatastream(DigitalObject digitalObject, String datastreamID) throws ResourceIndexException {
	    if (m_indexLevel == INDEX_LEVEL_OFF) {
            return;
        }
        
        Datastream ds = getLatestDatastream(digitalObject.datastreams(datastreamID));
	    String doURI = getDOURI(digitalObject);
	    String datastreamURI;
	    if (ds.DatastreamURI != null && !ds.DatastreamURI.equals("")) {
	        datastreamURI = ds.DatastreamURI;
	    } else {
	        datastreamURI = doURI + "/" + datastreamID;
	    }
        
        // Alternate IDs
        String[] altIDs = ds.DatastreamAltIDs;
        for (int i = 0; i < altIDs.length; i++) {
            queuePlainLiteralTriple(datastreamURI, MODEL_ALT_ID, altIDs[i]);
        }
        
        // TODO not needed till we do dependency analysis
        // Volatile Datastreams: False for datastreams that are locally managed 
        // (have a control group "M" or "I").
        //String isVolatile = !(ds.DSControlGrp.equals("M") || ds.DSControlGrp.equals("I")) ? "true" : "false";
        //queuePlainLiteralTriple(datastreamURI, REP_DIRECT, "true");
        //queuePlainLiteralTriple(datastreamURI, REP_VOLATILE, isVolatile);

        queueTriple(doURI, REP_REPRESENTATION, datastreamURI);
        queueTypedLiteralTriple(datastreamURI, MODEL_DATE_MODIFIED, getDate(ds.DSCreateDT), XML_DATE);
        addQueue(false);
        
		// handle special system datastreams: DC, METHODMAP, RELS-EXT
		if (datastreamID.equalsIgnoreCase("DC")) {
			addDublinCoreDatastream(digitalObject, ds);
        } else if (datastreamID.equalsIgnoreCase("DSINPUTSPEC")) { // which objs have this?
            addDSInputSpecDatastream(ds);
        } else if (datastreamID.equalsIgnoreCase("METHODMAP")) { 
            addMethodMapDatastream(digitalObject, ds);
        } else if (datastreamID.equalsIgnoreCase("RELS-EXT")) {
            addRelsDatastream(ds);
        } else if (datastreamID.equalsIgnoreCase("SERVICE-PROFILE")) { 
            addServiceProfileDatastream(ds);
		} else if (datastreamID.equalsIgnoreCase("WSDL")) { 
		    addWSDLDatastream(digitalObject, ds);
		}		
	}

	protected void addDisseminator(DigitalObject digitalObject, String disseminatorID) throws ResourceIndexException {
	    if (m_indexLevel == INDEX_LEVEL_OFF) {
            return;
        }
        
        Disseminator diss = getLatestDisseminator(digitalObject.disseminators(disseminatorID));
	    String doIdentifier = getDOURI(digitalObject);
        String bMechPID = diss.bMechID;
        
        queueTriple(doIdentifier, MODEL_USES_BMECH, getDOURI(bMechPID));
	    String bDefPID = diss.bDefID;
        String dissState = getStateURI(diss.dissState);
	    String dissCreateDT = getDate(diss.dissCreateDT);
        
	    // insert representations
	    if (digitalObject.getFedoraObjectType() == DigitalObject.FEDORA_OBJECT) {
            String query = "SELECT riMethodPermutation.permutation, riMethodMimeType.mimeType " +
                           "FROM riMethodPermutation, riMethodMimeType, riMethodImpl " +
                           "WHERE riMethodPermutation.methodId = riMethodImpl.methodId " +
                           "AND riMethodImpl.methodImplId = riMethodMimeType.methodImplId " +
                           "AND riMethodImpl.bMechPid = '" + bMechPID + "'";
            Statement select = null;
            ResultSet rs = null;
            try {
                 select = m_conn.createStatement();
                 rs = select.executeQuery(query);
                 String permutation, mimeType, rep;
                 while (rs.next()) {
                     permutation = rs.getString("permutation");
                     mimeType = rs.getString("mimeType");
                     rep = doIdentifier + "/" + bDefPID + "/" + permutation;
                     queueTriple(doIdentifier, REP_REPRESENTATION, rep);
                     queueTriple(rep, MODEL_STATE, dissState);
                     queuePlainLiteralTriple(rep, 
                                             REP_MEDIATYPE, 
                                             mimeType);
                     //queuePlainLiteralTriple(rep, 
                     //                        REP_DIRECT, 
                     //                        "false"); 
                     queueTypedLiteralTriple(rep, 
                                             MODEL_DATE_MODIFIED, 
                                             dissCreateDT,
                                             XML_DATE); 
                 }
            } catch (SQLException e) {
                try {
                    if (rs != null) {
                        rs.close();
                    }
                    if (select != null) {
                        select.close();
                    }
                m_cPool.free(m_conn);
                } catch(SQLException e2) {
                    throw new ResourceIndexException(e2.getMessage(), e2);
                } finally {
                    rs = null;
                    select = null;
                }
                throw new ResourceIndexException(e.getMessage(), e);
            }
	    }

	    // TODO
        //m_store.insert(disseminatorIdentifier, MODEL_DISS_TYPE, diss.?);
        //m_store.insert(disseminatorIdentifier, FEDORA_VOLATILE, diss.?); // redirect, external, based on diss that depends on red/ext (true/false)
        
        addQueue(false);
    }

	/* (non-Javadoc)
	 * @see fedora.server.resourceIndex.ResourceIndex#modifyDigitalObject(fedora.server.storage.types.DigitalObject)
	 */
	public void modifyDigitalObject(DigitalObject digitalObject) throws ResourceIndexException {
        if (m_indexLevel == INDEX_LEVEL_OFF) {
            return;
        }

		deleteDigitalObject(digitalObject);
        addDigitalObject(digitalObject);
	}

	protected void modifyDatastream(DigitalObject digitalObject, String datastreamID) throws ResourceIndexException {
        if (m_indexLevel == INDEX_LEVEL_OFF) {
            return;
        }
        
        deleteDatastream(digitalObject, datastreamID);
        addDatastream(digitalObject, datastreamID);
	}

	protected void modifyDisseminator(DigitalObject digitalObject, String disseminatorID) throws ResourceIndexException {
        if (m_indexLevel == INDEX_LEVEL_OFF) {
            return;
        }
        
        deleteDisseminator(digitalObject, disseminatorID);
        addDisseminator(digitalObject, disseminatorID);
	}

	/* (non-Javadoc)
	 * @see fedora.server.resourceIndex.ResourceIndex#deleteDigitalObject(java.lang.String)
	 */
	public void deleteDigitalObject(DigitalObject digitalObject) throws ResourceIndexException {
        if (m_indexLevel == INDEX_LEVEL_OFF) {
            return;
        }
        
        Iterator it;
        it = digitalObject.datastreamIdIterator();
        while (it.hasNext()) {
            deleteDatastream(digitalObject, (String)it.next());
        }
        
        // Add disseminators
        it = digitalObject.disseminatorIdIterator();
        while (it.hasNext()) {
            deleteDisseminator(digitalObject, (String)it.next());          
        }
        
        // Delete all statements where doURI is the subject
        String doURI = getDOURI(digitalObject);
        try {
            m_writer.delete(m_reader.findTriples(TripleMaker.createResource(doURI), null, null, 0), false);
        } catch (IOException e) {
            throw new ResourceIndexException(e.getMessage(), e);
        } catch (TrippiException e) {
            throw new ResourceIndexException(e.getMessage(), e);
        }
	}

	protected void deleteDatastream(DigitalObject digitalObject, String datastreamID) throws ResourceIndexException {
	    if (m_indexLevel == INDEX_LEVEL_OFF) {
            return;
        }
        
        Datastream ds = getLatestDatastream(digitalObject.datastreams(datastreamID));
        String doURI = getDOURI(digitalObject);
        String datastreamURI;
        if (ds.DatastreamURI != null && !ds.DatastreamURI.equals("")) {
            datastreamURI = ds.DatastreamURI;
        } else {
            datastreamURI = doURI + "/" + datastreamID;
        }
        
        // DELETE statements where datastreamURI is subject
        try {
            m_writer.delete(m_reader.findTriples(TripleMaker.createResource(datastreamURI), null, null, 0), false);
        } catch (IOException e) {
            throw new ResourceIndexException(e.getMessage(), e);
        } catch (TrippiException e) {
            throw new ResourceIndexException(e.getMessage(), e);
        }
        
        // handle special system datastreams: DC, METHODMAP, RELS-EXT
        if (datastreamID.equalsIgnoreCase("DC")) {
            deleteDublinCoreDatastream(digitalObject, ds);
        } else if (datastreamID.equalsIgnoreCase("DSINPUTSPEC")) { // which objs have this?
            deleteDSInputSpecDatastream(ds);   
        } else if (datastreamID.equalsIgnoreCase("METHODMAP")) { 
            deleteMethodMapDatastream(digitalObject, ds);
        } else if (datastreamID.equalsIgnoreCase("RELS-EXT")) {
            deleteRelsDatastream(ds);
        } else if (datastreamID.equalsIgnoreCase("SERVICE-PROFILE")) { 
            deleteServiceProfileDatastream(ds);
        } else if (datastreamID.equalsIgnoreCase("WSDL")) { 
            deleteWSDLDatastream(digitalObject, ds);
        }
	}

	protected void deleteDisseminator(DigitalObject digitalObject, String disseminatorID) throws ResourceIndexException {
        if (m_indexLevel == INDEX_LEVEL_OFF) {
            return;
        }
        
        Disseminator diss = getLatestDisseminator(digitalObject.disseminators(disseminatorID));
        String doIdentifier = getDOURI(digitalObject);
        
        String bDefPID = diss.bDefID;
        String bMechPID = diss.bMechID;
        
        // delete bMech reference: 
        try {
            m_writer.delete(m_reader.findTriples(TripleMaker.createResource(doIdentifier), 
                                                 TripleMaker.createResource(MODEL_USES_BMECH), 
                                                 TripleMaker.createResource(getDOURI(bMechPID)), 
                                                 0), 
                            false);
        } catch (IOException e) {
            throw new ResourceIndexException(e.getMessage(), e);
        } catch (TrippiException e) {
            throw new ResourceIndexException(e.getMessage(), e);
        }
        
        if (digitalObject.getFedoraObjectType() == DigitalObject.FEDORA_OBJECT) {
            // delete statement where rep is the subject
            String query = "SELECT permutation " +
                           "FROM riMethodPermutation, riMethodImpl " +
                           "WHERE riMethodPermutation.methodId = riMethodImpl.methodId " +
                           "AND riMethodImpl.bMechPid = '" + bMechPID + "'";
            Statement select = null;
            ResultSet rs = null;
            
            try {
                 select = m_conn.createStatement();
                 rs = select.executeQuery(query);
                 String permutation, rep;
                 while (rs.next()) {
                     permutation = rs.getString("permutation");
                     rep = doIdentifier + "/" + bDefPID + "/" + permutation;
                     m_writer.delete(m_reader.findTriples(TripleMaker.createResource(rep), null, null, 0), false);
                 }
            } catch (SQLException e) {
                try {
                    if (rs != null) {
                        rs.close();
                    }
                    if (select != null) {
                        select.close();
                    }
                m_cPool.free(m_conn);
                } catch(SQLException e2) {
                    throw new ResourceIndexException(e2.getMessage(), e2);
                } finally {
                    rs = null;
                    select = null;
                }
                throw new ResourceIndexException(e.getMessage(), e);
            } catch (IOException e) {
                throw new ResourceIndexException(e.getMessage(), e);
            } catch (TrippiException e) {
                throw new ResourceIndexException(e.getMessage(), e);
            }
        }	
	}
    
    /**
     * 
     * @param out
     * @param format
     * @throws IOException
     * @throws TrippiException
     */
    public void export(OutputStream out, RDFFormat format) throws ResourceIndexException {
        try {
            TripleIterator it = m_reader.findTriples(null, null, null, 0);
            it.setAliasMap(namespaces);
            it.toStream(out, format);
        } catch (TrippiException e) {
            throw new ResourceIndexException(e.getMessage(), e);
        }
    }

	private void addBDef(DigitalObject bDef) throws ResourceIndexException {
		String doURI = getDOURI(bDef);
        queueTriple(doURI, RDF_TYPE, MODEL_BDEF);
		
		Datastream ds = getLatestDatastream(bDef.datastreams("METHODMAP"));
		MethodDef[] mdef = getMethodDefs(bDef.getPid(), ds);
		for (int i = 0; i < mdef.length; i++) {
            queuePlainLiteralTriple(doURI, MODEL_DEFINES_METHOD, mdef[i].methodName);
	        // m_store.insertLiteral(doIdentifier, "foo:methodLabel", mdef[i].methodLabel);
	    }
        addQueue(false);
	}
	
	private void addBMech(DigitalObject bMech) throws ResourceIndexException {
		String doURI = getDOURI(bMech);
        queueTriple(doURI, RDF_TYPE, MODEL_BMECH);
	
		String bDefPid = getBDefPid(bMech);
		queueTriple(doURI, MODEL_IMPLEMENTS, getDOURI(bDefPid));
		addQueue(false);	
	}
	
	private void addDataObject(DigitalObject digitalObject) throws ResourceIndexException {
		String identifier = getDOURI(digitalObject);
        queueTriple(identifier, RDF_TYPE, MODEL_DATAOBJECT);	
        addQueue(false);
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
            queuePlainLiteralTriple(doURI, NS_DC + "title", (String)it.next());
		}
		it = dcf.creators().iterator();
		while (it.hasNext()) {
            queuePlainLiteralTriple(doURI, NS_DC + "creator", (String)it.next());
		}
		it = dcf.subjects().iterator();
		while (it.hasNext()) {
            queuePlainLiteralTriple(doURI, NS_DC + "subject", (String)it.next());
		}
		it = dcf.descriptions().iterator();
		while (it.hasNext()) {
            queuePlainLiteralTriple(doURI, NS_DC + "description", (String)it.next());
		}
		it = dcf.publishers().iterator();
		while (it.hasNext()) {
            queuePlainLiteralTriple(doURI, NS_DC + "publisher", (String)it.next());
		}
		it = dcf.contributors().iterator();
		while (it.hasNext()) {
			queuePlainLiteralTriple(doURI, NS_DC + "contributor", (String)it.next());
		}
		it = dcf.dates().iterator();
		while (it.hasNext()) {
			queuePlainLiteralTriple(doURI, NS_DC + "date", (String)it.next());
		}
		it = dcf.types().iterator();
		while (it.hasNext()) {
			queuePlainLiteralTriple(doURI, NS_DC + "type", (String)it.next());
		}
		it = dcf.formats().iterator();
		while (it.hasNext()) {
			queuePlainLiteralTriple(doURI, NS_DC + "format", (String)it.next());
		}
		it = dcf.identifiers().iterator();
		while (it.hasNext()) {
			queuePlainLiteralTriple(doURI, NS_DC + "identifier", (String)it.next());
		}
		it = dcf.sources().iterator();
		while (it.hasNext()) {
			queuePlainLiteralTriple(doURI, NS_DC + "source", (String)it.next());
		}
		it = dcf.languages().iterator();
		while (it.hasNext()) {
			queuePlainLiteralTriple(doURI, NS_DC + "language", (String)it.next());
		}
		it = dcf.relations().iterator();
		while (it.hasNext()) {
			queuePlainLiteralTriple(doURI, NS_DC + "relation", (String)it.next());
		}
		it = dcf.coverages().iterator();
		while (it.hasNext()) {
			queuePlainLiteralTriple(doURI, NS_DC + "coverage", (String)it.next());
		}
		it = dcf.rights().iterator();
		while (it.hasNext()) {
			queuePlainLiteralTriple(doURI, NS_DC + "rights", (String)it.next());
		}
        addQueue(false);
	}
	
    private void addDSInputSpecDatastream(Datastream ds) {
        // Placeholder
    }
    
    private void addExtPropertiesDatastream(Datastream ds) {
        // Placeholder
    }
	
    /**
     * MethodMap datastream is only required for identifying the various 
     * combinations (permutations) of method names, their parameters and values.
     * However, the MethodMap datastream is only available in the bDef that 
     * defines the methods or the bMech(s) that implement the bDef, but this 
     * information is needed at the ingest of the dataobjects.
     * So, these method permutations are stored in a relational database
     * for performance reasons.
     * 
     * Note that we only track unparameterized disseminations and disseminations
     * with fixed parameters
     * 
     * @param digitalObject
     * @param ds
     * @throws ResourceIndexException
     */
	private void addMethodMapDatastream(DigitalObject digitalObject, Datastream ds) throws ResourceIndexException {
	    // only bdefs & bmechs have mmaps, and we only add when we see bdefs.
	    if (digitalObject.getFedoraObjectType() != DigitalObject.FEDORA_BDEF_OBJECT) {
	        return;
	    }
        
        String doURI = getDOURI(digitalObject);
        String bDefPid = digitalObject.getPid();
	    MethodDef[] mdef = getMethodDefs(bDefPid, ds);
        List permutations = new ArrayList();
	    
	    String methodName;
	    boolean noRequiredParms;
        int optionalParms;
        PreparedStatement insertMethod = null, insertPermutation = null;
        
	    for (int i = 0; i < mdef.length; i++) {
	    	methodName = mdef[i].methodName;
	    	MethodParmDef[] mparms = mdef[i].methodParms;
	    	if (m_indexLevel != INDEX_LEVEL_PERMUTATIONS || mparms.length == 0) { // no method parameters
                permutations.add(methodName);
	    	} else {
	    		noRequiredParms = true;
                optionalParms = 0;
                List parms = new ArrayList();
	    		for (int j = 0; j < mparms.length; j++) {
	    			if (noRequiredParms && mparms[j].parmRequired) {
	    			    noRequiredParms = false;
    			    }
                    if (!mparms[j].parmRequired) {
                        optionalParms++;
                    }
	    		}
	    		if (noRequiredParms) {
                    permutations.add(methodName);
	    		} else {
	    		    // add methods with their required, fixed parameters
                    parms.addAll(getMethodParameterCombinations(mparms, true));
	    		}
                if (optionalParms > 0) {
                    parms.addAll(getMethodParameterCombinations(mparms, false));
                }
                Iterator it = parms.iterator();
                while (it.hasNext()) {
                    permutations.add(methodName + "?" + it.next());
                }
	    	}
            
            // build the batch of sql statements to execute
            String riMethodPK = getRIMethodPrimaryKey(bDefPid, methodName);
            try {
                insertMethod = m_conn.prepareStatement("INSERT INTO riMethod (methodId, bDefPid, methodName) VALUES (?, ?, ?)");
                insertPermutation = m_conn.prepareStatement("INSERT INTO riMethodPermutation (methodId, permutation) VALUES (?, ?)");
                insertMethod.setString(1, riMethodPK);
                insertMethod.setString(2, bDefPid);
                insertMethod.setString(3, methodName);
                insertMethod.addBatch();

                Iterator it = permutations.iterator();
                while (it.hasNext()) {
                    insertPermutation.setString(1, riMethodPK);
                    insertPermutation.setString(2, (String)it.next());
                    insertPermutation.addBatch();
                }
                permutations.clear();
            
            } catch (SQLException e) {
                try {
                    if (insertMethod != null) {
                        insertMethod.close();
                    }
                    if (insertPermutation != null) {
                        insertPermutation.close();
                    }
                m_cPool.free(m_conn);
                } catch(SQLException e2) {
                    throw new ResourceIndexException(e2.getMessage(), e2);
                } finally {
                    insertMethod = null;
                    insertPermutation = null;
                }
                throw new ResourceIndexException(e.getMessage(), e);
            }

	    	// FIXME do we need passby and type in the graph?
//	        for (int j = 0; j < mparms.length; j++) {
//	            System.out.println(methodName + " *parmName: " + mparms[j].parmName);
//	            System.out.println(methodName + " *parmPassBy: " + mparms[j].parmPassBy);
//	            System.out.println(methodName + " *parmType: " + mparms[j].parmType);
//	        }
	    }
        
        try {
            insertMethod.executeBatch();
            insertPermutation.executeBatch();
        } catch (SQLException e) {
            try {
                if (insertMethod != null) {
                    insertMethod.close();
                }
                if (insertPermutation != null) {
                    insertPermutation.close();
                }
            m_cPool.free(m_conn);
            } catch(SQLException e2) {
                throw new ResourceIndexException(e2.getMessage(), e2);
            } finally {
                insertMethod = null;
                insertPermutation = null;
            }
            throw new ResourceIndexException(e.getMessage(), e);
        }
	}
	
    private void addRelsDatastream(Datastream ds) throws ResourceIndexException {
        DatastreamXMLMetadata rels = (DatastreamXMLMetadata)ds;
        try {
            m_writer.add(TripleIterator.fromStream(rels.getContentStream(), 
                                                   RDFFormat.RDF_XML),
                                                   false);
        } catch (IOException e) {
            throw new ResourceIndexException(e.getMessage(), e);
        } catch (TrippiException e) {
            throw new ResourceIndexException(e.getMessage(), e);
        }
    }
    
    private void addServiceProfileDatastream(Datastream ds) {
        // Placeholder
    }
    
    /**
     * The WSDL datastream is only parsed to obtain the mime types of
     * disseminations. As this is only available in bMech objects,
     * we cache this information in a relational database table for future
     * queries when data objects are ingested.
     * 
     * @param digitalObject
     * @param ds
     * @throws ResourceIndexException
     */
    private void addWSDLDatastream(DigitalObject digitalObject, Datastream ds) throws ResourceIndexException {
        // for the moment, we're only interested in WSDL Datastreams
        // in BMechs, so that we can extract mimetypes.
        if (digitalObject.getFedoraObjectType() != DigitalObject.FEDORA_BMECH_OBJECT) {
            return;
        }

        String doURI = getDOURI(digitalObject);
        String bDefPid = getBDefPid(digitalObject);
        String bMechPid = digitalObject.getPid();
        DatastreamXMLMetadata wsdlDS = (DatastreamXMLMetadata)ds;
        Map bindings;
        PreparedStatement insertMethodImpl, insertMethodMimeType;
        
        try {
            WSDLFactory wsdlFactory = WSDLFactory.newInstance();
            WSDLReader wsdlReader = wsdlFactory.newWSDLReader();
            wsdlReader.setFeature("javax.wsdl.verbose",false);
            
            Definition definition = wsdlReader.readWSDL(null, new InputSource(new ByteArrayInputStream(wsdlDS.xmlContent)));
            bindings = definition.getBindings();
            
            Set bindingKeys = bindings.keySet();
            Iterator it = bindingKeys.iterator();
            
            String methodName, mimeType;
            insertMethodImpl = m_conn.prepareStatement("INSERT INTO riMethodImpl (methodImplId, bMechPid, methodId) VALUES (?, ?, ?)");
            insertMethodMimeType = m_conn.prepareStatement("INSERT INTO riMethodMimeType (methodImplId, mimeType) VALUES (?, ?)");
            String riMethodImplPK, riMethodFK;
            QName mimeContentQName = new QName("http://schemas.xmlsoap.org/wsdl/mime/", "content");
            while (it.hasNext()) {
                QName qname = (QName)it.next();
                Binding binding = (Binding)bindings.get(qname);

                List bops = binding.getBindingOperations();
                Iterator bit = bops.iterator();
                while (bit.hasNext()) {
                    BindingOperation bop = (BindingOperation)bit.next();
                    methodName = bop.getName();
                    riMethodFK = getRIMethodPrimaryKey(bDefPid, methodName);
                    riMethodImplPK = getRIMethodImplPrimaryKey(bMechPid, methodName);
                    insertMethodImpl.setString(1, riMethodImplPK);
                    insertMethodImpl.setString(2, bMechPid);
                    insertMethodImpl.setString(3, riMethodFK);
                    insertMethodImpl.addBatch();
                    BindingOutput bout = bop.getBindingOutput();
                    List extEls = bout.getExtensibilityElements();
                    Iterator eit = extEls.iterator();
                    while (eit.hasNext()) {
                        ExtensibilityElement extEl = (ExtensibilityElement)eit.next();
                        QName eType = extEl.getElementType();
                        if (eType.equals(mimeContentQName)) {
                            MIMEContent mc = (MIMEContent)extEl;
                            mimeType = mc.getType();
                            insertMethodMimeType.setString(1, riMethodImplPK);
                            insertMethodMimeType.setString(2, mimeType);
                            insertMethodMimeType.addBatch();
                        }
                    }
                }
            }
            
            try {
                insertMethodImpl.executeBatch();
                insertMethodMimeType.executeBatch();
            } catch (SQLException e) {
                try {
                    if (insertMethodImpl != null) {
                        insertMethodImpl.close();
                    }
                    if (insertMethodMimeType != null) {
                        insertMethodMimeType.close();
                    }
                m_cPool.free(m_conn);
                } catch(SQLException e2) {
                    throw new ResourceIndexException(e2.getMessage(), e2);
                } finally {
                    insertMethodImpl = null;
                    insertMethodMimeType = null;
                }
                throw new ResourceIndexException(e.getMessage(), e);
            }
            
        } catch (WSDLException e) {
            throw new ResourceIndexException(e.getMessage(), e);
        } catch (SQLException e) {
            throw new ResourceIndexException(e.getMessage(), e);
        } finally {
            m_cPool.free(m_conn);
        }
    }
    
    private void deleteDublinCoreDatastream(DigitalObject digitalObject, Datastream ds) {
        // So long as deletes are always initiated at the object level
        // (i.e., deleteDigitalObject(DigitalObject do), we don't actually
        // need to handle anything here.
    }
    
    private void deleteDSInputSpecDatastream(Datastream ds) {
        // placeholder
    }

    private void deleteMethodMapDatastream(DigitalObject digitalObject, Datastream ds) throws ResourceIndexException {
        if (digitalObject.getFedoraObjectType() != DigitalObject.FEDORA_BDEF_OBJECT) {
            return;
        }
        
        String bDefPid = digitalObject.getPid();
        String delete = "DELETE riMethod.*, riMethodPermutation.* " +
                        "FROM riMethod, riMethodPermutation " +
                        "WHERE (riMethod.methodId = riMethodPermutation.methodId) " +
                        "AND riMethod.bDefPid = '" + bDefPid + "'";
        Statement stmt = null;
        try {
            stmt = m_conn.createStatement();
            stmt.execute(delete);
        } catch (SQLException e) {
            try {
                if (stmt != null) {
                    stmt.close();
                }
            m_cPool.free(m_conn);
            } catch(SQLException e2) {
                throw new ResourceIndexException(e2.getMessage(), e2);
            } finally {
                stmt = null;
            }
            throw new ResourceIndexException(e.getMessage(), e);
        }
    }

    private void deleteRelsDatastream(Datastream ds) throws ResourceIndexException {
        // So long as deletes are always initiated at the object level
        // (i.e., deleteDigitalObject(DigitalObject do), we don't actually
        // need to handle anything here.
        
//        // FIXME this is not complete
//        DatastreamXMLMetadata rels = (DatastreamXMLMetadata)ds;
//        // Use the SAX2-compliant Xerces parser:
//        System.setProperty(
//                "org.xml.sax.driver",
//                "org.apache.xerces.parsers.SAXParser");
//        Parser parser = new RdfXmlParser();
//        try {
//            TripleIterator it = new RIOTripleIterator(rels.getContentStream(), parser, "http://www.example.org/");
//            m_writer.delete(it, false);
//        } catch (TrippiException e) {
//            throw new ResourceIndexException(e.getMessage(), e);
//        } catch (IOException e) {
//            throw new ResourceIndexException(e.getMessage(), e);
//        }
    }

    private void deleteServiceProfileDatastream(Datastream ds) {
        // placeholder
    }

    private void deleteWSDLDatastream(DigitalObject digitalObject, Datastream ds) throws ResourceIndexException {
        if (digitalObject.getFedoraObjectType() != DigitalObject.FEDORA_BMECH_OBJECT) {
            return;
        }
        
        String bMechPid = digitalObject.getPid();
        String delete = "DELETE riMethodImpl.*, riMethodMimeType.* " +
                        "FROM riMethodImpl, riMethodMimeType " +
                        "WHERE (riMethodImpl.methodImplId = riMethodMimeType.methodImplId) " +
                        "AND riMethodImpl.bMechPid = '" + bMechPid + "'";
        Statement stmt = null;
        try {
            stmt = m_conn.createStatement();
            stmt.execute(delete);
        } catch (SQLException e) {
            try {
                if (stmt != null) {
                    stmt.close();
                }
            m_cPool.free(m_conn);
            } catch(SQLException e2) {
                throw new ResourceIndexException(e2.getMessage(), e2);
            } finally {
                stmt = null;
            }
            throw new ResourceIndexException(e.getMessage(), e);
        }
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
        return NS_FEDORA + pid;
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
	
	private MethodDef[] getMethodDefs(String pid, Datastream ds) throws ResourceIndexException {
	    DatastreamXMLMetadata mmapDS = (DatastreamXMLMetadata)ds;
	    ServiceMapper serviceMapper = new ServiceMapper(pid);
	    try {
	        return serviceMapper.getMethodDefs(new InputSource(new ByteArrayInputStream(mmapDS.xmlContent)));
	    } catch (Throwable t) {
	        throw new ResourceIndexException(t.getMessage());
	    }
	}
    
    private String getStateURI(String state) {
        return state.equalsIgnoreCase("A") ? MODEL_ACTIVE : MODEL_INACTIVE;
    }

    private void queueTriple(String subject, 
                             String predicate, 
                             String object) throws ResourceIndexException {
        try {
            m_tQueue.add(TripleMaker.create(subject, predicate, object));
        } catch (TrippiException e) {
            throw new ResourceIndexException(e.getMessage(), e);
        }
    }
    
    private void queuePlainLiteralTriple(String subject, 
                                         String predicate, 
                                         String object) throws ResourceIndexException {
        try {
            if (object == null) {
                object = "";
            }
            m_tQueue.add(TripleMaker.createPlain(subject, predicate, object));
        } catch (TrippiException e) {
            throw new ResourceIndexException(e.getMessage(), e);
        }
    }
    
    private void queueLocalLiteralTriple(String subject, 
                                         String predicate, 
                                         String object, 
                                         String language) throws ResourceIndexException {
        try {
            if (object == null) {
                object = "";
            }
            m_tQueue.add(TripleMaker.createLocal(subject, predicate, object, language));
        } catch (TrippiException e) {
            throw new ResourceIndexException(e.getMessage(), e);
        }
    }
    
    private void queueTypedLiteralTriple(String subject, 
                                         String predicate, 
                                         String object, 
                                         String datatype) throws ResourceIndexException {
        try {
            if (object == null) {
                object = "";
            }
            m_tQueue.add(TripleMaker.createTyped(subject, predicate, object, datatype));
        } catch (TrippiException e) {
            throw new ResourceIndexException(e.getMessage(), e);
        }
}
    
    private void addQueue(boolean flush) throws ResourceIndexException {
        try {
            m_writer.add(m_tQueue, flush);
        } catch (IOException e) {
            throw new ResourceIndexException(e.getMessage(), e);
        } catch (TrippiException e) {
            throw new ResourceIndexException(e.getMessage(), e);
        }
        m_tQueue.clear();
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
    
    /**
     * Returns a List of Strings, representing the cross product of possible
     * method parameters and their values, e.g. 
     * ( "arg1=val1&arg2=val2", "foo=bar&baz=quux" )
     * 
     */
    private List getMethodParameterCombinations(MethodParmDef[] mparms, boolean isRequired) {
        List combinations = new ArrayList();
        
        Arrays.sort(mparms, new MethodParmDefParmNameComparator());
        List parms = new ArrayList();
        for (int j = 0; j < mparms.length; j++) {
            List parm = new ArrayList();
            for (int k = 0; k < mparms[j].parmDomainValues.length; k++) {
                if (isRequired) {
                    if (mparms[j].parmRequired) {
                        parm.add(mparms[j].parmName + "=" + mparms[j].parmDomainValues[k]);
                    }
                } else {
                    parm.add(mparms[j].parmName + "=" + mparms[j].parmDomainValues[k]);
                }
            }
            parms.add(parm);
        }
        
        CrossProduct cp = new CrossProduct(parms);
        List results = cp.getCrossProduct();
        Iterator it = results.iterator();
        while (it.hasNext()) {
            List cpParms = (List)it.next();
            Iterator it2 = cpParms.iterator();
            StringBuffer sb = new StringBuffer();
            while (it2.hasNext()) {
                sb.append(it2.next());
                if (it2.hasNext()) {
                    sb.append("&");
                }
            }
            combinations.add(sb.toString());
        }
        return combinations;
    }
    
    /**
     * If we could rely on support for JDBC 3.0's Statement.getGeneratedKeys(),
     * we could use an auto-increment field for the primary key.
     * 
     * A composite primary key isn't used (at the moment) because
     * of varying support for composite keys.
     * A post to the McKoi mailing list suggests that while McKoi
     * allows composite primary keys, it's not indexing them as a 
     * composite, and requiring a (full?) table scan.
     * 
     * @return bDefPid + "/" + methodName, e.g. demo:8/getImage
     */
    private String getRIMethodPrimaryKey(String bDefPid, String methodName) {
        return (bDefPid + "/" + methodName);
    }
    
    private String getRIMethodImplPrimaryKey(String bMechPid, String methodName) {
        return (bMechPid + "/" + methodName);
    }
    
    private String getBDefPid(DigitalObject bMech) throws ResourceIndexException {
        if (bMech.getFedoraObjectType() != DigitalObject.FEDORA_BMECH_OBJECT) {
            throw new ResourceIndexException("Illegal argument: object is not a bMech");
        }
        Datastream ds;
        ds = getLatestDatastream(bMech.datastreams("DSINPUTSPEC"));
        BMechDSBindSpec dsBindSpec = getDSBindSpec(bMech.getPid(), ds);
        return dsBindSpec.bDefPID;
    }

    /* (non-Javadoc)
     * @see org.trippi.TriplestoreReader#setAliasMap(java.util.Map)
     */
    public void setAliasMap(Map aliasToPrefix) throws TrippiException {
        m_reader.setAliasMap(aliasToPrefix);
    }

    /* (non-Javadoc)
     * @see org.trippi.TriplestoreReader#getAliasMap()
     */
    public Map getAliasMap() throws TrippiException {
        return m_reader.getAliasMap();
    }

    /* (non-Javadoc)
     * @see org.trippi.TriplestoreReader#findTuples(java.lang.String, java.lang.String, int, boolean)
     */
    public TupleIterator findTuples(String queryLang,
                                    String tupleQuery,
                                    int limit,
                                    boolean distinct) throws TrippiException {
        return m_reader.findTuples(queryLang, tupleQuery, limit, distinct);
    }

    /* (non-Javadoc)
     * @see org.trippi.TriplestoreReader#countTuples(java.lang.String, java.lang.String, int, boolean)
     */
    public int countTuples(String queryLang,
                           String tupleQuery,
                           int limit,
                           boolean distinct) throws TrippiException {
        return m_reader.countTuples(queryLang, tupleQuery, limit, distinct);
    }

    /* (non-Javadoc)
     * @see org.trippi.TriplestoreReader#findTriples(java.lang.String, java.lang.String, int, boolean)
     */
    public TripleIterator findTriples(String queryLang,
                                      String tripleQuery,
                                      int limit,
                                      boolean distinct) throws TrippiException {
        return m_reader.findTriples(queryLang, tripleQuery, limit, distinct);
    }

    /* (non-Javadoc)
     * @see org.trippi.TriplestoreReader#countTriples(java.lang.String, java.lang.String, int, boolean)
     */
    public int countTriples(String queryLang,
                            String tripleQuery,
                            int limit,
                            boolean distinct) throws TrippiException {
        return m_reader.countTriples(queryLang, tripleQuery, limit, distinct);
    }

    /* (non-Javadoc)
     * @see org.trippi.TriplestoreReader#findTriples(org.jrdf.graph.SubjectNode, org.jrdf.graph.PredicateNode, org.jrdf.graph.ObjectNode, int)
     */
    public TripleIterator findTriples(SubjectNode subject,
                                      PredicateNode predicate,
                                      ObjectNode object,
                                      int limit) throws TrippiException {
        return m_reader.findTriples(subject, predicate, object, limit);
    }

    /* (non-Javadoc)
     * @see org.trippi.TriplestoreReader#countTriples(org.jrdf.graph.SubjectNode, org.jrdf.graph.PredicateNode, org.jrdf.graph.ObjectNode, int)
     */
    public int countTriples(SubjectNode subject,
                            PredicateNode predicate,
                            ObjectNode object,
                            int limit) throws TrippiException {
        return m_reader.countTriples(subject, predicate, object, limit);
    }

    /* (non-Javadoc)
     * @see org.trippi.TriplestoreReader#findTriples(java.lang.String, java.lang.String, java.lang.String, int, boolean)
     */
    public TripleIterator findTriples(String queryLang,
                                      String tripleQuery,
                                      String tripleTemplate,
                                      int limit,
                                      boolean distinct) throws TrippiException {
        return m_reader.findTriples(queryLang, tripleQuery, tripleTemplate, limit, distinct);
    }

    /* (non-Javadoc)
     * @see org.trippi.TriplestoreReader#countTriples(java.lang.String, java.lang.String, java.lang.String, int, boolean)
     */
    public int countTriples(String queryLang,
                            String tripleQuery,
                            String tripleTemplate,
                            int limit,
                            boolean distinct) throws TrippiException {
        return m_reader.countTriples(queryLang, tripleQuery, tripleTemplate, limit, distinct);
    }

    /* (non-Javadoc)
     * @see org.trippi.TriplestoreReader#listTupleLanguages()
     */
    public String[] listTupleLanguages() {
        return m_reader.listTupleLanguages();
    }

    /* (non-Javadoc)
     * @see org.trippi.TriplestoreReader#listTripleLanguages()
     */
    public String[] listTripleLanguages() {
        return m_reader.listTripleLanguages();
    }

    /* (non-Javadoc)
     * @see org.trippi.TriplestoreReader#close()
     */
    public void close() throws TrippiException {
        m_reader.close();
    }
    
    /**
     * Case insensitive sort by parameter name
     */
    protected class MethodParmDefParmNameComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            MethodParmDef p1 = (MethodParmDef)o1;
            MethodParmDef p2 = (MethodParmDef)o2;
            return p1.parmName.toUpperCase().compareTo(p2.parmName.toUpperCase());
        }
    }
    
    protected class CrossProduct {
        public List crossProduct;
        public List lol;
        
        public CrossProduct(List listOfLists) {
            this.lol = listOfLists;
            this.crossProduct = new ArrayList();
        }
        
        public List getCrossProduct() {
            generateCrossProduct(new ArrayList());
            return crossProduct;
        }
        
        private void generateCrossProduct(List productList) {
            if (productList.size() == lol.size()) {
                addCopy(productList);
            } else {
                int idx = productList.size();
                List elementList = (List)lol.get(idx);
                Iterator it = elementList.iterator();
                if (it.hasNext()) {
                    productList.add(it.next());
                    generateCrossProduct(productList);
                    while (it.hasNext()) {
                        productList.set(idx, it.next());
                        generateCrossProduct(productList);
                    }
                    productList.remove(idx);
                }
            }
        }
        
        private void addCopy(List result) {
            List copy = new ArrayList();
            copy.addAll(result);
            crossProduct.add(copy);
        }
    }


}
