package fedora.server.storage.types;

/**
 * <p>Title: Datastream.java </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author Sandy Payette
 * @version 1.0
 */

import fedora.server.errors.StreamIOException;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Datastream
{

  private ArrayList m_auditRecordIdList;
  
  public Datastream() {
      m_auditRecordIdList=new ArrayList();
  }

  /** ControlGrp for "Repository Managed Content" datastreams (internal) */
  public final static int MANAGED_CONTENT = 1;

  /** ControlGrp for "Implementor-Defined XML Metadata" datastreams (internal/inline) */
  public final static int XML_METADATA = 2;

  /** ControlGrp for "External Referenced Content" datastreams (external) */
  public final static int EXTERNAL_REF = 3;

  /** Future(?): ControlGrp "External Service Request" datastreams  (external/request) */
  //public final static int EXTERNAL_REQUEST = 4;

  public String DatastreamID;

  public String DSVersionID;

  public String DSLabel;

  public String DSMIME;

  public Date DSCreateDT;

  public long DSSize;

  /** Control Group: MANAGED_CONTENT/XML_METADATA/EXTERNAL_REF */
  public int DSControlGrp;

  /** Info Type: DATA or one of the METS MDType values */
  public String DSInfoType;

  public String DSState;

  public String DSLocation;
  
  public List auditRecordIdList() {
      return m_auditRecordIdList;
  }
  
  public InputStream getContentStream() 
          throws StreamIOException {
      return null;
  }

}