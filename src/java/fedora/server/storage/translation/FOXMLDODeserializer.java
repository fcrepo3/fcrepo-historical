package fedora.server.storage.translation;

import fedora.server.Server;
import fedora.server.errors.ObjectIntegrityException;
import fedora.server.errors.RepositoryConfigurationException;
import fedora.server.errors.InitializationException;
import fedora.server.errors.StreamIOException;
import fedora.server.errors.StreamReadException;
import fedora.server.storage.types.AuditRecord;
import fedora.server.storage.types.DigitalObject;
import fedora.server.storage.types.Datastream;
import fedora.server.storage.types.DatastreamContent;
import fedora.server.storage.types.DatastreamManagedContent;
import fedora.server.storage.types.DatastreamReferencedContent;
import fedora.server.storage.types.DatastreamXMLMetadata;
import fedora.server.storage.types.Disseminator;
import fedora.server.storage.types.DSBindingMap;
import fedora.server.storage.types.DSBinding;
import fedora.server.utilities.DateUtility;
import fedora.server.utilities.StreamUtility;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

// FIXMEs:
// 1.  Implement binaryContent.  Now we flag it but do not process it. (See m_readingContent) Also in METS.
// 2.  Set object-level properties from the objectProperties datastream (during or after parse?)
// 2.  AUDIT: what do they look like now? Finalize new datastream format and check parsing code.
// 3.  AUDIT: (in METS ds backrefs via the ADMID)?  Ignore in FOXML or put in inner rels ds?
// 4.  ADMID and DMDID pointers in METS.  How preserve in FOXML serialization?  Inner relationship datastream?
// 5.  DS location TYPE attribute gets dropped on ingest.  Add to Datastream class?
// 6.  Check use of old METS metadata type categories.  Now just take format URI for m_dsInfoType.
// 7.  Commented out stuff on query behavior to gets the datastream content as inputstream.  What's up?
// 8.  dsVersionable:  if null on ingest or addDatastream, where do we set it to repo defaults?
// 9.  m_extObjPropertyName: how propogate these into default indexing?
// 10. Backward compatibility with METS: AUDIT, object properties, CURRENT attr
// 11. Add content digest parse;  also to DigitalObject.  What do we do with it now?
// 12. Check on xml "data level" tracking in parsing code.  What's up?

/**
 *
 * <p><b>Title:</b> FOXMLDODeserializer.java</p>
 * <p><b>Description:</b> Deserializes the XML of a FOXML-encoded Fedora 
 * digital object into a java object (DigitalObject.class).  Based on the
 * pattern established in METSLikeDODeserializer by Chris Wilper.</p>
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
 * <p>The entire file consists of original code.  Copyright &copy; 2002-2004 by The
 * Rector and Visitors of the University of Virginia and Cornell University.
 * All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 *
 * @author payette@cs.cornell.edu
 */
public class FOXMLDODeserializer
        extends DefaultHandler
        implements DODeserializer {

    /** The namespace for FOXML */
    private final static String F="http://www.fedora.info/definitions/foxml";
    
	/** The namespace for SYSMETA */
	private final static String SYS="http://www.fedora.info/definitions/foxml/sysmeta";

    private SAXParser m_parser;
    private String m_characterEncoding;

    /** The object to deserialize to. */
    private DigitalObject m_obj;

    /**
     * URI-to-namespace prefix mapping info from SAX2 startPrefixMapping events.
     */
    private HashMap m_prefixes;
    private HashMap m_prefixUris;

    private boolean m_rootElementFound;
    private String m_dsId;
	private String m_dsURI;
	private String m_dsVersionable;
    private String m_dsVersId;
    private Date m_dsCreateDate;
    private String m_dsCurrent;
    private String m_dsState;
    //private String m_dsInfoType;
    private String m_dsFormatURI;
    private String m_dsLabel;
    //private int m_dsMDClass;
    private long m_dsSize;
    private String m_dsLocationType;
    private URL m_dsLocation;
    private String m_dsMimeType;
    private String m_dsControlGrp;
	private String m_dissemId;
	private String m_dissemState;
	private String m_extObjPropertyName;
	
	private AuditRecord m_auditRec;

    // key=dsId, value=List of datastream ids (strings)
    private HashMap m_dsAdmIds; // these are saved till end of parse
    private String[] m_dsDmdIds; // these are only saved while parsing cur ds

	// buffer for reading single element content
	private StringBuffer m_elementContent;
	// buffer for reading arbitrary chunks of inline XML metadata
    private StringBuffer m_dsXMLBuffer;
    private StringBuffer m_dsFirstElementBuffer;
    


    // are we reading binaryContent element (base64-encoded)?
    private boolean m_readingBinaryContent;

    private boolean m_firstInlineXMLElement;

    /** Namespace prefixes used in the currently scanned datastream */
    private ArrayList m_dsPrefixes;

    /** While parsing, are we inside XML metadata? */
    private boolean m_inXMLMetadata;

    /**
     * Used to differentiate between a metadata section in this object
     * and a metadata section in an inline XML datastream that happens
     * to be a METS document.
     */
    private int m_xmlDataLevel;

    /** String buffer for audit element contents */
    //private StringBuffer m_auditBuffer;
	private String m_auditComponentID;
    private String m_auditProcessType;
    private String m_auditAction;
    private String m_auditResponsibility;
    private String m_auditDate;
    private String m_auditJustification;

    /** Hashmap for holding disseminators during parsing, keyed
     * by structMapId */
    private HashMap m_dissems;

    /**
     * Currently-being-initialized disseminator, during structmap parsing.
     */
    private Disseminator m_diss;

    /**
     * Whether, while in structmap, we've already seen a div
     */
    //private boolean m_indiv;

    /** The structMapId of the dissem currently being parsed. */
    //private String m_structId;

    /**
     * Never query the server and take it's values for Content-length and
     * Content-type
     */
    public static int QUERY_NEVER=0;

    /**
     * Query the server and take it's values for Content-length and
     * Content-type if either are undefined.
     */
    public static int QUERY_IF_UNDEFINED=1;

    /**
     * Always query the server and take it's values for Content-length and
     * Content-type.
     */
    public static int QUERY_ALWAYS=2;

    private int m_queryBehavior;

    private static Pattern s_localPattern; // "http://local.fedora.server/"
    private static String s_hostInfo; // "http://actual.hostname:8080/"

    public FOXMLDODeserializer()
            throws FactoryConfigurationError, ParserConfigurationException,
            SAXException, UnsupportedEncodingException {
        this("UTF-8", false, QUERY_NEVER);
    }

    /**
     * Initializes by setting up a parser that doesn't validate and never
     * queries the server for values of DSSize and DSMIME.
     */
    public FOXMLDODeserializer(String characterEncoding)
            throws FactoryConfigurationError, ParserConfigurationException,
            SAXException, UnsupportedEncodingException {
        this(characterEncoding, false, QUERY_NEVER);
    }

    /**
     * Initializes by setting up a parser that validates only if validate=true.
     * <p></p>
     * The character encoding of the XML is auto-determined by sax, but
     * we need it for when we set the byte[] in DatastreamXMLMetadata, so
     * we effectively, we need to also specify the encoding of the datastreams.
     * this could be different than how the digital object xml was encoded,
     * and this class won't care.  However, the caller should keep track
     * of the byte[] encoding if it plans on doing any translation of
     * that to characters (such as in xml serialization)
     */
    public FOXMLDODeserializer(String characterEncoding, boolean validate,
            int queryBehavior)
            throws FactoryConfigurationError, ParserConfigurationException,
            SAXException, UnsupportedEncodingException {
        m_queryBehavior=queryBehavior;
        // ensure the desired encoding is supported before starting
        // unsuppenc will be thrown if not
        m_characterEncoding=characterEncoding;
        StringBuffer buf=new StringBuffer();
        buf.append("test");
        byte[] temp=buf.toString().getBytes(m_characterEncoding);
        // then init sax
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setValidating(validate);
        spf.setNamespaceAware(true);
        m_parser=spf.newSAXParser();
    }

    public DODeserializer getInstance()
            throws RepositoryConfigurationException {
        try {
            return (DODeserializer) new FOXMLDODeserializer("UTF-8", false, QUERY_NEVER);
        } catch (Exception e) {
            throw new RepositoryConfigurationException("Error trying to get a "
                    + "new FOXMLDODeserializer instance: " + e.getClass().getName()
                    + ": " + e.getMessage());
        }
    }

    public void deserialize(InputStream in, DigitalObject obj, String encoding)
            throws ObjectIntegrityException, StreamIOException, UnsupportedEncodingException {
        m_obj=obj;
        m_rootElementFound=false;
        m_dsId=null;
		m_dsURI=null;
		m_dsVersionable=null;
        m_dsVersId=null;
        m_dsCreateDate=null;
        m_dsState=null;
        m_dsFormatURI=null;
        //m_dsInfoType=null;
        m_dsLabel=null;
        m_dsXMLBuffer=null;
        m_prefixes=new HashMap();
        m_prefixUris=new HashMap();
        m_dsAdmIds=new HashMap();
        m_dsDmdIds=null;
        m_dissems=new HashMap();
        try {
            m_parser.parse(in, this);
        } catch (IOException ioe) {
            throw new StreamIOException("low-level stream io problem occurred "
                    + "while sax was parsing this object.");
        } catch (SAXException se) {
            throw new ObjectIntegrityException("FOXML stream was bad : " + se.getMessage());
        }
        System.out.println("Just finished parse.");
        // SDP: Can we move any of this logic to a utility class shared among deserializers or is
        // there METS-specific logic here?

        if (!m_rootElementFound) {
            throw new ObjectIntegrityException("digitalObject root element not found in namespace " + F + " .");
        }
        
        obj.setNamespaceMapping(m_prefixes);
        
		//TEST:
		Iterator i = obj.datastreamIdIterator();
		while (i.hasNext()){
			System.out.println("DSID=" + i.next());
		}
        // TEST:
        List dsVersionList = obj.datastreams("SYSMETA");
        Iterator i2 = dsVersionList.iterator();
		while (i2.hasNext()){
			Datastream ds = (Datastream) i2.next();
			System.out.println("DSID=" + ds.DatastreamID);
			System.out.println("DSVersionID=" + ds.DSVersionID);
		}
    }

    public void startPrefixMapping(String prefix, String uri) {
        // save a forward and backward hash of namespace prefix-to-uri
        // mapping ... for the entire object.
        m_prefixes.put(uri, prefix);
        m_prefixUris.put(prefix, uri);
        // if we're looking at inline metadata, be sure to save the prefix
        // so we know it's used in that datastream

        if (m_inXMLMetadata) {
            if (!m_dsPrefixes.contains(prefix)) {
//                if (!"".equals(prefix)) {
                    m_dsPrefixes.add(prefix);
//                }
            }
        }
    }

    public void startElement(String uri, String localName, String qName,
            Attributes a) throws SAXException {

		// Initialize string buffer to hold content of the new element.
		// This will start a fresh buffer for every element encountered.            	
		m_elementContent=new StringBuffer();

        if (uri.equals(F) && !m_inXMLMetadata) {
            // a new foxml element is starting
            if (localName.equals("digitalObject")) {
                m_rootElementFound=true;
				m_obj.setPid(grab(a, F, "PID"));
			} else if (localName.equals("datastream")) {
				// get datastream attributes...
				m_dsId=grab(a, F, "ID");
				m_dsURI=grab(a, F, "URI");
				m_dsState=grab(a, F, "STATE");
				m_dsFormatURI=grab(a, F, "FORMAT_URI");
				m_dsMimeType=grab(a, F, "MIMETYPE");
				m_dsControlGrp=grab(a, F, "CONTROL_GROUP");
				m_dsVersionable=grab(a, F, "VERSIONABLE");
				// Never allow the AUDIT datastream to be versioned
				// since it naturally represents a system-controlled
				// view of changes over time.
				if (m_dsId.equals("AUDIT")) {
					m_dsVersionable="NO";
				}
				// initialize values for datastream version-level attributes				
				m_dsVersId=null;
				m_dsLabel=null;
				m_dsCreateDate=null;
				m_dsSize=-1;
			} else if (localName.equals("datastreamVersion")) {
				// get datastream version-level attributes...
				m_dsVersId=grab(a, F, "ID");
				m_dsLabel=grab(a, F, "LABEL");
				m_dsCreateDate=DateUtility.convertStringToDate(grab(a, F, "CREATED"));
				m_dsCurrent=grab(a, F, "CURRENT");
				String sizeString=grab(a, F, "SIZE");
				if (sizeString!=null && !sizeString.equals("")) {
					try {
						m_dsSize=Long.parseLong(sizeString);
					} catch (NumberFormatException nfe) {
						throw new SAXException("If specified, a datastream's "
								+ "SIZE attribute must be an xsd:long.");
					}
				}
			// get the datastream content...
			// inside a datastreamVersion element, it's either going to be
			// xmlContent (inline xml), contentLocation (a reference) or binaryContent
			} else if (localName.equals("xmlContent")) {
				m_dsXMLBuffer=new StringBuffer();
				m_dsFirstElementBuffer=new StringBuffer();
				m_dsPrefixes=new ArrayList();
				m_xmlDataLevel=0;
				m_inXMLMetadata=true;
				m_firstInlineXMLElement=true;
            } else if (localName.equals("contentLocation")) {
				//String dsLocationType=grab(a, F, "TYPE");
                String dsLocation=grab(a,F,"REF");
                if (dsLocation==null || dsLocation.equals("")) {
                    throw new SAXException("REF attribute must be specified in contentLocation element");
                }
                // check if datastream is ExternalReferenced
                if (m_dsControlGrp.equalsIgnoreCase("E") ||
                    m_dsControlGrp.equalsIgnoreCase("R") )
                {
                  try {
                    m_dsLocation=new URL(dsLocation);
                  } catch (MalformedURLException murle) {
                    throw new SAXException("REF specifies malformed url: " + dsLocation);
                  }
                  m_dsLocationType="URL";
                  populateDatastream(new DatastreamReferencedContent(), dsLocation);
				  // check if datastream is ManagedContent
                } else if (m_dsControlGrp.equalsIgnoreCase("M")) {
                  // Validate ManagedContent dsLocation URL only if this
                  // is initial creation of this object; upon subsequent
                  // invocations, initial URL will have been replaced with
                  // an internal identifier for the dsLocation and will
                  // no longer be a URL.
                  if (m_obj.isNew())
                  {
                    try {
                      m_dsLocation=new URL(dsLocation);
                    } catch (MalformedURLException murle) {
                      throw new SAXException("REF specifies malformed url: " + dsLocation);
                    }
                  }                 
                  m_dsLocationType="INTERNAL_ID";
				  populateDatastream(new DatastreamManagedContent(), dsLocation);
                }
            } else if (localName.equals("binaryContent")) {
                // signal that we want to suck it in
				// FIXME: implement support for this in Fedora 1.2
				m_readingBinaryContent=true;
            }
        } else {
        	// process inline XML datastream content
            if (m_inXMLMetadata) {
                // we are inside an xmlContent element.
                // just output it, remembering the number of foxml:xmlContent elements we see
                String prefix=(String) m_prefixes.get(uri);
                if (m_firstInlineXMLElement) {
                // deal with root element... buffer it separately so we can 
                // add namespace stuff after it's known
                    m_firstInlineXMLElement=false;
                    m_dsFirstElementBuffer.append('<');
                    if (prefix!=null && !prefix.equals("")) {
                        if (!m_dsPrefixes.contains(prefix)) {
                            m_dsPrefixes.add(prefix);
                        }
                        m_dsFirstElementBuffer.append(prefix);
                        m_dsFirstElementBuffer.append(':');
                    }
                    m_dsFirstElementBuffer.append(localName);
                } else {
                // deal with non-root elements
                    m_dsXMLBuffer.append('<');
                    if (prefix!=null && !prefix.equals("")) {
                        if (!m_dsPrefixes.contains(prefix)) {
                            if (!"".equals(prefix)) {
                                m_dsPrefixes.add(prefix);
                            }
                        }
                        m_dsXMLBuffer.append(prefix);
                        m_dsXMLBuffer.append(':');
                    }
                    m_dsXMLBuffer.append(localName);
                }
                // deal with attributes
                for (int i=0; i<a.getLength(); i++) {
                    m_dsXMLBuffer.append(' ');
                    String aPrefix=(String) m_prefixes.get(a.getURI(i));
                    if (aPrefix!=null && !aPrefix.equals("")) {
                        if (!m_dsPrefixes.contains(aPrefix)) {
                            if (!"".equals(aPrefix)) {
                                m_dsPrefixes.add(aPrefix);
                            }
                        }
                        m_dsXMLBuffer.append(aPrefix);
                        m_dsXMLBuffer.append(':');
                    }
                    m_dsXMLBuffer.append(a.getLocalName(i));
                    m_dsXMLBuffer.append("=\"");
                    // re-encode decoded standard entities (&, <, >, ", ')
                    m_dsXMLBuffer.append(StreamUtility.enc(a.getValue(i)));
                    m_dsXMLBuffer.append("\"");
                }
                m_dsXMLBuffer.append('>');
                if (uri.equals(F) && localName.equals("xmlContent")) {
                    m_xmlDataLevel++;
                }
                
                // SYSMETA datastream: 
                // pick up name of object extended property from current version
				if (m_dsId.equals("SYSMETA") && m_dsCurrent.equalsIgnoreCase("YES")) {
					// get name of extensible object property
					if (localName.equals("ext")) {
						m_extObjPropertyName=grab(a, uri, "propertyName");
					}
				}
				// AUDIT datastream: 
				// initialize new audit record object
				if (m_dsId.equals("AUDIT") && m_dsCurrent.equalsIgnoreCase("YES")) {
					if (localName.equals("record")) {
						m_auditRec=new AuditRecord();
					} else if (localName.equals("process")) {
						m_auditProcessType=grab(a, uri, "type");
					}
					

				}
            } else {
                // ignore all else
            }
        }
    }


    public void characters(char[] ch, int start, int length) {
		// read normal element content into a string buffer
		if (m_elementContent !=null){
			m_elementContent.append(ch, start, length);
		}
		// read entire inline XML metadata chunks into a buffer
        if (m_inXMLMetadata) {
			// since this data is encoded straight back to xml,
			// we need to make sure special characters &, <, >, ", and '
			// are re-converted to the xml-acceptable equivalents.
			StreamUtility.enc(ch, start, length, m_dsXMLBuffer);
        } else if (m_readingBinaryContent) {
            // append it to something...
            // SDP: IMPLEMENT HERE FOR V1.2?
        }
    }

    public void endElement(String uri, String localName, String qName) {
        if (m_inXMLMetadata) {
            if (uri.equals(F) && localName.equals("xmlContent") && m_xmlDataLevel==0) {
                // create the right class of datastream and add it to m_obj
                for (int i=0; i<m_dsPrefixes.size(); i++) {
                    // now finish writing to m_dsFirstElementBuffer, a series of strings like
                    // ' xmlns:PREFIX="URI"'
                    String pfx=(String) m_dsPrefixes.get(i);
                    String pfxUri=(String) m_prefixUris.get(pfx);
                    if (!pfx.equals("")) {
                        m_dsFirstElementBuffer.append(" xmlns:");
                        m_dsFirstElementBuffer.append(pfx);
                    } else {
                        m_dsFirstElementBuffer.append(" xmlns");
                    }
                    m_dsFirstElementBuffer.append("=\"");
                    m_dsFirstElementBuffer.append(pfxUri);
                    m_dsFirstElementBuffer.append("\"");
                }
                DatastreamXMLMetadata ds=new DatastreamXMLMetadata();
                try {
                    String combined=m_dsFirstElementBuffer.toString() + m_dsXMLBuffer.toString();
                    ds.xmlContent=combined.getBytes(
                            m_characterEncoding);
                } catch (UnsupportedEncodingException uee) {
                  System.out.println("oops..encoding not supported, this could have been caught earlier.");
                }
                // set the attrs common to all datastreams
                ds.DatastreamID=m_dsId;
                ds.DatastreamURI=m_dsURI;
				ds.isVersionable=m_dsVersionable;
				ds.FormatURI=m_dsFormatURI;
                ds.DSVersionID=m_dsVersId;
                ds.DSLabel=m_dsLabel;
                if (m_dsMimeType==null) {
                    ds.DSMIME="text/xml";
                } else {
                    ds.DSMIME=m_dsMimeType;
                }
                ds.DSCreateDT=m_dsCreateDate;
                ds.DSSize=ds.xmlContent.length; // bytes, not chars, but
                                                // probably N/A anyway
                //ds.DSControlGrp=Datastream.XML_METADATA;
                ds.DSControlGrp="X";
                // DSInfoType and DSMCClass are METS-only.
                //ds.DSInfoType="OTHER";
                //ds.DSMDClass=m_dsMDClass;
                ds.DSState=m_dsState;
                ds.DSLocation=m_obj.getPid() + "+" + m_dsId + "+"
                    + m_dsVersId;
                // add it to the digitalObject
                m_obj.datastreams(m_dsId).add(ds);
                m_inXMLMetadata=false; // other stuff is re-initted upon
                                       // startElement for next xml metadata
                                       // element

            } else {
                // finish an element within the inline xml metadata... print end tag,
                // subtracting the level of the xmlContent elements we're at
                // if needed
                m_dsXMLBuffer.append("</");
                String prefix=(String) m_prefixes.get(uri);
                if (prefix!=null && !prefix.equals("")) {
                    m_dsXMLBuffer.append(prefix);
                    m_dsXMLBuffer.append(':');
                }
                m_dsXMLBuffer.append(localName);
                m_dsXMLBuffer.append(">");
                if (uri.equals(F) && localName.equals("xmlContent")) {
                    m_xmlDataLevel--;
                }
                
				// SYSMETA datastream: 
				// pick up system metadata stuff from the current ds version
				if (m_dsId.equals("SYSMETA") && m_dsCurrent.equalsIgnoreCase("YES")) {
	                // get element value if we are processing a SYSMETA datastream
					if (localName.equals("contentModelID")) {
						m_obj.setContentModelId(m_elementContent.toString());
					} else if (localName.equals("URI")) {
						m_obj.setURI(m_elementContent.toString());
					} else if (localName.equals("state")) {
						m_obj.setState(m_elementContent.toString());
					} else if (localName.equals("label")) {
						m_obj.setLabel(m_elementContent.toString());
					} else if (localName.equals("objectType")) {
						String objType=m_elementContent.toString();
						if (objType==null) { objType="FedoraObject"; }
						if (objType.equalsIgnoreCase("FedoraBDefObject")) {
							m_obj.setFedoraObjectType(DigitalObject.FEDORA_BDEF_OBJECT);
						} else if (objType.equalsIgnoreCase("FedoraBMechObject")) {
							m_obj.setFedoraObjectType(DigitalObject.FEDORA_BMECH_OBJECT);
						} else {
							m_obj.setFedoraObjectType(DigitalObject.FEDORA_OBJECT);
						}
					} else if (localName.equals("createdDate")) {
						m_obj.setCreateDate(
							DateUtility.convertStringToDate(m_elementContent.toString()));
					} else if (localName.equals("modifiedDate")) {
						m_obj.setLastModDate(
							DateUtility.convertStringToDate(m_elementContent.toString()));
					} else if (localName.equals("ext")) {
						m_obj.setProperty(m_extObjPropertyName, m_elementContent.toString());
					}
				}
				// AUDIT datastream: 
				// pick up audit records from the current ds version
				if (m_dsId.equals("AUDIT") && m_dsCurrent.equalsIgnoreCase("YES")) {
					if (localName.equals("action")) {
						m_auditAction=m_elementContent.toString();
					} else if (localName.equals("componentID")) {
						m_auditComponentID=m_elementContent.toString();
					} else if (localName.equals("responsibility")) {
						m_auditResponsibility=m_elementContent.toString();
					} else if (localName.equals("date")) {
						m_auditDate=m_elementContent.toString();
					} else if (localName.equals("justification")) {
						m_auditJustification=m_elementContent.toString();
					} else if (localName.equals("record")) {
						m_auditRec.processType=m_auditProcessType;
						m_auditRec.action=m_auditAction;
						m_auditRec.componentID=m_auditComponentID;
						m_auditRec.responsibility=m_auditResponsibility;
						m_auditRec.date=DateUtility.convertStringToDate(m_auditDate);
						m_auditRec.justification=m_auditJustification;
						// add the audit records to the digital object
						m_obj.getAuditRecords().add(m_auditRec);
						// reinit variables for next audit record
						m_auditProcessType=null;
						m_auditAction=null;
						m_auditComponentID=null;
						m_auditResponsibility=null;
						m_auditDate=null;
						m_auditJustification=null;
					}
				}					
            }
        // NOT m_inXMLMetadata ...
        } else {
			if (m_readingBinaryContent) {
				// FIXME: Implement for version 1.2?
				if (uri.equals(F) && localName.equals("binaryContent")) {
					m_readingBinaryContent=false;
				}
			}
        }
    }

    private static String grab(Attributes a, String namespace,
            String elementName) {
        String ret=a.getValue(namespace, elementName);
        if (ret==null) {
            ret=a.getValue(elementName);
        }
        return ret;
    }
    
    private void populateDatastream(Datastream ds, String dsLocation) {
    	
		ds.DatastreamID=m_dsId;
		ds.DatastreamURI=m_dsURI;
		ds.isVersionable=m_dsVersionable;
		ds.FormatURI=m_dsFormatURI;
		ds.DSVersionID=m_dsVersId;
		ds.DSLabel=m_dsLabel;
		ds.DSCreateDT=m_dsCreateDate;
		ds.DSMIME=m_dsMimeType;
		ds.DSControlGrp=m_dsControlGrp;
		//ds.DSInfoType="OTHER";
		ds.DSState=m_dsState;
		ds.DSLocation=dsLocation;
		// SDP: what is this about?  It does not set mime and size anyhow?
		/*
		if (m_queryBehavior!=QUERY_NEVER) {
		  if ((m_queryBehavior==QUERY_ALWAYS) || (m_dsMimeType==null)
			  || (m_dsSize==-1)) {
			try {
			  InputStream in=ds.getContentStream();
			} catch (StreamIOException sioe) {
			  throw new SAXException(sioe.getMessage());
			}
		  }
		}
		*/
		
		// SDP: this is METS specific stuff.  What to do?
		/*
		if (m_dsDmdIds!=null) {
		  for (int idi=0; idi<m_dsDmdIds.length; idi++) {
			drc.metadataIdList().add(m_dsDmdIds[idi]);
		  }
		}
		*/
		m_obj.datastreams(m_dsId).add(ds);   	
    }
  }
