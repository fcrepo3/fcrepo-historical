package fedora.server.validation;

import fedora.server.errors.ObjectValidityException;
import fedora.server.errors.ServerException;

import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.Writer;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.Properties;
import java.net.URL;

// DOM classes
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

// TrAX classes
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.*;

import javax.xml.parsers.ParserConfigurationException;

/**
 *
 * <p><b>Title:</b> DOValidatorSchematron.java</p>
 * <p><b>Description:</b> Schematron validation for fedora objects encoded in
 * schematron schema for Fedora.  The schematron schema (fedoraRulesExt.xml)
 * expresses a set of rules using XPATH that enable us to check for things
 * that are either not expressed in the METS XML schema, or that cannot be
 * expressed with XML Schema language.  Generally we will look for things
 * that are requirements of Fedora objects, which are not requirements for
 * METS objects in general.</p>
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
 * <p>The entire file consists of original code.  Copyright &copy; 2002, 2003 by The
 * Rector and Visitors of the University of Virginia and Cornell University.
 * All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 *
 * @author payette@cs.cornell.edu
 * @version $Id$
 */
public class DOValidatorSchematron
{

  private StreamSource rulesSource;
  private StreamSource preprocessorSource;
  private StreamSource validatingStyleSheet;
  private StringBuffer string = new StringBuffer();

  /**
   * Constructs a DOValidatorSchematron instance with a Schematron preprocessor
   * that is provided by the calling class.  This will allow the DOValidator module
   * to pass in the preprocessor that is configured with the Fedora repository.
   *
   * @param schemaPath  the URL of the Schematron schema
   * @param preprocessorPath the location of the Schematron preprocessor
   * @param workFlowPhase the phase in the fedora object lifecycle to which
   *                      validation should pertain.  (Currently options are
   *                      "ingest" and "store"
   * @throws ObjectValidityException
   */
  public DOValidatorSchematron(String schemaPath, String preprocessorPath, String workFlowPhase)
      throws ObjectValidityException
  {
    validatingStyleSheet = setUp(preprocessorPath, schemaPath, workFlowPhase);
  }

  /**
   * Run the Schematron validation on a Fedora object.
   *
   * @param objectAsFile  the Fedora object as a File
   * @throws ServerException
   */
  public void validate(File objectAsFile) throws ServerException
  {
    validate(new StreamSource(objectAsFile));
  }

  /**
   * Run the Schematron validation on a Fedora object.
   *
   * @param objectAsStream  the Fedora object as an Inputstream
   * @throws ServerException
   */
  public void validate(InputStream objectAsStream) throws ServerException
  {
    validate(new StreamSource(objectAsStream));
  }

  /**
   * Run the Schematron validation on a Fedora object.
   *
   * @param objectSource  the Fedora object as an StreamSource
   * @throws ServerException
   */
  public void validate(StreamSource objectSource) throws ServerException
  {
    DOValidatorSchematronResult result = null;
    try
    {
      // Create a transformer that uses the validating stylesheet.
      // Run the Schematron validation of the Fedora object and
      // output results in DOM format.
      TransformerFactory tfactory = TransformerFactory.newInstance();
      Transformer vtransformer = tfactory.newTransformer(validatingStyleSheet);
      DOMResult validationResult = new DOMResult();
      vtransformer.transform(objectSource, validationResult);
      result = new DOValidatorSchematronResult(validationResult);
    }
    catch(TransformerException e)
    {
       System.err.println("Schematron validation: " + e.getMessage()) ;
       throw new ObjectValidityException(e.getMessage());
    }
    catch(Exception e)
    {
       System.err.println("Schematron validation: " + e.getMessage()) ;
       throw new ObjectValidityException(e.getMessage());
    }

    if (!result.isValid())
    {
      String msg = null;
      try
      {
        msg = result.getXMLResult();
        System.err.println(msg);
      }
      catch(Exception e)
      {
         System.err.println("Schematron validation: " + e.getMessage()) ;
         throw new ObjectValidityException(e.getMessage());
      }
      throw new ObjectValidityException(msg);
    }
  }

  /**
   * Run setup to prepare for Schematron validation.  This entails dynamically
   * creating the validating stylesheet using the preprocessor and the schema.
   *
   * @param preprocessorPath the location of the Schematron preprocessor
   * @param fedoraschemaPath the URL of the Schematron schema
   * @param workFlowPhase the phase in the fedora object lifecycle to which
   *                      validation should pertain.  (Currently options are
   *                      "ingest" and "store"
   * @return StreamSource
   * @throws ObjectValidityException
   */
  private StreamSource setUp(String preprocessorPath, String fedoraschemaPath, String workFlowPhase)
    throws ObjectValidityException
  {
    rulesSource = fileToStreamSource(fedoraschemaPath);
    preprocessorSource = fileToStreamSource(preprocessorPath);
    return(createValidatingStyleSheet(rulesSource, preprocessorSource, workFlowPhase));
  }

  /**
   * Create the validating stylesheet which will be used to perform the actual
   * Schematron validation.  The validating stylesheet is created dynamically
   * using the preprocessor stylesheet and the Schematron schema for Fedora.
   * The workFlowPhase is key.  The stylesheet is created for the appropriate
   * phase as specified in the fedoraRulesExt.xml schema.  Valid work flow phases
   * are currently "ingest" and "store."  Different schematron rules apply to
   * different phases of the object lifecycle.  Some rules are applied when
   * an object is first being ingested into the repository.  Other rules apply
   * before the object is put into permanent storage.
   * @param rulesSource the location of the rules
   * @param preprocessorSource the location of the Schematron preprocessor
   * @param workFlowPhase the phase in the fedora object lifecycle to which
   *                      validation should pertain.  (Currently options are
   *                      "ingest" and "store"
   * @return StreamSource
   * @throws ObjectValidityException
   */
  private StreamSource createValidatingStyleSheet(
    StreamSource rulesSource, StreamSource preprocessorSource, String workFlowPhase)
    throws ObjectValidityException
  {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try
    {
      // Create a transformer for that uses the Schematron preprocessor stylesheet.
      // Transform the Schematron schema (rules) into a validating stylesheet.
      TransformerFactory tfactory = TransformerFactory.newInstance();
      Transformer ptransformer = tfactory.newTransformer(preprocessorSource);
      ptransformer.setParameter("phase", workFlowPhase);
      ptransformer.transform(rulesSource, new StreamResult(out));
    }
    catch(TransformerException e)
    {
       System.err.println("Schematron validation: " + e.getMessage()) ;
       throw new ObjectValidityException(e.getMessage());
    }
    return(new StreamSource(new ByteArrayInputStream(out.toByteArray())));
  }

  /** Code based on com.jclark.xsl.sax.Driver: **/
  /**
   * Generates a StreamSource from a file name.
   */
  static public StreamSource fileToStreamSource(String str) {
    return fileToStreamSource(new File(str));
  }

  static public StreamSource fileToStreamSource(File file) {
    String path = file.getAbsolutePath();
    String fSep = System.getProperty("file.separator");
    if (fSep != null && fSep.length() == 1)
      path = path.replace(fSep.charAt(0), '/');
    if (path.length() > 0 && path.charAt(0) != '/')
      path = '/' + path;
    try {
      String url = new URL("file", null, path).toString();
      System.out.println("file to URL for StreamSource: " + url);
      return new StreamSource(url);
    }
    catch (java.net.MalformedURLException e) {
      /* According to the spec this could only happen if the file
  protocol were not recognized. */
      throw new Error("unexpected MalformedURLException");
    }
  }
}