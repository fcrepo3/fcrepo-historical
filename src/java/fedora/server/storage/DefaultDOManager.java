package fedora.server.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

import fedora.server.Context;
import fedora.server.Module;
import fedora.server.Server;
import fedora.server.errors.*;
import fedora.server.management.Management;
import fedora.server.management.PIDGenerator;
import fedora.server.resourceIndex.*;
import fedora.server.search.Condition;
import fedora.server.search.FieldSearch;
import fedora.server.search.FieldSearchResult;
import fedora.server.search.FieldSearchQuery;
import fedora.server.storage.lowlevel.FileSystemLowlevelStorage;
import fedora.server.storage.lowlevel.ILowlevelStorage;
import fedora.server.storage.replication.DOReplicator;
import fedora.server.storage.translation.DOTranslator;
import fedora.server.storage.types.BasicDigitalObject;
import fedora.server.storage.types.Datastream;
import fedora.server.storage.types.DatastreamXMLMetadata;
import fedora.server.storage.types.DigitalObject;
import fedora.server.storage.types.Disseminator;
import fedora.server.storage.types.MIMETypedStream;
import fedora.server.storage.translation.DOTranslationUtility;
import fedora.server.utilities.DateUtility;
import fedora.server.utilities.DCFields;
import fedora.server.utilities.SQLUtility;
import fedora.server.utilities.StreamUtility;
import fedora.server.validation.DOValidator;
import fedora.server.validation.DOValidatorImpl;
import fedora.server.validation.RelsExtValidator;

/**
 *
 * <p><b>Title:</b> DefaultDOManager.java</p>
 * <p><b>Description:</b> Manages the reading and writing of digital objects
 * by instantiating an appropriate object reader or writer.  Also, manages the
 * object ingest process and the object replication process.
 * </p>
 *
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2002-2005 by The
 * Rector and Visitors of the University of Virginia and Cornell University.
 * All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 *
 * @author cwilper@cs.cornell.edu
 * @version $Id$
 */
public class DefaultDOManager
        extends Module implements DOManager {

    private String m_pidNamespace;
    private String m_storagePool;
    private String m_defaultStorageFormat;
    private String m_defaultExportFormat;
    private String m_storageCharacterEncoding;
    private PIDGenerator m_pidGenerator;
    private DOTranslator m_translator;
    private ILowlevelStorage m_permanentStore;
    private ILowlevelStorage m_tempStore;
    private DOReplicator m_replicator;
    private DOValidator m_validator;
    private FieldSearch m_fieldSearch;
    private ExternalContentManager m_contentManager;
    private Management m_management;
    private HashSet m_retainPIDs;
    private ResourceIndex m_resourceIndex;

    private ConnectionPool m_connectionPool;
    private Connection m_connection;

    public static String DEFAULT_STATE="L";

    /**
     * Creates a new DefaultDOManager.
     */
    public DefaultDOManager(Map moduleParameters, Server server, String role)
            throws ModuleInitializationException {
        super(moduleParameters, server, role);
    }

    /**
     * Gets initial param values.
     */
    public void initModule()
            throws ModuleInitializationException {
        // pidNamespace (required, 1-17 chars, a-z, A-Z, 0-9 '-')
        m_pidNamespace=getParameter("pidNamespace");
        if (m_pidNamespace==null) {
            throw new ModuleInitializationException(
                    "pidNamespace parameter must be specified.", getRole());
        }
        if ( (m_pidNamespace.length() > 17) || (m_pidNamespace.length() < 1) ) {
            throw new ModuleInitializationException(
                    "pidNamespace parameter must be 1-17 chars long", getRole());
        }
        StringBuffer badChars=new StringBuffer();
        for (int i=0; i<m_pidNamespace.length(); i++) {
            char c=m_pidNamespace.charAt(i);
            boolean invalid=true;
            if (c>='0' && c<='9') {
                invalid=false;
            } else if (c>='a' && c<='z') {
                invalid=false;
            } else if (c>='A' && c<='Z') {
                invalid=false;
            } else if (c=='-') {
                invalid=false;
            }
            if (invalid) {
                badChars.append(c);
            }
        }
        if (badChars.toString().length()>0) {
            throw new ModuleInitializationException("pidNamespace contains "
                    + "invalid character(s) '" + badChars.toString() + "'", getRole());
        }
        // storagePool (optional, default=ConnectionPoolManager's default pool)
        m_storagePool=getParameter("storagePool");
        if (m_storagePool==null) {
            getServer().logConfig("Parameter storagePool "
                + "not given, will defer to ConnectionPoolManager's "
                + "default pool.");
        }
        // internal storage format (required)		
        if (fedora.server.Debug.DEBUG) System.out.println("Server property format.storage= " + Server.STORAGE_FORMAT);
		m_defaultStorageFormat = Server.STORAGE_FORMAT;
        if (m_defaultStorageFormat==null) {
            throw new ModuleInitializationException("System property format.storage "
                + "not given, but it's required.", getRole());
        }
        // default export format (required)
        m_defaultExportFormat=getParameter("defaultExportFormat");
        if (m_defaultExportFormat==null) {
            throw new ModuleInitializationException("Parameter defaultExportFormat "
                + "not given, but it's required.", getRole());
        }
        // storageCharacterEncoding (optional, default=UTF-8)
        m_storageCharacterEncoding=getParameter("storageCharacterEncoding");
        if (m_storageCharacterEncoding==null) {
            getServer().logConfig("Parameter storage_character_encoding "
                + "not given, using UTF-8");
            m_storageCharacterEncoding="UTF-8";
        }
        // retainPIDs (optional, default=demo,test)
        String retainPIDs=null;
        retainPIDs=getParameter("retainPIDs");
        m_retainPIDs=new HashSet();
        retainPIDs=getParameter("retainPIDs");
        if (retainPIDs==null) {
            m_retainPIDs.add("demo");
            m_retainPIDs.add("test");
        } else {
            if (retainPIDs.equals("*")) {
                // when m_retainPIDS is set to null, that means "all"
                m_retainPIDs=null;
            } else {
                // add to list (accept space and/or comma-separated)
                String[] ns=retainPIDs.trim().replaceAll(" +", ",").replaceAll(",+", ",").split(",");
                for (int i=0; i<ns.length; i++) {
                    if (ns[i].length()>0) {
                        m_retainPIDs.add(ns[i]);
                    }
                }
            }
        }
    }

    public void postInitModule()
            throws ModuleInitializationException {
		// get ref to management module
		m_management = (Management) getServer().getModule("fedora.server.management.Management");
		if (m_management==null) {
            throw new ModuleInitializationException(
                    "Management module not loaded.", getRole());
		}
        // get ref to contentmanager module
        m_contentManager = (ExternalContentManager)
          getServer().getModule("fedora.server.storage.ExternalContentManager");
        if (m_contentManager==null) {
            throw new ModuleInitializationException(
                    "ExternalContentManager not loaded.", getRole());
        }
        // get ref to fieldsearch module
        m_fieldSearch=(FieldSearch) getServer().
                getModule("fedora.server.search.FieldSearch");
        // get ref to pidgenerator
        m_pidGenerator=(PIDGenerator) getServer().
                getModule("fedora.server.management.PIDGenerator");
        // note: permanent and temporary storage handles are lazily instantiated

        // get ref to translator and derive storageFormat default if not given
        m_translator=(DOTranslator) getServer().
                getModule("fedora.server.storage.translation.DOTranslator");
        // get ref to replicator
        m_replicator=(DOReplicator) getServer().
                getModule("fedora.server.storage.replication.DOReplicator");
        // get ref to digital object validator
        m_validator=(DOValidator) getServer().
                getModule("fedora.server.validation.DOValidator");
        if (m_validator==null) {
            throw new ModuleInitializationException(
                    "DOValidator not loaded.", getRole());
        }
        // get ref to ResourceIndex
        m_resourceIndex=(ResourceIndex) getServer().
        		getModule("fedora.server.resourceIndex.ResourceIndex");
        if (m_resourceIndex==null) {
            // FIXME should throw exception when not loaded.
            // but until ResourceIndex supports discrete indexing levels
            // we don't force loading of the module for cases where
            // we don't want to use it.
            logFinest("ResourceIndex not loaded");
            //throw new ModuleInitializationException(
            //        "ResourceIndex not loaded", getRole());
        }
        
        // now get the connectionpool
        ConnectionPoolManager cpm=(ConnectionPoolManager) getServer().
                getModule("fedora.server.storage.ConnectionPoolManager");
        if (cpm==null) {
            throw new ModuleInitializationException(
                    "ConnectionPoolManager not loaded.", getRole());
        }
        try {
            if (m_storagePool==null) {
                m_connectionPool=cpm.getPool();
            } else {
                m_connectionPool=cpm.getPool(m_storagePool);
            }
        } catch (ConnectionPoolNotFoundException cpnfe) {
            throw new ModuleInitializationException("Couldn't get required "
                    + "connection pool...wasn't found", getRole());
        }
        try {
            String dbSpec="fedora/server/storage/resources/DefaultDOManager.dbspec";
            InputStream specIn=this.getClass().getClassLoader().
                    getResourceAsStream(dbSpec);
            if (specIn==null) {
                throw new IOException("Cannot find required "
                    + "resource: " + dbSpec);
            }
            SQLUtility.createNonExistingTables(m_connectionPool, specIn, this);
        } catch (Exception e) {
            throw new ModuleInitializationException("Error while attempting to "
                    + "check for and create non-existing table(s): "
                    + e.getClass().getName() + ": " + e.getMessage(), getRole());
        }

    }

    public void releaseWriter(DOWriter writer) {
        writer.invalidate();
        // remove pid from tracked list...m_writers.remove(writer);
    }

    public ILowlevelStorage getObjectStore() {
        return FileSystemLowlevelStorage.getObjectStore();
    }

    public ILowlevelStorage getDatastreamStore() {
        return FileSystemLowlevelStorage.getDatastreamStore();
    }

    public ILowlevelStorage getTempStore() {
        return FileSystemLowlevelStorage.getTempStore();
    }

    public ConnectionPool getConnectionPool() {
        return m_connectionPool;
    }

    public DOValidator getDOValidator() {
        return m_validator;
    }

    public DOReplicator getReplicator() {
        return m_replicator;
    }

    public String[] getRequiredModuleRoles() {
        // FIXME add "fedora.server.resourceIndex.ResourceIndex" once
        // we force loading of the module
        return new String[] {
                "fedora.server.management.PIDGenerator",
                "fedora.server.search.FieldSearch",
                "fedora.server.storage.ConnectionPoolManager",
                "fedora.server.storage.ExternalContentManager",
                "fedora.server.storage.translation.DOTranslator",
                "fedora.server.storage.replication.DOReplicator",
                "fedora.server.validation.DOValidator" };
    }

    public String getStorageFormat() {
        return m_defaultStorageFormat;
    }

    public String getDefaultExportFormat() {
        return m_defaultExportFormat;
    }

    public String getStorageCharacterEncoding() {
        return m_storageCharacterEncoding;
    }

    public DOTranslator getTranslator() {
        return m_translator;
    }

    /**
     * Tells whether the context indicates that cached objects are required.
     */
    private static boolean cachedObjectRequired(Context context) {
        String c=context.get("useCachedObject");
        if (c!=null && c.equalsIgnoreCase("true")) {
            return true;
        } else {
            return false;
        }
    }

	/**
	 * Gets a reader on an an existing digital object.
	 */
    public DOReader getReader(Context context, String pid)
            throws ServerException {
        if (cachedObjectRequired(context)) {
            return new FastDOReader(context, pid);
        } else {
            return new SimpleDOReader(context, this, m_translator,
                    m_defaultExportFormat, m_defaultStorageFormat,
                    m_storageCharacterEncoding,
                    getObjectStore().retrieve(pid), this);
        }
    }
	/**
	 * Gets a reader on an an existing behavior mechanism object.
	 */
    public BMechReader getBMechReader(Context context, String pid)
            throws ServerException {
        if (cachedObjectRequired(context)) {
            return new FastBmechReader(context, pid);
            //throw new InvalidContextException("A BMechReader is unavailable in a cached context.");
        } else {
            return new SimpleBMechReader(context, this, m_translator,
                    m_defaultExportFormat, m_defaultStorageFormat,
                    m_storageCharacterEncoding,
                    getObjectStore().retrieve(pid), this);
        }
    }

	/**
	 * Gets a reader on an an existing behavior definition object.
	 */
    public BDefReader getBDefReader(Context context, String pid)
            throws ServerException {
        if (cachedObjectRequired(context)) {
            return new FastBdefReader(context, pid);
            //throw new InvalidContextException("A BDefReader is unavailable in a cached context.");
        } else {
            return new SimpleBDefReader(context, this, m_translator,
                    m_defaultExportFormat, m_defaultStorageFormat,
                    m_storageCharacterEncoding,
                    getObjectStore().retrieve(pid), this);
        }
    }
    
	/**
	 * Gets a writer on an an existing object.
	 */
	public DOWriter getWriter(Context context, String pid)
			throws ServerException, ObjectLockedException {
		if (cachedObjectRequired(context)) {
			throw new InvalidContextException("A DOWriter is unavailable in a cached context.");
		} else {
			// TODO: make sure there's no SESSION lock on a writer for the pid

			BasicDigitalObject obj=new BasicDigitalObject();
			m_translator.deserialize(getObjectStore().retrieve(pid), obj,
					m_defaultStorageFormat, m_storageCharacterEncoding, 
					DOTranslationUtility.DESERIALIZE_INSTANCE);
			DOWriter w=new SimpleDOWriter(context, this, m_translator,
					m_defaultStorageFormat,
					m_storageCharacterEncoding, obj, this);
			// add to internal list...somehow..think...
			System.gc();
			return w;
		}
	}

	/**
	 * Manages the INGEST process which includes validation of the ingest
	 * XML file, deserialization of the XML into a Digital Object instance,
	 * setting of properties on the object by the system (dates and states),
	 * PID validation or generation, object registry functions, getting a
	 * writer for the digital object, and ultimately writing the object to
	 * persistent storage via the writer.
	 * 
	 * @param context 
	 * @param in  the input stream that is the XML ingest file for a digital object
	 * @param format  the format of the XML ingest file (e.g., FOXML, Fedora METS)
	 * @param encoding  the character encoding of the XML ingest file (e.g., UTF-8)
	 * @param newPid  true if the system should generate a new PID for the object
	 *
	 */
	public synchronized DOWriter getIngestWriter(Context context, InputStream in, String format, 
		String encoding, boolean newPid)
			throws ServerException {
		getServer().logFinest("INGEST: start ingest via DefaultDOManager.getIngestWriter.");

		File tempFile = null;
		if (cachedObjectRequired(context)) {
			throw new InvalidContextException("A DOWriter is unavailable in a cached context.");
		} else {
			try {
				// CURRENT TIME:
				// Get the current time to use for created dates on object
				// and object components (if they are not already there).
				Date nowUTC=DateUtility.convertLocalDateToUTCDate(new Date());
				
				// TEMP STORAGE:
				// write ingest input stream to a temporary file
				tempFile = File.createTempFile("fedora-ingest-temp", ".xml");
				logFinest("INGEST: Creating temporary file for ingest: " + tempFile.toString());
				StreamUtility.pipeStream(in, new FileOutputStream(tempFile), 4096);

				// VALIDATION: 
				// perform initial validation of the ingest submission file
				logFinest("INGEST: Validation (ingest phase)...");
				m_validator.validate(tempFile, format, DOValidatorImpl.VALIDATE_ALL, "ingest");

				// DESERIALIZE:
				// deserialize the ingest input stream into a digital object instance
				BasicDigitalObject obj=new BasicDigitalObject();
				// FIXME: just setting ownerId manually for now...
				obj.setOwnerId("fedoraAdmin");
				obj.setNew(true);
				if (fedora.server.Debug.DEBUG) System.out.println("Deserializing from format: " + format);
				logFinest("INGEST: Deserializing from format: " + format);
				m_translator.deserialize(new FileInputStream(tempFile), obj, format, encoding, 
					DOTranslationUtility.DESERIALIZE_INSTANCE);
                	
				// SET OBJECT PROPERTIES:
				logFinest("INGEST: Setting object/component states and create dates if unset...");
				// set object state to "A" (Active) if not already set
				if (obj.getState()==null || obj.getState().equals("")) {
					obj.setState("A");
				}
				// set object create date to UTC if not already set
				if (obj.getCreateDate()==null || obj.getCreateDate().equals("")) {
					obj.setCreateDate(nowUTC);
				}
				// set object last modified date to UTC
				obj.setLastModDate(nowUTC);
				
				// SET DATASTREAM PROPERTIES...
				Iterator dsIter=obj.datastreamIdIterator();
				while (dsIter.hasNext()) {
					List dsList=(List) obj.datastreams((String) dsIter.next());
					for (int i=0; i<dsList.size(); i++) {
						Datastream ds=(Datastream) dsList.get(i);
						// Set create date to UTC if not already set
						if (ds.DSCreateDT==null || ds.DSCreateDT.equals("")) {
							ds.DSCreateDT=nowUTC;
						}
						// Set state to "A" (Active) if not already set
						if (ds.DSState==null || ds.DSState.equals("")) {
							ds.DSState="A";
						}
					}
				}
				// SET DISSEMINATOR PROPERTIES...
				Iterator dissIter=obj.disseminatorIdIterator();
				while (dissIter.hasNext()) {
					List dissList=(List) obj.disseminators((String) dissIter.next());
					for (int i=0; i<dissList.size(); i++) {
						Disseminator diss=(Disseminator) dissList.get(i);
						// Set create date to UTC if not already set
						if (diss.dissCreateDT==null || diss.dissCreateDT.equals("")) {
							diss.dissCreateDT=nowUTC;
						}
						// Set state to "A" (Active) if not already set
						if (diss.dissState==null || diss.dissState.equals("")) {
							diss.dissState="A";
						}
					}
				}

				// PID VALIDATION:
				// validate and normalized the provided pid, if any
				if ( (obj.getPid() != null) && (obj.getPid().length() > 0) ) {
					obj.setPid(Server.getPID(obj.getPid()).toString());
				}

				// PID GENERATION:
				// have the system generate a PID if one was not provided
				if ( ( obj.getPid()!=null )
						&& ( obj.getPid().indexOf(":")!=-1 )
						&& ( ( m_retainPIDs==null )
								|| ( m_retainPIDs.contains(obj.getPid().split(":")[0]) )
								)
						) {
					getServer().logFinest("INGEST: Stream contained PID with retainable namespace-id... will use PID from stream.");
					try {
						m_pidGenerator.neverGeneratePID(obj.getPid());
					} catch (IOException e) {
						throw new GeneralException("Error calling pidGenerator.neverGeneratePID(): " + e.getMessage());
					}
				} else {
					if (newPid) {
						getServer().logFinest("INGEST: client wants a new PID.");
						// yes... so do that, then set it in the obj.
						String p=null;
						try {
							p=m_pidGenerator.generatePID(m_pidNamespace);
						} catch (Exception e) {
							throw new GeneralException("Error generating PID, PIDGenerator returned unexpected error: ("
									+ e.getClass().getName() + ") - " + e.getMessage());
						}
						getServer().logFiner("INGEST: Generated new PID: " + p);
						obj.setPid(p);
					} else {
						getServer().logFinest("INGEST: client wants to use existing PID.");
					}
				}
                
				// CHECK REGISTRY:
				// ensure the object doesn't already exist
				if (objectExists(obj.getPid())) {
					throw new ObjectExistsException("The PID '" + obj.getPid() + "' already exists in the registry... the object can't be re-created.");
				}

				// GET DIGITAL OBJECT WRITER:
				// get an object writer configured with the DEFAULT export format
				if (fedora.server.Debug.DEBUG) System.out.println("Getting new writer with default export format: " + m_defaultExportFormat);
				logFinest("INGEST: Instantiating a SimpleDOWriter...");
				DOWriter w=new SimpleDOWriter(context, this, m_translator,
						m_defaultExportFormat,
						m_storageCharacterEncoding, obj, this);
                
				// DEFAULT DUBLIN CORE DATASTREAM:
				logFinest("INGEST: Adding/Checking default DC record...");
				// DC System Reserved Datastream...
				// if there's no DC datastream, add one using PID for identifier
				// and Label for dc:title
				//
				// if there IS a DC record, make sure one of the dc:identifiers
				// is the PID
				DatastreamXMLMetadata dc=(DatastreamXMLMetadata) w.GetDatastream("DC", null);
				DCFields dcf;
				if (dc==null) {
					dc=new DatastreamXMLMetadata("UTF-8");
					dc.DSMDClass=0;
					//dc.DSMDClass=DatastreamXMLMetadata.DESCRIPTIVE;
					dc.DatastreamID="DC";
					dc.DSVersionID="DC1.0";
					dc.DSControlGrp="X";
					dc.DSCreateDT=nowUTC;
					dc.DSLabel="Dublin Core Metadata";
					dc.DSMIME="text/xml";
					dc.DSSize=0;
					dc.DSState="A";
					dc.DSVersionable=true;
					dcf=new DCFields();
					if (obj.getLabel()!=null && !(obj.getLabel().equals(""))) {
						dcf.titles().add(obj.getLabel());
					}
					w.addDatastream(dc);
				} else {
					dcf=new DCFields(new ByteArrayInputStream(dc.xmlContent));
				}
				// ensure one of the dc:identifiers is the pid
				boolean sawPid=false;
				for (int i=0; i<dcf.identifiers().size(); i++) {
					if ( ((String) dcf.identifiers().get(i)).equals(obj.getPid()) ) {
						sawPid=true;
					}
				}
				if (!sawPid) {
					dcf.identifiers().add(obj.getPid());
				}
				// set the value of the dc datastream according to what's in the DCFields object
				try {
					dc.xmlContent=dcf.getAsXML().getBytes("UTF-8");
				} catch (UnsupportedEncodingException uee) {
					// safely ignore... we know UTF-8 works
				}
                
				// RELATIONSHIP METADATA VALIDATION:
				// if a RELS-EXT datastream exists do validation on it
				RelsExtValidator deser=new RelsExtValidator("UTF-8", false);
				DatastreamXMLMetadata relsext=(DatastreamXMLMetadata) w.GetDatastream("RELS-EXT", null);
				if (relsext!=null) {
					InputStream in2 = new ByteArrayInputStream(relsext.xmlContent);
					logFinest("INGEST: Validating RELS-EXT datastream...");
					deser.deserialize(in2, "info:fedora/" + obj.getPid());
					if (fedora.server.Debug.DEBUG) System.out.println("Done validating RELS-EXT.");
					logFinest("INGEST: RELS-EXT datastream passed validation.");
				}

				// REGISTRY:
				// at this point the object is valid, so make a record 
				// of it in the digital object registry
				registerObject(obj.getPid(), obj.getFedoraObjectType(), 
					getUserId(context), obj.getLabel(), obj.getContentModelId(), 
					obj.getCreateDate(), obj.getLastModDate());
				return w;
			} catch (IOException e) {
				String message = e.getMessage();
				if (message == null) message = e.getClass().getName();
				e.printStackTrace();
				throw new GeneralException("Error reading/writing temporary ingest file: " + message);
			} catch (Exception e) {
				if (e instanceof ServerException) {
					ServerException se = (ServerException) e;
					throw se;
				}
				String message = e.getMessage();
				if (message == null) message = e.getClass().getName();
				throw new GeneralException("Ingest failed: " + message);
			} finally {
				if (tempFile != null) {
					logFinest("Finally, removing temp file...");
					try {
						tempFile.delete();
					} catch (Exception e) {
						// don't worry if it doesn't exist
					}
				}
			}
		}
	}

	/**
	 * Gets a writer on a new, empty object.
	 */
	/*
	public synchronized DOWriter getIngestWriter(Context context)
			throws ServerException {
		getServer().logFinest("INGEST: Entered DefaultDOManager.getIngestWriter(Context)");
		if (cachedObjectRequired(context)) {
			throw new InvalidContextException("A DOWriter is unavailable in a cached context.");
		} else {
			BasicDigitalObject obj=new BasicDigitalObject();
			getServer().logFinest("Creating object, need a new PID.");
			String p=null;
			try {
				p=m_pidGenerator.generatePID(m_pidNamespace);
			} catch (Exception e) {
				throw new GeneralException("Error generating PID, PIDGenerator returned unexpected error: ("
						+ e.getClass().getName() + ") - " + e.getMessage());
			}
			getServer().logFiner("Generated PID: " + p);
			obj.setPid(p);
			if (objectExists(obj.getPid())) {
				throw new ObjectExistsException("The PID '" + obj.getPid() + "' already exists in the registry... the object can't be re-created.");
			}
			// make a record of it in the registry
			// FIXME: this method is incomplete...
			// obj.setNew(true);
			//registerObject(obj.getPid(), obj.getFedoraObjectType(), getUserId(context));

			// serialize to disk, then validate.. if that's ok, go on.. else unregister it!
		}
		return null;
	}
	*/

    /**
     * The doCommit method finalizes an ingest/update/remove of a digital object. 
     * The process makes updates the object modified date, stores managed content 
     * datastreams, creates the final XML serialization of the digital object, 
     * saves the object to persistent storage, updates the object registry, 
     * and replicates the object's current version information to the relational db.
     *
     * In the case where it is not a deletion, the session lock (TODO) is released, too.
     * This happens as the result of a writer.commit() call.
     *
     */
    public void doCommit(Context context, DigitalObject obj, String logMessage, boolean remove)
            throws ServerException {
        // OBJECT REMOVAL...
        if (remove) {
            logFinest("COMMIT: Entered doCommit (remove)");
            // REFERENTIAL INTEGRITY:
            // Before removing an object, verify that there are no other objects
            // in the repository that depend on the object being deleted.
            FieldSearchResult result = findObjects(context,
                new String[] {"pid"}, 10,
                new FieldSearchQuery(Condition.getConditions("bDef~"+obj.getPid())));
            if (result.objectFieldsList().size() > 0)
            {
                throw new ObjectDependencyException("The digital object \""
                    + obj.getPid() + "\" is used by one or more other objects "
                    + "in the repository. All related objects must be removed "
                    + "before this object may be deleted. Use the search "
                    + "interface with the query \"bDef~" + obj.getPid()
                    + "\" to obtain a list of dependent objects.");
            }
            result = findObjects(context,
                new String[] {"pid"}, 10,
                new FieldSearchQuery(Condition.getConditions("bMech~"+obj.getPid())));
            if (result.objectFieldsList().size() > 0)
            {
              throw new ObjectDependencyException("The digital object \""
                  + obj.getPid() + "\" is used by one or more other objects "
                  + "in the repository. All related objects must be removed "
                  + "before this object may be deleted. Use the search "
                  + "interface with the query \"bMech~" + obj.getPid()
                  + "\" to obtain a list of dependent objects.");
            }
            // DATASTREAM STORAGE:
            // remove any managed content datastreams associated with object
            // from persistent storage.
            Iterator dsIDIter = obj.datastreamIdIterator();
            while (dsIDIter.hasNext())
            {
              String dsID=(String) dsIDIter.next();
              String controlGroupType =
                  ((Datastream) obj.datastreams(dsID).get(0)).DSControlGrp;
              if ( controlGroupType.equalsIgnoreCase("M"))
              {
                List allVersions = obj.datastreams(dsID);
                Iterator dsIter = allVersions.iterator();

                // iterate over all versions of this dsID
                while (dsIter.hasNext())
                {
                  Datastream dmc =
                      (Datastream) dsIter.next();
                  String id = obj.getPid() + "+" + dmc.DatastreamID + "+"
                      + dmc.DSVersionID;
                  logInfo("COMMIT: Deleting ManagedContent datastream. " + "id: " + id);
                  try {
                    getDatastreamStore().remove(id);
                  } catch (LowlevelStorageException llse) {
                    logWarning("COMMIT: While attempting removal of managed content datastream: " + llse.getClass().getName() + ": " + llse.getMessage());
                  }
                }
              }
            }
            // STORAGE:
            // remove digital object from persistent storage
            try {
                getObjectStore().remove(obj.getPid());
            } catch (ObjectNotInLowlevelStorageException onilse) {
                logWarning("COMMIT: Object wasn't found in permanent low level store, but that might be ok...continuing with purge.");
            }
            // REGISTRY:
            // Remove digital object from the registry
            boolean wasInRegistry=false;
            try {
                unregisterObject(obj.getPid());
                wasInRegistry=true;
            } catch (ServerException se) {
                logWarning("COMMIT: Object couldn't be removed from registry, but that might be ok...continuing with purge.");
            }
            if (wasInRegistry) {
                try {
                    // Set entry for this object to "D" in the replication jobs table
                    addReplicationJob(obj.getPid(), true);
                    // tell replicator to do deletion
                    m_replicator.delete(obj.getPid());
                    removeReplicationJob(obj.getPid());
                } catch (ServerException se) {
                    logWarning("COMMIT: Object couldn't be deleted from the cached copy (" + se.getMessage() + ") ... leaving replication job unfinished.");
                }
            }
			// FIELD SEARCH INDEX:
			// remove digital object from the default search index
            try {
                logInfo("COMMIT: Deleting from FieldSearch indexes...");
                m_fieldSearch.delete(obj.getPid());
            } catch (ServerException se) {
                logWarning("COMMIT: Object couldn't be removed from fieldsearch indexes (" + se.getMessage() + "), but that might be ok...continuing with purge.");
            }
            
            // RESOURCE INDEX:
            // remove digital object from the resourceIndex
            try {
                logInfo("COMMIT: Deleting from ResourceIndex...");
                m_resourceIndex.deleteDigitalObject(obj);
            } catch (ServerException se) {
                logWarning("COMMIT: Object couldn't be removed from ResourceIndex (" + se.getMessage() + "), but that might be ok...continuing with purge.");
            }
            
		// OBJECT INGEST (ADD) OR MODIFY...
        } else {
            logFinest("COMMIT: Entered doCommit (add/modify)");
            try {
                // DATASTREAM STORAGE:
                // copy and store any datastreams of type Managed Content
                Iterator dsIDIter = obj.datastreamIdIterator();
                while (dsIDIter.hasNext())
                {
                  String dsID=(String) dsIDIter.next();
                  Datastream dStream=(Datastream) obj.datastreams(dsID).get(0);
                  String controlGroupType = dStream.DSControlGrp;
                  if ( controlGroupType.equalsIgnoreCase("M") )
                       // if it's managed, we might need to grab content
                  {
                    List allVersions = obj.datastreams(dsID);
                    Iterator dsIter = allVersions.iterator();

                    // iterate over all versions of this dsID
                    while (dsIter.hasNext())
                    {
                      Datastream dmc =
                          (Datastream) dsIter.next();
                      if (dmc.DSLocation.indexOf("//")!=-1) {
                        // if it's a url, we need to grab content for this version
                        MIMETypedStream mimeTypedStream;
						if (dmc.DSLocation.startsWith("uploaded://")) {
						    mimeTypedStream=new MIMETypedStream(null, m_management.getTempStream(dmc.DSLocation), null);
                            logInfo("COMMIT: Retrieving ManagedContent datastream from internal uploaded "
                                + "location: " + dmc.DSLocation);
						} else if (dmc.DSLocation.startsWith("copy://"))  {
                            // make a copy of the pre-existing content
                            mimeTypedStream=new MIMETypedStream(null,
                                    getDatastreamStore().retrieve(
                                            dmc.DSLocation.substring(7)), null);
						} else {
                            mimeTypedStream = m_contentManager.
                                getExternalContent(dmc.DSLocation.toString());
                            logInfo("COMMIT: Retrieving ManagedContent datastream from remote "
                                + "location: " + dmc.DSLocation);
						}
                        String id = obj.getPid() + "+" + dmc.DatastreamID + "+"
                                  + dmc.DSVersionID;
                        if (obj.isNew()) {
                            getDatastreamStore().add(id, mimeTypedStream.getStream());
                        } else {
                            // object already existed...so we may need to call
                            // replace if "add" indicates that it was already there
                            try {
                                getDatastreamStore().add(id, mimeTypedStream.getStream());
                            } catch (ObjectAlreadyInLowlevelStorageException oailse) {
                                getDatastreamStore().replace(id, mimeTypedStream.getStream());
                            }
                        }
                        // Reset dsLocation in object to new internal location.
                        dmc.DSLocation = id;
                        logInfo("COMMIT: Replacing ManagedContent datastream with "
                            + "internal id: " + id);
                        //bais = null;
                      }
                    }
                  }
                }

                // MODIFIED DATE:
                // set digital object last modified date, in UTC
                obj.setLastModDate(DateUtility.convertLocalDateToUTCDate(new Date()));
                    ByteArrayOutputStream out = new ByteArrayOutputStream();

				// FINAL XML SERIALIZATION:
				// serialize the object in its final form for persistent storage                                      
                logFinest("COMMIT: Serializing digital object for persistent storage...");
                m_translator.serialize(obj, out, m_defaultStorageFormat, 
                	m_storageCharacterEncoding, DOTranslationUtility.SERIALIZE_STORAGE_INTERNAL);

				// FINAL VALIDATION:
				// As of version 2.0, final validation is only performed in DEBUG mode.
				// This is to help performance during the ingest process since validation
				// is a large amount of the overhead of ingest.  Instead of a second run
				// of the validation module, we depend on the integrity of our code to 
				// create valid XML files for persistent storage of digital objects.
				if (fedora.server.Debug.DEBUG) {
					ByteArrayInputStream inV = new ByteArrayInputStream(out.toByteArray());
					logFinest("COMMIT: Final Validation (storage phase)...");
					m_validator.validate(inV, m_defaultStorageFormat,
						DOValidatorImpl.VALIDATE_ALL, "store");
				}                     	
                    
                // RESOURCE INDEX:
                logFinest("COMMIT: Adding to ResourceIndex...");
                if (obj.isNew()) {
                    m_resourceIndex.addDigitalObject(obj);
                } else {
                    m_resourceIndex.modifyDigitalObject(obj);
                }
                logFinest("COMMIT: Finished adding to ResourceIndex.");
                
                // STORAGE: 
                // write XML serialization of object to persistent storage
                logFinest("COMMIT: Storing digital object...");
                if (obj.isNew()) {
                    getObjectStore().add(obj.getPid(), new ByteArrayInputStream(out.toByteArray()));
                } else {
                    getObjectStore().replace(obj.getPid(), new ByteArrayInputStream(out.toByteArray()));
                }
                
                // REGISTRY:
				// update systemVersion in doRegistry (add one)
                logFinest("COMMIT: Updating registry...");
                Connection conn=null;
                Statement s = null;
                ResultSet results=null;
                try {
                    conn=m_connectionPool.getConnection();
                    String query="SELECT systemVersion "
                               + "FROM doRegistry "
                               + "WHERE doPID='" + obj.getPid() + "'";
                    s=conn.createStatement();
                    results=s.executeQuery(query);
                    if (!results.next()) {
                        throw new ObjectNotFoundException("Error creating replication job: The requested object doesn't exist in the registry.");
                    }
                    int systemVersion=results.getInt("systemVersion");
                    systemVersion++;
                    Date now=new Date();
                    s.executeUpdate("UPDATE doRegistry SET systemVersion="
                            + systemVersion + " "
                            + "WHERE doPID='" + obj.getPid() + "'");
                } catch (SQLException sqle) {
                    throw new StorageDeviceException("Error creating replication job: " + sqle.getMessage());
                } finally {
                    try
                    {
                      if (results!=null) results.close();
                      if (s!= null) s.close();
                      if (conn!=null) m_connectionPool.free(conn);
                    } catch (SQLException sqle)
                    {
                        throw new StorageDeviceException("Unexpected error from SQL database: " + sqle.getMessage());
                    } finally {
                        results=null;
                        s=null;
                    }
                }
                // REPLICATE:
                // add to replication jobs table and do replication to db
                logFinest("COMMIT: Adding replication job...");
                addReplicationJob(obj.getPid(), false);
                try {
                    if (obj.getFedoraObjectType()==DigitalObject.FEDORA_BDEF_OBJECT) {
                        logInfo("COMMIT: Attempting replication as bdef object: " + obj.getPid());
                        BDefReader reader=getBDefReader(context, obj.getPid());
                        m_replicator.replicate(reader);
                        logInfo("COMMIT: Updating FieldSearch indexes...");
                        m_fieldSearch.update(reader);
                    } else if (obj.getFedoraObjectType()==DigitalObject.FEDORA_BMECH_OBJECT) {
                        logInfo("COMMIT: Attempting replication as bmech object: " + obj.getPid());
                        BMechReader reader=getBMechReader(context, obj.getPid());
                        m_replicator.replicate(reader);
                        logInfo("COMMIT: Updating FieldSearch indexes...");
                        m_fieldSearch.update(reader);
                    } else {
                        logInfo("COMMIT: Attempting replication as normal object: " + obj.getPid());
                        DOReader reader=getReader(context, obj.getPid());
                        m_replicator.replicate(reader);
                        logInfo("COMMIT: Updating FieldSearch indexes...");
                        m_fieldSearch.update(reader);
                    }
                    // FIXME: also remove from temp storage if this is successful
                    removeReplicationJob(obj.getPid());
                } catch (ServerException se) {
                  System.out.println("Error while replicating: " + se.getClass().getName() + ": " + se.getMessage());
                  se.printStackTrace();
                    throw se;
                } catch (Throwable th) {
                  System.out.println("Error while replicating: " + th.getClass().getName() + ": " + th.getMessage());
                  logStackTrace(th);
                    throw new GeneralException("Replicator returned error: (" + th.getClass().getName() + ") - " + th.getMessage());
                }
            } catch (ServerException se) {
                if (obj.isNew()) {
                    doCommit(context, obj, logMessage, true);
                }
                throw se;
            }
        }
    }

    private void logStackTrace(Throwable th) {
        StackTraceElement[] els=th.getStackTrace();
        StringBuffer lines=new StringBuffer();
        for (int i=0; i<els.length; i++) {
            lines.append(els[i].toString());
            lines.append("\n");
        }
        logFiner("Stack trace: " + th.getClass().getName() + "\n" + lines.toString());
    }

    /**
     * Add an entry to the replication jobs table.
     */
    private void addReplicationJob(String pid, boolean deleted)
            throws StorageDeviceException {
        Connection conn=null;
        String[] columns=new String[] {"doPID", "action"};
        String action="M";
        if (deleted) {
            action="D";
        }
        String[] values=new String[] {pid, action};
        try {
            conn=m_connectionPool.getConnection();
            SQLUtility.replaceInto(conn, "doRepJob", columns,
                    values, "doPID");
        } catch (SQLException sqle) {
            throw new StorageDeviceException("Error creating replication job: " + sqle.getMessage());
        } finally {
            if (conn!=null) {
                m_connectionPool.free(conn);
            }
        }
    }

    private void removeReplicationJob(String pid)
            throws StorageDeviceException {
        Connection conn=null;
        Statement s=null;
        try {
            conn=m_connectionPool.getConnection();
            s=conn.createStatement();
            s.executeUpdate("DELETE FROM doRepJob "
                    + "WHERE doPID = '" + pid + "'");
        } catch (SQLException sqle) {
            throw new StorageDeviceException("Error removing entry from replication jobs table: " + sqle.getMessage());
        } finally {

            try {
                if (s!=null) s.close();
                if (conn!=null) m_connectionPool.free(conn);
            } catch (SQLException sqle) {
                throw new StorageDeviceException("Unexpected error from SQL database: " + sqle.getMessage());
            } finally {
                s=null;
            }
        }
    }


    /**
     * Gets the userId property from the context... if it's not
     * populated, throws an InvalidContextException.
     */
    private String getUserId(Context context)
            throws InvalidContextException {
        String ret=context.get("userId");
        if (ret==null) {
            throw new InvalidContextException("The context identifies no userId, but a user must be identified for this operation.");
        }
        return ret;
    }

    /**
     * Checks the object registry for the given object.
     */
    public boolean objectExists(String pid)
            throws StorageDeviceException {
        logFinest("Checking if " + pid + " already exists...");
        Connection conn=null;
        Statement s = null;
        ResultSet results=null;
        try {
            String query="SELECT doPID "
                       + "FROM doRegistry "
                       + "WHERE doPID='" + pid + "'";
            conn=m_connectionPool.getConnection();
            s=conn.createStatement();
            results=s.executeQuery(query);
            return results.next(); // 'true' if match found, else 'false'
        } catch (SQLException sqle) {
            throw new StorageDeviceException("Unexpected error from SQL database: " + sqle.getMessage());
        } finally {
          try
          {
            if (results!=null) results.close();
            if (s!= null) s.close();
            if (conn!=null) m_connectionPool.free(conn);
          } catch (SQLException sqle)
          {
              throw new StorageDeviceException("Unexpected error from SQL database: " + sqle.getMessage());
          } finally {
              results=null;
              s=null;
              logFinest("Finished checking if " + pid + " already exists...");
          }
        }
    }

    public String getOwnerId(String pid)
            throws StorageDeviceException, ObjectNotFoundException {
        Connection conn=null;
        Statement s = null;
        ResultSet results=null;
        try {
            String query="SELECT ownerId "
                       + "FROM doRegistry "
                       + "WHERE doPID='" + pid + "'";
            conn=m_connectionPool.getConnection();
            s=conn.createStatement();
            results=s.executeQuery(query);
            if (results.next()) {
                return results.getString(1);
            } else {
                throw new ObjectNotFoundException("Object " + pid + " not found in object registry.");
            }
        } catch (SQLException sqle) {
            throw new StorageDeviceException("Unexpected error from SQL database: " + sqle.getMessage());
        } finally {
          try
          {
            if (results!=null) results.close();
            if (s!= null) s.close();
            if (conn!=null) m_connectionPool.free(conn);
          } catch (SQLException sqle)
          {
            throw new StorageDeviceException("Unexpected error from SQL database: " + sqle.getMessage());
          } finally {
              results=null;
              s=null;
          }
        }
    }

    /**
     * Adds a new object.
     */
    private void registerObject(String pid, int fedoraObjectType, String userId,
            String label, String contentModelId, Date createDate, Date lastModDate)
            throws StorageDeviceException {
        // label or contentModelId may be null...set to blank if so
        String theLabel=label;
        if (theLabel==null) {
            theLabel="";
        }
        String theContentModelId=contentModelId;
        if (theContentModelId==null) {
            theContentModelId="";
        }
        Connection conn=null;
        Statement st=null;
        String foType="O";
        if (fedoraObjectType==DigitalObject.FEDORA_BDEF_OBJECT) {
            foType="D";
        }
        if (fedoraObjectType==DigitalObject.FEDORA_BMECH_OBJECT) {
            foType="M";
        }
        try {
            String query="INSERT INTO doRegistry (doPID, foType, "
                                                   + "ownerId, label, "
                                                   + "contentModelID) "
                       + "VALUES ('" + pid + "', '" + foType +"', '"
                                     + userId +"', '" + SQLUtility.aposEscape(theLabel) + "', '"
                                     + theContentModelId + "')";
            conn=m_connectionPool.getConnection();
            st=conn.createStatement();
            st.executeUpdate(query);
        } catch (SQLException sqle) {
            throw new StorageDeviceException("Unexpected error from SQL database while registering object: " + sqle.getMessage());
        } finally {
            try {
                if (st!=null) st.close();
                if (conn!=null) m_connectionPool.free(conn);
            } catch (Exception sqle) {
                throw new StorageDeviceException("Unexpected error from SQL database while registering object: " + sqle.getMessage());
            } finally {
                st=null;
            }
        }
    }

    /**
     * Removes an object from the object registry.
     */
    private void unregisterObject(String pid)
            throws StorageDeviceException {
        Connection conn=null;
        Statement st=null;
        try {
            conn=m_connectionPool.getConnection();
            st=conn.createStatement();
            st.executeUpdate("DELETE FROM doRegistry WHERE doPID='" + pid + "'");
        } catch (SQLException sqle) {
            throw new StorageDeviceException("Unexpected error from SQL database while unregistering object: " + sqle.getMessage());
        } finally {
            try {
                if (st!=null) st.close();
                if (conn!=null) m_connectionPool.free(conn);
            } catch (Exception sqle) {
                throw new StorageDeviceException("Unexpected error from SQL database while unregistering object: " + sqle.getMessage());
            } finally {
                st=null;
            }
        }
    }


    public String[] listObjectPIDs(Context context)
            throws StorageDeviceException {
        return getPIDs("WHERE systemVersion > 0");
    }


    // translates simple wildcard string to sql-appropriate.
    // the first character is a " " if it needs an escape
    public static String toSql(String name, String in) {
        if (in.indexOf("\\")!=-1) {
            // has one or more escapes, un-escape and translate
            StringBuffer out=new StringBuffer();
            out.append("\'");
            boolean needLike=false;
            boolean needEscape=false;
            boolean lastWasEscape=false;
            for (int i=0; i<in.length(); i++) {
                char c=in.charAt(i);
                if ( (!lastWasEscape) && (c=='\\') ) {
                    lastWasEscape=true;
                } else {
                    char nextChar='!';
                    boolean useNextChar=false;
                    if (!lastWasEscape) {
                        if (c=='?') {
                            out.append('_');
                            needLike=true;
                        } else if (c=='*') {
                            out.append('%');
                            needLike=true;
                        } else {
                            nextChar=c;
                            useNextChar=true;
                        }
                    } else {
                        nextChar=c;
                        useNextChar=true;
                    }
                    if (useNextChar) {
                        if (nextChar=='\"') {
                            out.append("\\\"");
                            needEscape=true;
                        } else if (nextChar=='\'') {
                            out.append("\\\'");
                            needEscape=true;
                        } else if (nextChar=='%') {
                            out.append("\\%");
                            needEscape=true;
                        } else if (nextChar=='_') {
                            out.append("\\_");
                            needEscape=true;
                        } else {
                            out.append(nextChar);
                        }
                    }
                    lastWasEscape=false;
                }
            }
            out.append("\'");
            if (needLike) {
                out.insert(0, " LIKE ");
            } else {
                out.insert(0, " = ");
            }
            out.insert(0, name);
            if (needEscape) {
                out.insert(0, ' ');
            }
            return out.toString();
        } else {
            // no escapes, just translate if needed
            StringBuffer out=new StringBuffer();
            out.append("\'");
            boolean needLike=false;
            boolean needEscape=false;
            for (int i=0; i<in.length(); i++) {
                char c=in.charAt(i);
                if (c=='?') {
                    out.append('_');
                    needLike=true;
                } else if (c=='*') {
                    out.append('%');
                    needLike=true;
                } else if (c=='\"') {
                    out.append("\\\"");
                    needEscape=true;
                } else if (c=='\'') {
                    out.append("\\\'");
                    needEscape=true;
                } else if (c=='%') {
                    out.append("\\%");
                    needEscape=true;
                } else if (c=='_') {
                    out.append("\\_");
                    needEscape=true;
                } else {
                    out.append(c);
                }
            }
            out.append("\'");
            if (needLike) {
                out.insert(0, " LIKE ");
            } else {
                out.insert(0, " = ");
            }
            out.insert(0, name);
            if (needEscape) {
                out.insert(0, ' ');
            }
            return out.toString();
        }
    }

    /** whereClause is a WHERE clause, starting with "where" */
    private String[] getPIDs(String whereClause)
            throws StorageDeviceException {
        ArrayList pidList=new ArrayList();
        Connection conn=null;
        Statement s = null;
        ResultSet results=null;
        try {
            conn=m_connectionPool.getConnection();
            s=conn.createStatement();
            StringBuffer query=new StringBuffer();
            query.append("SELECT doPID FROM doRegistry ");
            query.append(whereClause);
            logFinest("Executing db query: " + query.toString());
            results=s.executeQuery(query.toString());
            while (results.next()) {
                pidList.add(results.getString("doPID"));
            }
            String[] ret=new String[pidList.size()];
            Iterator pidIter=pidList.iterator();
            int i=0;
            while (pidIter.hasNext()) {
                ret[i++]=(String) pidIter.next();
            }
            return ret;
        } catch (SQLException sqle) {
            throw new StorageDeviceException("Unexpected error from SQL database: " + sqle.getMessage());

        } finally {
          try
          {
            if (results!=null) results.close();
            if (s!= null) s.close();
            if (conn!=null) m_connectionPool.free(conn);
          } catch (SQLException sqle)
          {
            throw new StorageDeviceException("Unexpected error from SQL database: " + sqle.getMessage());
          } finally {
              results=null;
              s=null;
          }
        }
    }

    public FieldSearchResult findObjects(Context context,
            String[] resultFields, int maxResults, FieldSearchQuery query)
            throws ServerException {
        return m_fieldSearch.findObjects(resultFields, maxResults, query);
    }

    public FieldSearchResult resumeFindObjects(Context context,
            String sessionToken)
            throws ServerException {
        return m_fieldSearch.resumeFindObjects(sessionToken);
    }

    /**
     * <p> Gets a list of the requested next available PIDs. the number of PIDs.</p>
     *
     * @param numPIDs The number of PIDs to generate. Defaults to 1 if the number
     *                is not a positive integer.
     * @param namespace The namespace to be used when generating the PIDs. If
     *                  null, the namespace defined by the <i>pidNamespace</i>
     *                  parameter in the fedora.fcfg configuration file is used.
     * @return An array of PIDs.
     * @throws ServerException If an error occurs in generating the PIDs.
     */
    public String[] getNextPID(int numPIDs, String namespace) throws ServerException {

      if (numPIDs < 1) {
        numPIDs = 1;
      }
      String[] pidList = new String[numPIDs];
      if (namespace==null || namespace.equals("")) {
        namespace = m_pidNamespace;
      }
      try {
        for (int i=0; i<numPIDs; i++)
        {
          pidList[i] = m_pidGenerator.generatePID(namespace);
        }
        return pidList;
        } catch (IOException ioe)
        {
          throw new GeneralException("DefaultDOManager.getNextPID: Error "
              + "generating PID, PIDGenerator returned unexpected error: ("
              + ioe.getClass().getName() + ") - " + ioe.getMessage());
        }
    }
}
