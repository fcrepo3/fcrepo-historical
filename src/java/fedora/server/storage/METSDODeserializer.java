package fedora.server.storage;

import fedora.server.errors.ObjectIntegrityException;
import fedora.server.errors.StreamIOException;
import fedora.server.errors.StreamReadException;
import fedora.server.storage.types.AuditRecord;
import fedora.server.storage.types.DigitalObject;
import fedora.server.storage.types.Datastream;
import fedora.server.storage.types.DatastreamContent;
import fedora.server.storage.types.DatastreamReferencedContent;
import fedora.server.storage.types.DatastreamXMLMetadata;
import fedora.server.storage.types.Disseminator;
import fedora.server.storage.types.DSBindingMap;
import fedora.server.storage.types.DSBinding;
import fedora.server.utilities.DateUtility;
import fedora.server.utilities.StreamUtility;

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
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Reads an XML stream that is similar to a METS v1_1 document into a
 * Fedora DigitalObject.
 * <p></p>
 * See <a href="METSDOSerializer.html">METSDOSerializer</a> for details on
 * the differences between the XML encoding used here and the
 * METS schema.
 *
 * @author cwilper@cs.cornell.edu
 */
public class METSDODeserializer
        extends DefaultHandler
        implements DODeserializer {

    /** The namespace for METS */
    private final static String M="http://www.loc.gov/METS/";

    /** The namespace for XLINK */
    private final static String XLINK_NAMESPACE="http://www.w3.org/TR/xlink";
    // Mets says the above, but the spec at http://www.w3.org/TR/xlink/
    // says it's http://www.w3.org/1999/xlink


    private SAXParser m_parser;
    private String m_characterEncoding;

    /** The object to deserialize to. */
    private DigitalObject m_obj;

    /**
     * URI-to-namespace prefix mapping info from SAX2 startPrefixMapping events.
     */
    private HashMap m_prefixes;

    private boolean m_rootElementFound;
    private String m_dsId;
    private String m_dsVersId;
    private Date m_dsCreateDate;
    private String m_dsState;
    private String m_dsInfoType;
    private String m_dsLabel;
    private int m_dsMDClass;
    private long m_dsSize;
    private URL m_dsLocation;
    private String m_dsMimeType;
    private String m_dsControlGrp;

    // key=dsId, value=List of datastream ids (strings)
    private HashMap m_dsAdmIds; // these are saved till end of parse
    private String[] m_dsDmdIds; // these are only saved while parsing cur ds


    private StringBuffer m_dsXMLBuffer;

    // are we reading binary in an FContent element? (base64-encoded)
    private boolean m_readingContent;

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
    private StringBuffer m_auditBuffer;
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
    private boolean m_indiv;

    /** The structMapId of the dissem currently being parsed. */
    private String m_structId;
    
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

    public METSDODeserializer()
            throws FactoryConfigurationError, ParserConfigurationException,
            SAXException, UnsupportedEncodingException {
        this("UTF-8", false, QUERY_NEVER);
    }

    /**
     * Initializes by setting up a parser that doesn't validate and never
     * queries the server for values of DSSize and DSMIME.
     */
    public METSDODeserializer(String characterEncoding)
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
    public METSDODeserializer(String characterEncoding, boolean validate,
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

    public void deserialize(InputStream in, DigitalObject obj, String encoding)
            throws ObjectIntegrityException, StreamIOException, UnsupportedEncodingException {
        m_obj=obj;
        m_rootElementFound=false;
        m_dsId=null;
        m_dsVersId=null;
        m_dsCreateDate=null;
        m_dsState=null;
        m_dsInfoType=null;
        m_dsLabel=null;
        m_dsXMLBuffer=null;
        m_prefixes=new HashMap();
        m_dsAdmIds=new HashMap();
        m_dsDmdIds=null;
        m_dissems=new HashMap();
        try {
            m_parser.parse(in, this);
        } catch (IOException ioe) {
            throw new StreamIOException("low-level stream io problem occurred "
                    + "while sax was parsing this object.");
        } catch (SAXException se) {
            throw new ObjectIntegrityException("mets stream was bad : " + se.getMessage());
        }
        if (!m_rootElementFound) {
            throw new ObjectIntegrityException("METS root element not found -- must have 'mets' element in namespace " + M + " as root element.");
        }
        obj.setNamespaceMapping(m_prefixes);
        // foreach datastream: ArrayList ids=(ArrayList) m_dsAdmIds.get(m_dsVersId);
        // put the admids in the correct place (metadata or auditrecs) for each datastream
        Iterator dsIdIter=obj.datastreamIdIterator();
        while (dsIdIter.hasNext()) {
            List datastreams=obj.datastreams((String) dsIdIter.next());
            for (int i=0; i<datastreams.size(); i++) {
                Datastream ds=(Datastream) datastreams.get(i);
                List admIdList=(List) m_dsAdmIds.get(ds.DSVersionID);
                if (admIdList!=null) {
                    // it's got admids..cool.  decide where to put em based
                    // on the types of their targets.
                    Iterator admIdIter=admIdList.iterator();
                    while (admIdIter.hasNext()) {
                       String admId=(String) admIdIter.next();
                       List targetDatastreamSequence=obj.datastreams(admId);
                       if (targetDatastreamSequence.size()>0) {
                           // it's definitely a datastream.. assume it's admin
                           try {
                               DatastreamContent dsc=(DatastreamContent) ds;
                               dsc.metadataIdList().add(admId);
                           } catch (ClassCastException cce) {
                               throw new ObjectIntegrityException(
                                       "Metadata datastream can't use ADMID to"
                                       + " point to non-AuditRecord "
                                       + "datastream(s).");
                           }
                       } else {
                           // it's not a datastram..assume it's an audit record
                           ds.auditRecordIdList().add(admId);
                       }
                    }
                }
            }
        }
        // put dissems in obj
        Iterator dissemIter=m_dissems.values().iterator();
        while (dissemIter.hasNext()) {
            Disseminator diss=(Disseminator) dissemIter.next();
            obj.disseminators(diss.dissID).add(diss);
        }
    }

    public void startPrefixMapping(String prefix, String uri) {
        m_prefixes.put(uri, prefix);
    }

    public void startElement(String uri, String localName, String qName,
            Attributes a) throws SAXException {
        if (uri.equals(M) && !m_inXMLMetadata) {
            // a new mets element is starting
            if (localName.equals("mets")) {
                m_rootElementFound=true;
                m_obj.setPid(grab(a, M, "OBJID"));
                m_obj.setLabel(grab(a, M, "LABEL"));
                m_obj.setContentModelId(grab(a, M, "PROFILE"));
                String objType=grab(a, M, "TYPE");
                if (objType==null) { objType="FedoraObject"; }
                if (objType.equalsIgnoreCase("FedoraBDefObject")) {
                    m_obj.setFedoraObjectType(DigitalObject.FEDORA_BDEF_OBJECT);
                } else if (objType.equalsIgnoreCase("FedoraBMechObject")) {
                    m_obj.setFedoraObjectType(DigitalObject.FEDORA_BMECH_OBJECT);
                } else {
                    m_obj.setFedoraObjectType(DigitalObject.FEDORA_OBJECT);
                }
            } else if (localName.equals("metsHdr")) {
                m_obj.setCreateDate(DateUtility.convertStringToDate(
                        grab(a, M, "CREATEDATE")));
                m_obj.setLastModDate(DateUtility.convertStringToDate(
                        grab(a, M, "LASTMODDATE")));
                m_obj.setState(grab(a, M, "RECORDSTATUS"));
            } else if (localName.equals("amdSec")) {
                m_dsId=grab(a, M, "ID");
            } else if (localName.equals("techMD") || localName.equals("dmdSec")
                    || localName.equals("sourceMD")
                    || localName.equals("rightsMD")
                    || localName.equals("digiprovMD")) {
                m_dsVersId=grab(a, M, "ID");
                if (localName.equals("techMD")) {
                    m_dsMDClass=DatastreamXMLMetadata.TECHNICAL;
                }
                if (localName.equals("sourceMD")) {
                    m_dsMDClass=DatastreamXMLMetadata.SOURCE;
                }
                if (localName.equals("rightsMD")) {
                    m_dsMDClass=DatastreamXMLMetadata.RIGHTS;
                }
                if (localName.equals("digiprovMD")) {
                    m_dsMDClass=DatastreamXMLMetadata.DIGIPROV;
                }
                if (localName.equals("dmdSec")) {
                    m_dsMDClass=DatastreamXMLMetadata.DESCRIPTIVE;
                    // dmdsec metadata has primary id in GROUPID attribute
                    // whereas amdSec metadata has an outerlying element
                    // that includes the primary id in an ID attribute
                    m_dsId=grab(a, M, "GROUPID");
                }
                m_dsCreateDate=DateUtility.convertStringToDate(
                        grab(a, M, "CREATED"));
                m_dsState=grab(a, M, "STATUS");
            } else if (localName.equals("mdWrap")) {
                m_dsInfoType=grab(a, M, "MDTYPE");
                m_dsLabel=grab(a, M, "LABEL");
            } else if (localName.equals("xmlData")) {
                m_dsXMLBuffer=new StringBuffer();
                m_dsPrefixes=new ArrayList();
                m_xmlDataLevel=0;
                m_inXMLMetadata=true;
            } else if (localName.equals("fileGrp")) {
                m_dsId=grab(a, M, "ID");
                // reset the values for the next file
                m_dsVersId=null;
                m_dsCreateDate=null;
                m_dsMimeType=null;
                m_dsControlGrp=null;
                m_dsDmdIds=null;
                m_dsState=null;
                m_dsSize=-1;
            } else if (localName.equals("file")) {
                // ID="DS3.0"
                // CREATED="2002-05-20T06:32:00"
                // MIMETYPE="image/jpg"
                // ADMID="TECH3"
                // OWNERID="E" // ignored this is determinable otherwise
                // STATUS=""
                // SIZE="bytes"
                m_dsVersId=grab(a, M, "ID");
                m_dsCreateDate=DateUtility.convertStringToDate(
                        grab(a,M,"CREATED"));
                m_dsMimeType=grab(a,M,"MIMETYPE");
                m_dsControlGrp=grab(a,M,"OWNERID");
                String ADMID=grab(a,M,"ADMID");
                if ((ADMID!=null) && (!"".equals(ADMID))) {
                    // remember admids for when we're finished...
                    // we can't reliably determine yet whether they are
                    // metadata refs or audit record refs, since we can't
                    // rely on having gone through all audit records yet.
                    ArrayList al=new ArrayList();
                    if (ADMID.indexOf(" ")!=-1) {
                        String[] admIds=ADMID.split(" ");
                        for (int idi=0; idi<admIds.length; idi++) {
                            al.add(admIds[idi]);
                        }
                    } else {
                        al.add(ADMID);
                    }
                    m_dsAdmIds.put(m_dsVersId, al);
                }
                String DMDID=grab(a,M,"DMDID");
                if ((DMDID!=null) && (!"".equals(DMDID))) {
                    // saved till we're makin' the datastream object
                    if (DMDID.indexOf(" ")!=-1) {
                        m_dsDmdIds=DMDID.split(" ");
                    } else {
                        m_dsDmdIds=new String[] {DMDID};
                    }
                }
                m_dsState=grab(a,M,"STATUS");
                String sizeString=grab(a,M,"SIZE");
                if (sizeString!=null && !sizeString.equals("")) {
                    try {
                        m_dsSize=Long.parseLong(sizeString);
                    } catch (NumberFormatException nfe) {
                        throw new SAXException("If specified, a datastream's "
                                + "SIZE attribute must be an xsd:long.");
                    }
                }
                // inside a "file" element, it's either going to be
                // FLocat (a reference) or FContent (inline)
            } else if (localName.equals("FLocat")) {
                // xlink:href="http://icarus.lib.virginia.edu/dic/colls/archive/screen/aict/006-007.jpg"
                // xlink:title="Saskia high jpg image"/>
                m_dsLabel=grab(a,XLINK_NAMESPACE,"title");
                String dsLocation=grab(a,XLINK_NAMESPACE,"href");
                if (dsLocation==null || dsLocation.equals("")) {
                    throw new SAXException("xlink:href must be specified in FLocat element");
                }
                try {
                    m_dsLocation=new URL(dsLocation);
                } catch (MalformedURLException murle) {
                    throw new SAXException("xlink:href specifies malformed url: " + dsLocation);
                }
                DatastreamReferencedContent d=new DatastreamReferencedContent();
                d.DatastreamID=m_dsId;
                d.DSVersionID=m_dsVersId;
                d.DSLabel=m_dsLabel;
                d.DSCreateDT=m_dsCreateDate;
                d.DSMIME=m_dsMimeType;
                d.DSControlGrp=m_dsControlGrp;
                //d.DSControlGrp=Datastream.EXTERNAL_REF;
                d.DSInfoType="DATA";
                d.DSState=m_dsState;
                d.DSLocation=m_dsLocation;
                if (m_queryBehavior!=QUERY_NEVER) {
                    if ((m_queryBehavior==QUERY_ALWAYS) || (m_dsMimeType==null)
                            || (m_dsSize==-1)) {
                        try {
                            InputStream in=d.getContentStream();
                        } catch (StreamIOException sioe) {
                            throw new SAXException(sioe.getMessage());
                        }
                    }
                }
                if (m_dsDmdIds!=null) {
                    for (int idi=0; idi<m_dsDmdIds.length; idi++) {
                        d.metadataIdList().add(m_dsDmdIds[idi]);
                    }
                }
                m_obj.datastreams(m_dsId).add(d);
            } else if (localName.equals("FContent")) {
                // signal that we want to suck it in
                m_readingContent=true;
            } else if (localName.equals("structMap")) {
                // this is a component of a disseminator.  here we assume the rest
                // of the disseminator's information will be seen later, so we
                // construct a new Disseminator object to hold the structMap...
                // and later, the other info
                //
                // Building up a global map of Disseminators, m_dissems, 
                // keyed by bindingmap ID.
                //
                if (grab(a,M,"TYPE").equals("fedora:dsBindingMap")) {
                    String bmId=grab(a,M,"ID");
                    if ( (bmId==null) || (bmId.equals("")) ) {
                        throw new SAXException("structMap with TYPE fedora:dsBindingMap must specify a non-empty ID attribute.");
                    } else {
                        Disseminator diss=new Disseminator();
                        diss.dsBindMapID=bmId;
                        m_dissems.put(bmId,diss);
                        m_diss=diss;
                        m_diss.dsBindMap=new DSBindingMap();
                        m_diss.dsBindMap.dsBindMapID=bmId;
                        m_indiv=false; // flag that we're not looking at inner part yet
                    }
                } else {
                    throw new SAXException("StructMap must have TYPE fedora:dsBindingMap");
                }
            } else if (localName.equals("div")) {
                if (m_indiv) {
                    // inner part of structmap
                    DSBinding binding=new DSBinding();
                    if (m_diss.dsBindMap.dsBindings==null) {
                        // none yet.. create array of size one
                        DSBinding[] bindings=new DSBinding[1];
                        m_diss.dsBindMap.dsBindings=bindings;
                        m_diss.dsBindMap.dsBindings[0]=binding;
                    } else {
                        // need to expand the array size by one,
                        // and do an array copy.
                        int curSize=m_diss.dsBindMap.dsBindings.length;
                        DSBinding[] oldArray=m_diss.dsBindMap.dsBindings;
                        DSBinding[] newArray=new DSBinding[curSize+1];
                        for (int i=0; i<curSize; i++) {
                            newArray[i]=oldArray[i];
                        }
                        newArray[curSize]=binding;
                        m_diss.dsBindMap.dsBindings=newArray;
                    }
                    // now populate 'binding' values...we'll have
                    // everything at this point except datastreamID...
                    // that comes as a child: <fptr FILEID="DS2"/>
                    binding.bindKeyName=grab(a,M,"TYPE");
                    binding.bindLabel=grab(a,M,"LABEL");
                    binding.seqNo=grab(a,M,"ORDER");
                } else {
                    m_indiv=true;
                    // first (outer div) part of structmap
                    // TYPE="test:2" LABEL="DS Binding Map for UVA Standard Image mechanism"
                    m_diss.dsBindMap.dsBindMechanismPID=grab(a,M,"TYPE");
                    m_diss.dsBindMap.dsBindMapLabel=grab(a,M,"LABEL");
                }
            } else if (localName.equals("fptr")) {
                // assume we're inside the inner div... that's the
                // only place the fptr element is valid.
                DSBinding binding=m_diss.dsBindMap.dsBindings[
                        m_diss.dsBindMap.dsBindings.length-1];
                binding.datastreamID=grab(a,M,"FILEID");
            } else if (localName.equals("behaviorSec")) {
                // looks like we're in a disseminator... it should be in the
                // hash by now because we've already gone through structmaps
                // ...keyed by structmap id... which is the behaviorSec
                // STRUCTID attribute... and remember the structmap id
                // so we can grab the right dissem when parsing children
                m_structId=grab(a,M,"STRUCTID");
                Disseminator dissem=(Disseminator) m_dissems.get(m_structId);
                // ID="DISS1.0" STRUCTID="S1" BTYPE="test:1" CREATED="2002-05-20T06:32:00" 
                // LABEL="UVA Std Image Behaviors" GROUPID="DISS1" STATUS=""
                dissem.dissVersionID=grab(a,M,"ID");
                dissem.bDefID=grab(a,M,"BTYPE");
                dissem.dissCreateDT=DateUtility.convertStringToDate(grab(a,M,"CREATED"));
                dissem.dissLabel=grab(a,M,"LABEL");
                dissem.dissID=grab(a,M,"GROUPID");
                dissem.dissState=grab(a,M,"STATUS");
            } else if (localName.equals("interfaceDef")) {
                // interfaceDef LABEL="UVA Std Image Behavior Definition" 
                // LOCTYPE="URN" xlink:href="test:1"/>
                Disseminator dissem=(Disseminator) m_dissems.get(m_structId);
                // already have the id from containing element, just need label
                dissem.bDefLabel=grab(a,M,"LABEL");
            } else if (localName.equals("mechanism")) {
                Disseminator dissem=(Disseminator) m_dissems.get(m_structId);
                dissem.bMechLabel=grab(a,M,"LABEL");
                dissem.bMechID=grab(a,XLINK_NAMESPACE,"href");
            }
        } else {
            if (m_inXMLMetadata) {
                // must be in xmlData... just output it, remembering the number
                // of METS:xmlData elements we see
                m_dsXMLBuffer.append('<');
                String prefix=(String) m_prefixes.get(uri);
                if (prefix!=null) {
                    if (!m_dsPrefixes.contains(prefix)) {
                        if (!"".equals(prefix)) {
                            m_dsPrefixes.add(prefix);
                        }
                    }
                    m_dsXMLBuffer.append(prefix);
                    m_dsXMLBuffer.append(':');
                }
                m_dsXMLBuffer.append(localName);
                for (int i=0; i<a.getLength(); i++) {
                    m_dsXMLBuffer.append(' ');
                    String aPrefix=(String) m_prefixes.get(a.getURI(i));
                    if (aPrefix!=null) {
                        if (!m_dsPrefixes.contains(prefix)) {
                            if (!"".equals(prefix)) {
                                m_dsPrefixes.add(prefix);
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
                if (uri.equals(M) && localName.equals("xmlData")) {
                    m_xmlDataLevel++;
                }
                // remember this stuff... (we don't have to look at level
                // because the audit schema doesn't allow for xml elements inside
                // these, so they're never set incorrectly)
                // signaling that we're interested in sending char data to
                // the m_auditBuffer by making it non-null, and getting
                // ready to accept data by allocating a new StringBuffer
                if (m_dsId.equals("FEDORA-AUDITTRAIL")) {
                    if (localName.equals("process")) {
                        m_auditProcessType=grab(a, uri, "type");
                    } else if ( (localName.equals("action"))
                            || (localName.equals("responsibility"))
                            || (localName.equals("date"))
                            || (localName.equals("justification")) ) {
                        m_auditBuffer=new StringBuffer();
                    }
                }
            } else {
                // ignore all else
            }
        }
    }

    public void characters(char[] ch, int start, int length) {
        if (m_inXMLMetadata) {
            if (m_auditBuffer!=null) {
                m_auditBuffer.append(ch, start, length);
            } else {
                // since this data is encoded straight back to xml,
                // we need to make sure special characters &, <, >, ", and '
                // are re-converted to the xml-acceptable equivalents.
                StreamUtility.enc(ch, start, length, m_dsXMLBuffer);
            }
        } else if (m_readingContent) {
            // append it to something...
        }
    }

    public void endElement(String uri, String localName, String qName) {
        m_readingContent=false;
        if (m_inXMLMetadata) {
            if (uri.equals(M) && localName.equals("xmlData")
                    && m_xmlDataLevel==0) {
                // finished all xml metadata for this datastream
                if (m_dsId.equals("FEDORA-AUDITTRAIL")) {
                    // we've been looking at an audit trail...
                    // m_auditProcessType, m_auditAction,
                    // m_auditResponsibility, m_auditDate, m_auditJustification
                    // should all be set
                    AuditRecord a=new AuditRecord();
                    a.id=m_dsVersId; // it's like the FEDORA-AUDITTRAIL is a
                                     // datastream and the records are versions
                    a.processType=m_auditProcessType;
                    a.action=m_auditAction;
                    a.responsibility=m_auditResponsibility;
                    a.date=DateUtility.convertStringToDate(m_auditDate);
                    a.justification=m_auditJustification;
                    m_obj.getAuditRecords().add(a);
                } else {
                    // create the right kind of datastream and add it to m_obj
                    String[] prefixes=new String[m_dsPrefixes.size()];
                    for (int i=0; i<m_dsPrefixes.size(); i++) {
                        prefixes[i]=(String) m_dsPrefixes.get(i);
                    }
                    DatastreamXMLMetadata ds=new DatastreamXMLMetadata();
                    // set the attrs specific to XML_METADATA datastreams
                    ds.namespacePrefixes=prefixes;
                    try {
                        ds.xmlContent=m_dsXMLBuffer.toString().getBytes(
                                m_characterEncoding);
                    } catch (UnsupportedEncodingException uee) {
                      System.out.println("oops..encoding not supported, this could have been caught earlier.");
                    }
                    // set the attrs common to all datastreams
                    ds.DatastreamID=m_dsId;
                    ds.DSVersionID=m_dsVersId;
                    ds.DSLabel=m_dsLabel;
                    ds.DSMIME="text/xml";
                    ds.DSCreateDT=m_dsCreateDate;
                    ds.DSSize=ds.xmlContent.length; // bytes, not chars, but
                                                    // probably N/A anyway
                    //ds.DSControlGrp=Datastream.XML_METADATA;
                    ds.DSControlGrp="X";
                    ds.DSInfoType=m_dsInfoType;
                    ds.DSMDClass=m_dsMDClass;
                    ds.DSState=m_dsState;
                    ds.DSLocation=null;  // N/A
                    // add it to the digitalObject
                    m_obj.datastreams(m_dsId).add(ds);
                }
                m_inXMLMetadata=false; // other stuff is re-initted upon
                                       // startElement for next xml metadata
                                       // element

            } else {
                // finished an element in xml metadata... print end tag,
                // subtracting the level of METS:xmlData elements we're at
                // if needed
                m_dsXMLBuffer.append("</");
                String prefix=(String) m_prefixes.get(uri);
                if (prefix!=null) {
                    m_dsXMLBuffer.append(prefix);
                    m_dsXMLBuffer.append(':');
                }
                m_dsXMLBuffer.append(localName);
                m_dsXMLBuffer.append(">");
                if (uri.equals(M) && localName.equals("xmlData")) {
                    m_xmlDataLevel--;
                }
                if (m_dsId.equals("FEDORA-AUDITTRAIL")) {
                    if (localName.equals("action")) {
                        m_auditAction=m_auditBuffer.toString();
                        m_auditBuffer=null;
                    } else if (localName.equals("responsibility")) {
                        m_auditResponsibility=m_auditBuffer.toString();
                        m_auditBuffer=null;
                    } else if (localName.equals("date")) {
                        m_auditDate=m_auditBuffer.toString();
                        m_auditBuffer=null;
                    } else if (localName.equals("justification")) {
                        m_auditJustification=m_auditBuffer.toString();
                        m_auditBuffer=null;
                    }
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

}
