/*
 * Created on Aug 12, 2004
 */
package fedora.server.security;

import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;

import org.w3c.dom.Element;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.sun.xacml.AbstractPolicy;
import com.sun.xacml.ParsingException;
import com.sun.xacml.Policy;
import com.sun.xacml.PolicySet;
import com.sun.xacml.finder.PolicyFinder;
import com.sun.xacml.finder.PolicyFinderModule;

/**
 * @author wdn5e@virginia.edu
 * to understand why this class is needed 
 * (why configuring the xacml pdp with all of the multiplexed policy finders just won't work),
 * @see "http://sourceforge.net/mailarchive/message.php?msg_id=6068981"
 */
public class ValidatePolicy extends PolicyFinderModule { 

	public static final String POLICY_SCHEMA_PROPERTY = "com.sun.xacml.PolicySchema";

	public static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

	public static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

	public static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";
	
    /** Logger for this class. */
    private static final Logger LOG = Logger.getLogger(
            ValidatePolicy.class.getName());

	private File schemaFile = null;

	public ValidatePolicy() {
		String schemaName = System.getProperty(POLICY_SCHEMA_PROPERTY);
		if (schemaName != null) {
			schemaFile = new File(schemaName);
			LOG.debug("using schemaFile="+schemaFile);
		}
	}

	private final DocumentBuilder getDocumentBuilder(ErrorHandler handler) throws ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setIgnoringComments(true);

		DocumentBuilder builder = null;

		// as of 1.2, we always are namespace aware
		factory.setNamespaceAware(true);

		if (schemaFile == null) {
			LOG.debug("not validating");			
			factory.setValidating(false);
			builder = factory.newDocumentBuilder();
		} else {
			LOG.debug("validating against "+schemaFile);			
			factory.setValidating(true);
			factory.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
			factory.setAttribute(JAXP_SCHEMA_SOURCE, schemaFile);
			builder = factory.newDocumentBuilder();
			builder.setErrorHandler(handler);
		}
		return builder;
	}
	
	

    public void init(PolicyFinder finder) {
    }

	public static void main(String[] args) {
		String filepath = args[0];
		File file = new File(filepath);
		if (!file.exists()) {
			LOG.error(filepath + " does not exist");
		} else if (! file.canRead()) {
			LOG.error("cannot read " + filepath);
		} else {
			ValidatePolicy policyChecker = new ValidatePolicy();
			String name = "";
			Element rootElement = null;
			try {
				DocumentBuilder builder = policyChecker.getDocumentBuilder(null);
				builder.setErrorHandler(policyChecker.new MyErrorHandler());
				rootElement = builder.parse(file).getDocumentElement();
				name = rootElement.getTagName();
			} catch (Throwable e) {
				LOG.error("couldn't parse repo-wide policy", e);
			}
	        AbstractPolicy abstractPolicy = null;
			try {
				if ("Policy".equals(name)) {
					LOG.debug("root node is Policy");
					abstractPolicy = Policy.getInstance(rootElement);
				} else if ("PolicySet".equals(name)) {
					LOG.debug("root node is PolicySet");
					abstractPolicy = PolicySet.getInstance(rootElement);
				} else {
					LOG.debug("bad root node for repo-wide policy");
				}
			} catch (ParsingException e) {
				LOG.error("couldn't parse repo-wide policy", e);
			}
		}
	}
	
	class MyErrorHandler implements ErrorHandler {

		/* (non-Javadoc)
		 * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
		 */
		public void error(SAXParseException exception) throws SAXException {
            LOG.error("error via handler", exception);
		}

		/* (non-Javadoc)
		 * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
		 */
		public void fatalError(SAXParseException exception) throws SAXException {
            LOG.error("fatal error via handler", exception);
		}

		/* (non-Javadoc)
		 * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
		 */
		public void warning(SAXParseException exception) throws SAXException {
            LOG.warn("warning via handler", exception);
		}
		
	}
	
}
