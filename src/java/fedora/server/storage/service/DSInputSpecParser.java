package fedora.server.storage.service;

import fedora.server.errors.RepositoryConfigurationException;
import fedora.server.errors.ObjectIntegrityException;
import fedora.server.storage.types.BMechDSBindSpec;
import fedora.server.storage.types.BMechDSBindRule;
import java.io.InputStream;
import java.util.Vector;
import java.util.StringTokenizer;
import java.util.Hashtable;
import java.util.HashMap;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

public class DSInputSpecParser extends DefaultHandler
{

  /** The namespaces we know we will encounter */
  private final static String FBS = "http://fedora.comm.nsdlib.org/service/bindspec";

  /**
   * URI-to-namespace prefix mapping info from SAX2 startPrefixMapping events.
   */
  private HashMap nsPrefixMap;

  // Variables for keeping state during SAX parse.
  private boolean inDSInputSpec = false;
  private boolean inDSInput = false;
  private boolean inDSInputLabel = false;
  private boolean inDSInputInstructions = false;
  private boolean inDSInputMIME = false;

  // Fedora Datastream Binding Spec objects
  private BMechDSBindSpec dsInputSpec;
  private BMechDSBindRule dsInputRule;

  // Working variables...
  private Vector tmp_InputRules;

  /**
   *   Constructor to enable another class to initiate the parsing
   */
  public DSInputSpecParser()
  {
  }

  /**
   *   Constructor allows this class to initiate the parsing
   */
  public DSInputSpecParser(InputStream in)
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
                  + "preparing for DSInputSpec datastream deserialization: "
                  + e.getMessage());
      }
      try
      {
          xmlReader.parse(new InputSource(in));
      }
      catch (Exception e)
      {
          throw new ObjectIntegrityException("Error parsing DSInputSpec datastream" +
                  e.getClass().getName() + ": " + e.getMessage());
      }
  }

  public BMechDSBindSpec getServiceDSInputSpec() {
      return dsInputSpec;
  }

  public void startDocument() throws SAXException
  {
    nsPrefixMap = new HashMap();
    tmp_InputRules = new Vector();
    dsInputSpec = new BMechDSBindSpec();
  }

  public void endDocument() throws SAXException
  {
      dsInputSpec.dsBindRules = (BMechDSBindRule[]) tmp_InputRules.toArray(new BMechDSBindRule[0]);
      dsInputSpec = dsInputSpec;
      tmp_InputRules = null;
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
    if (inDSInputLabel)
    {
      dsInputRule.bindingLabel = new String(ch, start, length);
    }
    else if (inDSInputInstructions)
    {
      dsInputRule.bindingInstruction = new String(ch, start, length);
    }
    else if (inDSInputMIME)
    {
      StringTokenizer st = new StringTokenizer(new String(ch, start, length), " ");
      String[] MIMETypes = new String[st.countTokens()];
      for (int i = 0; i < st.countTokens(); i++)
      {
        MIMETypes[i] = st.nextToken();
      }
      dsInputRule.bindingMIMETypes = MIMETypes;
    }
  }

  public void startElement(String namespaceURI, String localName, String qName, Attributes attrs)
    throws SAXException
  {
    if (namespaceURI.equalsIgnoreCase(FBS) && localName.equalsIgnoreCase("DSInputSpec"))
    {
      inDSInputSpec = true;
      dsInputSpec.bDefPID = attrs.getValue("bDefPID");
      dsInputSpec.bMechPID = attrs.getValue("bMechPID");
      dsInputSpec.bindSpecLabel = attrs.getValue("label");
    }
    else if (namespaceURI.equalsIgnoreCase(FBS) && localName.equalsIgnoreCase("DSInput"))
    {
      inDSInput = true;
      dsInputRule = new BMechDSBindRule();
      dsInputRule.bindingKeyName = attrs.getValue("wsdlMsgPartName");
      dsInputRule.maxNumBindings = new Integer(attrs.getValue("DSMax")).intValue();
      dsInputRule.minNumBindings = new Integer(attrs.getValue("DSMin")).intValue();
      dsInputRule.ordinality = Boolean.getBoolean(attrs.getValue("DSOrdinality"));
    }
    else if (namespaceURI.equalsIgnoreCase(FBS) && localName.equalsIgnoreCase("DSInputLabel"))
    {
      inDSInputLabel = true;
    }
    else if (namespaceURI.equalsIgnoreCase(FBS) && localName.equalsIgnoreCase("DSInputInstruction"))
    {
      inDSInputInstructions = true;
    }
    else if (namespaceURI.equalsIgnoreCase(FBS) && localName.equalsIgnoreCase("DSMIME"))
    {
      inDSInputMIME = true;
    }
  }

  public void endElement(String namespaceURI, String localName, String qName) throws SAXException
  {
    if (namespaceURI.equalsIgnoreCase(FBS) && localName.equalsIgnoreCase("DSInputSpec"))
    {
      inDSInputSpec = false;
    }
    else if (namespaceURI.equalsIgnoreCase(FBS) && localName.equalsIgnoreCase("DSInput"))
    {
      inDSInput = false;
      tmp_InputRules.add(dsInputRule);
      dsInputRule = null;
    }
    else if (namespaceURI.equalsIgnoreCase(FBS) && localName.equalsIgnoreCase("DSInputLabel"))
    {
      inDSInputLabel = false;
    }
    else if (namespaceURI.equalsIgnoreCase(FBS) && localName.equalsIgnoreCase("DSInputInstruction"))
    {
      inDSInputInstructions = false;
    }
    else if (namespaceURI.equalsIgnoreCase(FBS) && localName.equalsIgnoreCase("DSMIME"))
    {
      inDSInputMIME = false;
    }
  }
}
