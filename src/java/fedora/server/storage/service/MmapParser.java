package fedora.server.storage.service;

/**
 * <p>Title: MmapParser.java</p>
 * <p>Description:  </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author Sandy Payette  payette@cs.cornell.edu
 * @version 1.0
 */

import fedora.server.errors.*;
import fedora.server.storage.types.MethodDef;
import fedora.server.storage.types.MethodParmDef;
import java.io.InputStream;
import java.util.Vector;
import java.util.Hashtable;
import java.util.HashMap;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

class MmapParser extends DefaultHandler
{

  /** The namespaces we know we will encounter */
  private final static String FMM = "http://fedora.comm.nsdlib.org/service/methodmap";

  /**
   * URI-to-namespace prefix mapping info from SAX2 startPrefixMapping events.
   */
  private HashMap nsPrefixMap;

  // Variables for keeping state during SAX parse.
  private boolean inMethod = false;
  private boolean inUserInputParm = false;

  // Fedora Method Map Entities

  private Mmap methodMap;
  private MmapMethodDef methodMapMethod;
  private MmapMethodParmDef methodMapParm;

  //private Hashtable wsdlMsgToMethodTbl;
  private Hashtable wsdlOperationToMethodDefTbl;
  private Hashtable wsdlMsgPartToParmDefTbl;

  // Working variables...

  private Vector tmp_enum;
  private Vector tmp_parms;
  private Vector tmp_methods;

  /**
   *   Constructor to enable another class to initiate the parsing
   */
  public MmapParser()
  {
  }

  /**
   *   Constructor allows this class to initiate the parsing
   */
  public MmapParser(InputStream in)
    throws RepositoryConfigurationException, ObjectIntegrityException
  {
      XMLReader xmlReader = null;
      try
      {
          SAXParserFactory saxfactory=SAXParserFactory.newInstance();
          saxfactory.setValidating(false);
          SAXParser parser=saxfactory.newSAXParser();
          xmlReader=parser.getXMLReader();
          xmlReader.setContentHandler(this);
          xmlReader.setFeature("http://xml.org/sax/features/namespaces", false);
          xmlReader.setFeature("http://xml.org/sax/features/namespace-prefixes", false);
      }
      catch (Exception e)
      {
          throw new RepositoryConfigurationException("Internal SAX error while "
                  + "preparing for Method Map datastream parsing: "
                  + e.getMessage());
      }
      try
      {
          xmlReader.parse(new InputSource(in));
      }
      catch (Exception e)
      {
          throw new ObjectIntegrityException("Error parsing Method Map datastream" +
                  e.getClass().getName() + ": " + e.getMessage());
      }
  }
  protected Mmap getMethodMap()
  {
    return methodMap;
  }

  public void startDocument() throws SAXException
  {
    System.out.println("MmapParser: START DOC");
    nsPrefixMap = new HashMap();
    wsdlOperationToMethodDefTbl = new Hashtable();
  }

  public void endDocument() throws SAXException
  {
    System.out.println("MmapParser: END DOC");
    nsPrefixMap = null;
  }

  public void startPrefixMapping(String prefix, String uri) throws SAXException
  {
    nsPrefixMap.put(uri, prefix);
  }

  public void skippedEntity(String name) throws SAXException
  {
    StringBuffer sb = new StringBuffer();
    sb.append('&');
    sb.append(name);
    sb.append(';');
    char[] text = new char[sb.length()];
    sb.getChars(0, sb.length(), text, 0);
    this.characters(text, 0, text.length);
  }

  public void characters(char ch[], int start, int length)  throws SAXException
  {
  }

  public void startElement(String namespaceURI, String localName, String qName, Attributes attrs)
    throws SAXException
  {
    //System.out.println("MmapParser: START ELEMENT " + qName);
    if (namespaceURI.equalsIgnoreCase(FMM) && localName.equalsIgnoreCase("MethodMap"))
    {
      methodMap = new Mmap();
      methodMap.mmapName = attrs.getValue("name");
      tmp_methods = new Vector();
    }
    else if (namespaceURI.equalsIgnoreCase(FMM) && localName.equalsIgnoreCase("Method"))
    {
        inMethod = true;
        methodMapMethod = new MmapMethodDef();
        methodMapMethod.methodName = attrs.getValue("operationName");
        methodMapMethod.methodLabel = "fix me";
        methodMapMethod.wsdlOperationName = attrs.getValue("operationName");
        methodMapMethod.wsdlMessageName = attrs.getValue("wsdlMsgName");
        methodMapMethod.wsdlOutputMessageName = attrs.getValue("wsdlMsgOutput");
        tmp_parms = new Vector();
        wsdlMsgPartToParmDefTbl = new Hashtable();
    }
    else if (inMethod)
    {
      if (namespaceURI.equalsIgnoreCase(FMM) && localName.equalsIgnoreCase("DatastreamInputParm"))
      {
        methodMapParm = new MmapMethodParmDef();
        methodMapParm.wsdlMessagePartName = attrs.getValue("parmName");
        methodMapParm.parmName = attrs.getValue("parmName");
        methodMapParm.parmLabel = "fix me";
        methodMapParm.parmPassBy = attrs.getValue("passBy");
        methodMapParm.parmType = MethodParmDef.DATASTREAM_INPUT;
        methodMapParm.parmDefaultValue = null;
        methodMapParm.parmDomainValues = new String[0];
      }
      else if (namespaceURI.equalsIgnoreCase(FMM) && localName.equalsIgnoreCase("DefaultInputParm"))
      {
        methodMapParm = new MmapMethodParmDef();
        methodMapParm.wsdlMessagePartName = attrs.getValue("parmName");
        methodMapParm.parmName = attrs.getValue("parmName");
        methodMapParm.parmLabel = "fix me";
        methodMapParm.parmPassBy = MethodParmDef.PASS_BY_VALUE;
        methodMapParm.parmType = MethodParmDef.DEFAULT_INPUT;
        methodMapParm.parmDefaultValue = attrs.getValue("defaultValue");
        methodMapParm.parmDomainValues = new String[0];
      }
      else if (namespaceURI.equalsIgnoreCase(FMM) && localName.equalsIgnoreCase("UserInputParm"))
      {
        inUserInputParm = true;
        methodMapParm = new MmapMethodParmDef();
        methodMapParm.wsdlMessagePartName = attrs.getValue("parmName");
        methodMapParm.parmName = attrs.getValue("parmName");
        methodMapParm.parmLabel = "fix me";
        methodMapParm.parmPassBy = MethodParmDef.PASS_BY_VALUE;
        methodMapParm.parmType = MethodParmDef.USER_INPUT;
        methodMapParm.parmDefaultValue = attrs.getValue("defaultValue");
      }
      else if (inUserInputParm)
      {
        if (namespaceURI.equalsIgnoreCase(FMM) && localName.equalsIgnoreCase("ValidParmValues"))
        {
          tmp_enum = new Vector();
        }
        else if (namespaceURI.equalsIgnoreCase(FMM) && localName.equalsIgnoreCase("ValidParm"))
        {
          tmp_enum.add(attrs.getValue("value"));
        }
      }
    }
  }

  public void endElement(String namespaceURI, String localName, String qName) throws SAXException
  {
    //System.out.println("MmapParser: END ELEMENT " + qName);
    if (namespaceURI.equalsIgnoreCase(FMM) && localName.equalsIgnoreCase("MethodMap"))
    {
      methodMap.mmapMethods = (MmapMethodDef[])tmp_methods.toArray(new MmapMethodDef[0]);
      methodMap.wsdlOperationToMethodDef = wsdlOperationToMethodDefTbl;
      tmp_methods = null;
    }
    else if (namespaceURI.equalsIgnoreCase(FMM) && localName.equalsIgnoreCase("Method"))
    {
        methodMapMethod.methodParms = (MethodParmDef[])tmp_parms.toArray(new MethodParmDef[0]);
        methodMapMethod.wsdlMsgParts = (MmapMethodParmDef[])tmp_parms.toArray(new MmapMethodParmDef[0]);
        methodMapMethod.wsdlMsgPartToParmDefTbl = wsdlMsgPartToParmDefTbl;
        tmp_methods.add(methodMapMethod);
        wsdlOperationToMethodDefTbl.put(methodMapMethod.methodName, methodMapMethod);
        wsdlMsgPartToParmDefTbl = null;
        methodMapMethod = null;
        tmp_parms = null;
        inMethod = false;
    }
    else if (inMethod)
    {
      if (namespaceURI.equalsIgnoreCase(FMM) && localName.equalsIgnoreCase("DatastreamInputParm"))
      {
        tmp_parms.add(methodMapParm);
        wsdlMsgPartToParmDefTbl.put(methodMapParm.wsdlMessagePartName, methodMapParm);
        methodMapParm = null;
      }
      else if (namespaceURI.equalsIgnoreCase(FMM) && localName.equalsIgnoreCase("DefaultInputParm"))
      {
        tmp_parms.add(methodMapParm);
        wsdlMsgPartToParmDefTbl.put(methodMapParm.wsdlMessagePartName, methodMapParm);
        methodMapParm = null;
      }
      else if (namespaceURI.equalsIgnoreCase(FMM) && localName.equalsIgnoreCase("UserInputParm"))
      {
        tmp_parms.add(methodMapParm);
        wsdlMsgPartToParmDefTbl.put(methodMapParm.wsdlMessagePartName, methodMapParm);
        methodMapParm = null;
        inUserInputParm = false;
      }
      else if (inUserInputParm && namespaceURI.equalsIgnoreCase(FMM) && localName.equalsIgnoreCase("ValidParmValues"))
      {
        methodMapParm.parmDomainValues = (String[])tmp_enum.toArray(new String[0]);
        tmp_enum = null;
      }
    }
  }
}