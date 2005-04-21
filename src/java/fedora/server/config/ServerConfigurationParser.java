package fedora.server.config;

import java.io.*;
import java.util.*;
import javax.xml.parsers.*;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

/**
 *
 */
public class ServerConfigurationParser 
        extends DefaultHandler {

    private SAXParser m_parser;
    private InputStream m_xmlStream;

    private String m_serverClassName;
    private List m_serverParameters;
    private List m_moduleConfigurations;
    private List m_datastoreConfigurations;

    private Parameter m_lastParam;
    private String m_moduleOrDatastoreComment;

    private String m_role;
    private String m_class;
    private String m_id;

    private String m_paramName;
    private String m_paramValue;
    private String m_paramComment;
    private Map m_profileValues;

    private List m_moduleOrDatastoreParameters;  // module/datastore
    private StringBuffer m_commentBuffer;
    private boolean m_inParam;
    private boolean m_inModuleOrDatastore;

    public ServerConfigurationParser(InputStream xmlStream) throws IOException {
        m_xmlStream = xmlStream;
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            m_parser=spf.newSAXParser();
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("Error getting XML parser: " + e.getMessage());
        }
    }

    public ServerConfiguration parse() throws IOException {
        m_serverParameters = new ArrayList();
        m_moduleConfigurations = new ArrayList();
        m_datastoreConfigurations = new ArrayList();
        try {
            m_parser.parse(m_xmlStream, this);
            return new ServerConfiguration(m_serverClassName,
                                           m_serverParameters, 
                                           m_moduleConfigurations, 
                                           m_datastoreConfigurations);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("Error parsing XML: " + e.getMessage());
        }
    }

    public void startElement(String uri, 
                             String localName, 
                             String qName,
                             Attributes a) throws SAXException {
        if (localName.equals("server")) {
            m_serverClassName = a.getValue("class");
        } else if (localName.equals("module")) {
            m_inModuleOrDatastore = true;
            m_moduleOrDatastoreParameters = new ArrayList();
            m_role = a.getValue("role");
            m_class = a.getValue("class");
        } else if (localName.equals("datastore")) {
            m_inModuleOrDatastore = true;
            m_moduleOrDatastoreParameters = new ArrayList();
            m_id = a.getValue("id");
        } else if (localName.equals("comment")) {
            m_commentBuffer = new StringBuffer();
        } else if (localName.equals("param")) {
            m_inParam = true;
            m_paramName = a.getValue("name");
            m_paramValue = a.getValue("value");
            m_paramComment = null;
            m_profileValues = new HashMap();
            for (int i = 0; i < a.getLength(); i++) {
                String name = a.getLocalName(i);
                if (name.length() > 5 && name.endsWith("value")) {
                    String value = a.getValue(i);
                    m_profileValues.put(name.substring(0, name.length() - 5), value);
                }
            }
        }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (localName.equals("module")) {
            // add a new ModuleConfiguration to m_moduleConfigurations
            m_inModuleOrDatastore = false;
            m_moduleConfigurations.add(new ModuleConfiguration(m_moduleOrDatastoreParameters,
                                                               m_role,
                                                               m_class,
                                                               m_moduleOrDatastoreComment));
        } else if (localName.equals("datastore")) {
            // add a new DatastoreConfiguration to m_datastoreConfigurations
            m_inModuleOrDatastore = false;
            m_datastoreConfigurations.add(new DatastoreConfiguration(m_moduleOrDatastoreParameters,
                                                                     m_id,
                                                                     m_moduleOrDatastoreComment));
        } else if (localName.equals("comment")) {
            // figure out what kind of thing this is a comment for
            // if we're in a param, it's for the param.
            if (m_inParam) {
                m_paramComment = m_commentBuffer.toString();
            } else if (m_inModuleOrDatastore) {
                m_moduleOrDatastoreComment = m_commentBuffer.toString();
            } else {
                // the old style was to have a comment after (not inside) a param
                if (m_lastParam != null) {
                    m_lastParam.setComment(m_commentBuffer.toString());
                }
            }
        } else if (localName.equals("param")) {
            m_inParam = false;
            m_lastParam = new Parameter(m_paramName, m_paramValue, m_paramComment, m_profileValues);
            if (m_inModuleOrDatastore) {
                m_moduleOrDatastoreParameters.add(m_lastParam);
            } else {
                m_serverParameters.add(m_lastParam);
            }
        }
    }

    public void characters(char[] ch, int start, int length) {
        if (m_commentBuffer != null) m_commentBuffer.append(ch, start, length);
    }

}
