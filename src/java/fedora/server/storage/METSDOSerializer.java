package fedora.server.storage;

import fedora.server.errors.ObjectIntegrityException;
import fedora.server.errors.StreamIOException;
import fedora.server.errors.StreamWriteException;
import fedora.server.storage.types.AuditRecord;
import fedora.server.storage.types.DigitalObject;
import fedora.server.storage.types.Datastream;
import fedora.server.storage.types.DatastreamContent;
import fedora.server.storage.types.DatastreamReferencedContent;
import fedora.server.storage.types.DatastreamXMLMetadata;
import fedora.server.utilities.DateUtility;
import fedora.server.utilities.StreamUtility;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A DigitalObject serializer that outputs to a format similar to METS XML v1_1.
 * <p></p>
 * In order to support the following features of Fedora, we don't strictly 
 * adhere to the METS schema.
 * <p></p>
 * <h3>Inline XML Datastream Versioning for Descriptive Metadata</h3>
 * <dir>
 * All datastreams in a Fedora object can be versioned.  When the METS format
 * is used to encode Fedora objects, we go outside the METS schema because
 * it currently has no provision for versioning inline XML datastreams that
 * are descriptive metadata.  This exception to our use of the METS schema
 * is described below.
 * <p></p>
 * The METS schema doesn't allow for the GROUPID attribute on amdSec or dmdSec 
 * elements.  Now, because of the way we are encoding multi-versioned 
 * _administrative_ metadata[1], we are ok on this front.  But when it comes 
 * to _descriptive_ metadata, we are using a GROUPID attribute[2] that isn't 
 * METS schema-valid[3,4].
 * <p></p>
 * [1] The amdSec element allows for a set of metadata (where each is of type 
 * techMD, sourceMD, rightsMD, or digiprovMD). We put all versions of one 
 * administrative datastream into a single amdSec, where the ID is the 
 * datastream id, and each version of a specific datastream member is indicated
 * by it's surrounding techMD, sourceMD, rightsMD, or digiprovMD element's ID 
 * attribute.
 * <p></p>
 * [2] The dmdSec element may contain only one chunk of METS-element-surrounded 
 * metadata, so we create a separate dmdSec element for each version of a 
 * particular datastream.  Since they are not all grouped together in the METS 
 * document, we group them via a "GROUPID" attribute in the dmdSec
 * <p></p>
 * [3] The METS schema: (As of 2002-10-13 the version at this URL is 1.1)
 *     <a href="http://www.loc.gov/standards/mets/mets.xsd">http://www.loc.gov/standards/mets/mets.xsd</a>
 * <p></p>
 * [4] The Fedora tech. spec., v1.0, section 2.4, paragraph 2
 *     <a href="http://www.fedora.info/documents/master-spec.html#_Toc11835694">http://www.fedora.info/documents/master-spec.html#_Toc11835694</a>
 * </dir>
 * <p></p>
 * <h3>Describing Content Datastreams with Inline XML Metadata</h3>
 * <dir>
 * Fedora supports arbitrary content datastreams that can either be repository-
 * managed (internal to the repository) or referenced 
 * (external to the repository).  These datastreams can additionally be
 * described inside the object by one or more inline XML metadata datastreams.
 * <p></p>
 * We represent part of this "is described by" relationship in METS via the 
 * ADMID attribute in the file element for each content datastream.  This 
 * covers a content stream's relationship to administrative metadata, but not 
 * descriptive metadata.  We use an additional attribute in this element, DMDID,
 * to similarly represent a content stream's relationship with descriptive
 * metadata.  Used in this location in the METS document, the DMDID attribute
 * is not METS schema-valid.
 * 
 * </dir>
 *
 * @author cwilper@cs.cornell.edu
 */
public class METSDOSerializer 
        implements DOSerializer {

    // test object says this.. but it should be 
    // http://www.fedora.info/definitions/1/0/auditing/
    private final static String FEDORA_AUDIT_NAMESPACE_URI=
            "http://fedora.comm.nsdlib.org/audit";
    
    private String m_characterEncoding;

    /**
     * Constructs a METS serializer.
     *
     * @param characterEncoding The character encoding to use when sending
     *        the objects to OutputStreams.
     * @throw UnsupportedEncodingException If the provided encoding is
     *        not supported or recognized.
     */
    public METSDOSerializer(String characterEncoding) 
            throws UnsupportedEncodingException {
        m_characterEncoding=characterEncoding;
        StringBuffer buf=new StringBuffer();
        buf.append("test");
        byte[] temp=buf.toString().getBytes(m_characterEncoding);
    }
    
    public String getEncoding() {
        return m_characterEncoding;
    }

    // subclasses should override this
    public static String getVersion() {
        return "1.0";
    }

    /**
     * Serializes the given Fedora object to an OutputStream.
     */
    public void serialize(DigitalObject obj, OutputStream out) 
            throws ObjectIntegrityException, StreamIOException, 
            StreamWriteException {
        try {
            StringBuffer buf=new StringBuffer();
            //
            // Serialize root element and header
            //
            buf.append("<?xml version=\"1.0\" ");
            buf.append("encoding=\"");
            buf.append(m_characterEncoding);
            buf.append("\" ?>\n");
            buf.append("<mets xmlns=\"http://www.loc.gov/METS/\"\n");
            Iterator nsIter=obj.getNamespaceMapping().keySet().iterator();
            while (nsIter.hasNext()) {
                String uri=(String) nsIter.next();
                String prefix=(String) obj.getNamespaceMapping().get(uri);
                buf.append("    xmlns:");
                buf.append(prefix);
                buf.append("=\"");
                buf.append(uri);
                buf.append("\"\n");
            }
            buf.append("    OBJID=\"");
            buf.append(obj.getPid());
            buf.append("\"\n    LABEL=\"");
            StreamUtility.enc(obj.getLabel(), buf);
            buf.append("\"\n    TYPE=\"");
            buf.append("FedoraObject");
            buf.append("\"\n    PROFILE=\"");
            StreamUtility.enc(obj.getContentModelId(), buf);
            buf.append("\">\n  <metsHdr CREATEDATE=\"");
            buf.append(DateUtility.convertDateToString(obj.getCreateDate()));
            buf.append("\" LASTMODDATE=\"");
            buf.append(DateUtility.convertDateToString(obj.getLastModDate()));
            buf.append("\" RECORDSTATUS=\"");
            buf.append(obj.getState());
            buf.append("\">\n    <!-- This info can't be set via API-M -- if it existed, it was ignored during import -->\n");
            buf.append("  </metsHdr>\n");
            //
            // Serialize Audit Records
            //
            if (obj.getAuditRecords().size()>0) {
                buf.append("  <amdSec ID=\"FEDORA-AUDITTRAIL\">\n");
                String auditPrefix=(String) obj.getNamespaceMapping().get(FEDORA_AUDIT_NAMESPACE_URI);
                Iterator iter=obj.getAuditRecords().iterator();
                while (iter.hasNext()) {
                    AuditRecord audit=(AuditRecord) iter.next();
                    buf.append("    <digiprovMD ID=\"");
                    buf.append(audit.id);
                    buf.append("\" CREATED=\""); 
                    String createDate=DateUtility.convertDateToString(audit.date); 
                    buf.append(createDate);
                    buf.append("\" STATUS=\"A\">\n");  // status is always A 
                    buf.append("      <mdWrap MIMETYPE=\"text/xml\" MDTYPE=\"OTHER\" LABEL=\"Fedora Object Audit Trail Record\">\n");
                    buf.append("        <xmlData>\n");
                    buf.append("          <");
                    buf.append(auditPrefix);
                    buf.append(":record>\n");
                    buf.append("            <");
                    buf.append(auditPrefix);
                    buf.append(":process type=\"");
                    StreamUtility.enc(audit.processType, buf);
                    buf.append("\"/>\n");
                    
                    buf.append("            <");
                    buf.append(auditPrefix);
                    buf.append(":action>");
                    StreamUtility.enc(audit.action, buf);
                    buf.append("</");
                    buf.append(auditPrefix);
                    buf.append(":action>\n");
                    
                    buf.append("            <");
                    buf.append(auditPrefix);
                    buf.append(":responsibility>");
                    StreamUtility.enc(audit.responsibility, buf);
                    buf.append("</");
                    buf.append(auditPrefix);
                    buf.append(":responsibility>\n");
                    
                    buf.append("            <");
                    buf.append(auditPrefix);
                    buf.append(":date>");
                    buf.append(createDate);
                    buf.append("</");
                    buf.append(auditPrefix);
                    buf.append(":date>\n");
                    
                    buf.append("            <");
                    buf.append(auditPrefix);
                    buf.append(":justification>");
                    StreamUtility.enc(audit.justification, buf);
                    buf.append("</");
                    buf.append(auditPrefix);
                    buf.append(":justfication>\n");
                    
                    buf.append("          </");
                    buf.append(auditPrefix);
                    buf.append(":record>\n");
                    buf.append("        </xmlData>\n");
                    buf.append("      </mdWrap>\n");
                    buf.append("    </digiprovMD>\n");
                }
                buf.append("  </amdSec>\n");
            }
            //
            // Serialize Datastreams
            //
            Iterator idIter=obj.datastreamIdIterator();
            while (idIter.hasNext()) {
                String id=(String) idIter.next();
                // from the first one with this id, 
                // first decide if its an inline xml 
                Datastream ds=(Datastream) obj.datastreams(id).get(0);
                if (ds.DSControlGrp==Datastream.XML_METADATA) {
                    //
                    // Serialize inline XML datastream
                    // - dmdSec || amdSec?
                    //
                    DatastreamXMLMetadata mds=(DatastreamXMLMetadata) ds;
                    if (mds.DSMDClass==DatastreamXMLMetadata.DESCRIPTIVE) {
                        //
                        // Descriptive inline XML Metadata
                        //
                        // <!-- For each version with this dsId -->
                        // <dmdSec GROUPID=dsId
                        //              ID=dsVersionId
                        //         CREATED=dsCreateDate
                        //          STATUS=dsState>
                        //   <mdWrap....>
                        //     ...
                        //   </mdWrap>
                        // </dmdSec>
                        //
                        Iterator dmdIter=obj.datastreams(id).iterator();
                        while (dmdIter.hasNext()) {
                            mds=(DatastreamXMLMetadata) dmdIter.next();
                            buf.append("  <dmdSec ID=\"");
                            buf.append(mds.DSVersionID);
                            buf.append("\" GROUPID=\"");
                            buf.append(mds.DatastreamID);
                            buf.append("\" CREATED=\"");
                            buf.append(DateUtility.convertDateToString(
		                    mds.DSCreateDT));
                            buf.append("\" STATUS=\"");
                            buf.append(mds.DSState);
                            buf.append("\">\n");
                            mdWrap(mds, buf);
                            buf.append("  </dmdsec>\n");
                        }
                    } else {
               	        //
                        // Administrative inline XML Metadata
                        //
                        // Technical          ($mdClass$=techMD)
                        // Source             ($mdClass$=sourceMD)
                        // Rights             ($mdClass$=rightsMD)
                        // Digital Provenance ($mdClass$=digiprovMD)
                        // 
                        // <amdSec ID=dsId>
                        //   <!-- For each version with this dsId -->
                        //   <$mdClass$      ID=dsVersionId
                        //              CREATED=dsCreateDate
                        //               STATUS=dsState>
                        //     <mdWrap....>
                        //       ...
                        //     </mdWrap>
                        //   </techMd>
                        // </amdSec>
                        //
                        String mdClass;
                        if (mds.DSMDClass==DatastreamXMLMetadata.TECHNICAL) {
                            mdClass="techMD";
                        } else if (mds.DSMDClass==DatastreamXMLMetadata.SOURCE) {
                            mdClass="sourceMD";
                        } else if (mds.DSMDClass==DatastreamXMLMetadata.RIGHTS) {
                            mdClass="rightsMD";
                        } else if (mds.DSMDClass==DatastreamXMLMetadata.DIGIPROV) {
                            mdClass="digiprovMD";
                        } else {
                            throw new ObjectIntegrityException(
                                    "Datastreams must have a class");
                        }
                        buf.append("  <amdSec ID=\"");
                        buf.append(mds.DatastreamID);
                        buf.append("\">\n");
                        Iterator amdIter=obj.datastreams(id).iterator();
                        while (amdIter.hasNext()) {
                            mds=(DatastreamXMLMetadata) amdIter.next();
                            buf.append("    <");
                            buf.append(mdClass);
                            buf.append(" ID=\"");
                            buf.append(mds.DSVersionID);
                            buf.append("\" CREATED=\"");
                            buf.append(DateUtility.convertDateToString(
		                    mds.DSCreateDT));
                            buf.append("\" STATUS=\"");
                            buf.append(mds.DSState);
                            buf.append("\">\n");
                            mdWrap(mds, buf);
                            buf.append("    </");
                            buf.append(mdClass);
                            buf.append(">\n");
                        }
                        buf.append("  </amdsec>\n");
                    }
                } else {
                    //
                    // Externally-referenced or managed datastreams
                    //
                    buf.append("  <fileGrp ID=\"");
                    buf.append(ds.DatastreamID);
                    buf.append(">\n");
                    Iterator contentIter=obj.datastreams(id).iterator();
                    while (contentIter.hasNext()) {
                        DatastreamContent dsc=(DatastreamContent) contentIter.next();
                        buf.append("    <file ID=\"");
                        buf.append(dsc.DSVersionID);
                        buf.append("\" CREATED=\"");
                        buf.append(DateUtility.convertDateToString(dsc.DSCreateDT));
                        buf.append("\" MIMETYPE=\"");
                        buf.append(dsc.DSMIME);
                        buf.append("\" STATUS=\"");
                        buf.append(dsc.DSState);
                        buf.append("\" SIZE=\"" + dsc.DSSize);
                        buf.append("\" ADMIDS=\"");
                        Iterator admIdIter=getAdmIds(obj, dsc).iterator();
                        int admNum=0;
                        while (admIdIter.hasNext()) {
                            String admId=(String) admIdIter.next();
                            if (admNum>0) {
                                buf.append(' ');
                            }
                            buf.append(admId);
                            admNum++;
                        }
                        //
                        // other attrs
                        //
                        buf.append("\">\n");
                        if (dsc.DSControlGrp==Datastream.EXTERNAL_REF) {
                        } else {
                        }
                        buf.append("    </file>\n");
                    } 
                    buf.append("  </fileGrp>\n");
                }
            }
            //
            // Serialization Complete
            //
            buf.append("</mets>");
            out.write(buf.toString().getBytes(m_characterEncoding));
            out.flush();
        } catch (IOException ioe) {
            // this could be an unsupportedencodingexception, but it won't be 
            // because we already checked for that in the constructor
            throw new StreamWriteException("Problem writing to outputstream "
                + "while serializing to mets: " + ioe.getMessage());
        } finally {
            try {
                out.close();
            } catch (IOException ioe2) { 
                throw new StreamIOException("Problem closing outputstream "
                    + "after attempting to serialize to mets: " 
                    + ioe2.getMessage());
            }
        }
        if (1==2) throw new ObjectIntegrityException("bad object");
    }
    
    private List getAdmIds(DigitalObject obj, DatastreamContent content) {
        ArrayList ret=new ArrayList(content.auditRecordIdList());
        Iterator mdIdIter=content.metadataIdList().iterator();
        while (mdIdIter.hasNext()) {
            String mdId=(String) mdIdIter.next();
            List datastreams=obj.datastreams(mdId);
            if (datastreams!=null) {
                Datastream ds=(Datastream) datastreams.get(0);
                if (ds!=null) {
                    if (ds.DSControlGrp==Datastream.XML_METADATA) {
                        DatastreamXMLMetadata mds=(DatastreamXMLMetadata) ds;
                        if (mds.DSMDClass != DatastreamXMLMetadata.DESCRIPTIVE) {
                            ret.add(mdId);
                        }
                    }
                }
            }
        }
        return ret;
    }

    private void mdWrap(DatastreamXMLMetadata mds, StringBuffer buf) 
            throws StreamIOException {
        buf.append("    <mdWrap MIMETYPE=\"");
        buf.append(mds.DSMIME);
        buf.append("\" MDTYPE=\"");
        buf.append(mds.DSInfoType);
        buf.append("\" LABEL=\"");
        StreamUtility.enc(mds.DSLabel, buf);
        buf.append("\">\n");
        buf.append("      <xmlData>");
        InputStream in=mds.getContentStream();
        try {
            byte[] byteBuf = new byte[4096];
            int len;
            while ( ( len = in.read( byteBuf ) ) != -1 ) {
                buf.append(new String(byteBuf, 0, len, m_characterEncoding));
            }
        } catch (IOException ioe) {
            throw new StreamIOException("Error reading from datastream");
        } finally {
            try {
                in.close();
            } catch (IOException closeProb) {
                throw new StreamIOException("Error closing read stream");
              // ignore problems while closing
            }
        }
        buf.append("      </xmlData>\n");
        buf.append("    </mdWrap>\n");
    }
    
    public boolean equals(Object o) {
        if (this==o) { return true; }
        try {
            return equals((METSDOSerializer) o);
        } catch (ClassCastException cce) {
            return false;
        }
    }
    
    public boolean equals(METSDOSerializer o) {
        return (o.getEncoding().equals(getEncoding())
                && o.getVersion().equals(getVersion()));
    }

}
