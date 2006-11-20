package fedora.server.test;

import junit.framework.TestCase;

import fedora.server.storage.types.BasicDigitalObject;
import fedora.server.storage.types.DigitalObject;
import fedora.server.storage.translation.*;
import fedora.server.errors.*;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

/**
 * Tests the FOXML deserializer and serializer by parsing
 * a FOXML input file and re-serializing it in the storage context.
 *
 * @author payette@cs.cornell.edu
 * @version $Id$
 */
public class FOXMLInOutTest
	extends TestCase {

	protected File inFile = null;
	protected File outFile = null;
	protected DigitalObject obj = null;
		
	protected void setUp() {
		//inFile=new File("TestIngestFiles/foxml-reference-example.xml");
		inFile=new File("TestIngestFiles/foxml-simple-nodissem.xml");
		outFile=new File("TestExportFiles/STORE-foxml.xml");
		System.setProperty("fedoraServerHost", "localhost");
		System.setProperty("fedoraServerPort", "8080");
		
		FileInputStream in=null;
		try {
			in=new FileInputStream(inFile);
		} catch (IOException ioe) {
				System.out.println("Error on FOXML file inputstream: " + ioe.getMessage());
				ioe.printStackTrace();
		}
		try {	
			// setup	
			FOXMLDODeserializer deser=new FOXMLDODeserializer();
			FOXMLDOSerializer ser=new FOXMLDOSerializer();
			HashMap desermap=new HashMap();
			HashMap sermap=new HashMap();
			desermap.put("foxml1.0", deser);
			DOTranslatorImpl trans=new DOTranslatorImpl(sermap, desermap);
			obj=new BasicDigitalObject();
			
			// deserialize input XML
			System.out.println("Deserializing...");
			trans.deserialize(in, obj, "foxml1.0", "UTF-8", DOTranslationUtility.DESERIALIZE_INSTANCE);
			System.out.println("Digital Object PID= " + obj.getPid());
			// serialize
			sermap.put("foxml1.0", ser);
			System.out.println("Re-serializing...");
			System.out.println("Writing file to... " + outFile.getPath());
			FileOutputStream out = new FileOutputStream(outFile);
			// re-serialize (either for the EXPORT or STORAGE context)
			int m_transContext = DOTranslationUtility.SERIALIZE_EXPORT_MIGRATE;
			trans.serialize(obj, out, "foxml1.0", "UTF-8", m_transContext);
			System.out.println("Done. Serialized for context: " + m_transContext);
		} catch (SAXException e) {
			e.printStackTrace();
			System.out.println("SAXException: (" + e.getClass().getName() + "):" + e.getMessage());
		} catch (ServerException e) {
			System.out.println("ServerException: suppressing info not available without running server.");
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Exception: (" + e.getClass().getName() + "):" + e.getMessage());

		}
	}
		
	public void testDigitalObject() {	
		//assertNotNull("Failure: digital object PID is null.", obj.getPid());
		//assertNotNull("Failure: digital object audit record set is null.", obj.getAuditRecords());
		//assertNotNull("Failure: digital object cmodel is null.", obj.getContentModelId());
		//assertNotNull("Failure: digital object createDate is null.", obj.getCreateDate());	
	}
}