package fedora.server.access;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.net.URLDecoder;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Properties;

import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;

import fedora.server.Context;
import fedora.server.Logging;
import fedora.server.ReadOnlyContext;
import fedora.server.Server;
import fedora.server.errors.InitializationException;
import fedora.server.errors.GeneralException;
import fedora.server.errors.ServerException;
import fedora.server.errors.StreamIOException;
import fedora.server.storage.DOManager;
import fedora.server.storage.types.ObjectMethodsDef;
import fedora.server.storage.types.MethodParmDef;
import fedora.server.utilities.DateUtility;
import fedora.server.utilities.Logger;
import fedora.server.utilities.StreamUtility;

/**
 * <p><b>Title: </b>ListMethodsServlet.java</p>
 * <p><b>Description: </b>Implements Fedora Access LITE (API-A-LITE) interface using a
 * java servlet front end. The syntax defined by API-A-LITE has two bindings:
 * <ol>
 * <li>GetDissemination URL syntax:
 * http://hostname:port/fedora/get/PID/bDefPID/methodName[/dateTime][?parmArray]
 * This syntax requests a dissemination of the specified object using the
 * specified method of the associated behavior definition object. The result
 * is returned as a MIME-typed stream.</li>
 * <ul>
 * <li>hostname - required hostname of the Fedora server.</li>
 * <li>port - required port number on which the Fedora server is running.</li>
 * <li>fedora - required path name for the Fedora access service.</li>
 * <li>get - required path name for the Fedora service.</li>
 * <li>PID - required persistent idenitifer of the digital object.</li>
 * <li>bDefPID - required persistent identifier of the behavior definition
 *               object to which the digital object subscribes.</li>
 * <li>methodName - required name of the method to be executed.</li>
 * <li>dateTime - optional dateTime value indicating dissemination of a
 *                version of the digital object at the specified point in time.
 *                (NOT currently implemented.)
 * <li>parmArray - optional array of method parameters consisting of name/value
 *                 pairs in the form parm1=value1&parm2=value2...</li>
 * </ul>
 * <li>GetObjectProfile URL syntax:
 * http://hostname:port/fedora/get/PID[/dateTime][?xml=BOOLEAN]
 * This syntax requests an object profile for the specified digital object.
 * The xml parameter determines the type of output returned.
 * If the parameter is omitted or has a value of "false", a MIME-typed stream
 * consisting of an html table is returned providing a browser-savvy means
 * of viewing the object profile. If the value specified is "true", then
 * a MIME-typed stream consisting of XML is returned.</li>
 * <ul>
 * <li>hostname - required hostname of the Fedora server.</li>
 * <li>port - required port number on which the Fedora server is running.</li>
 * <li>fedora - required name of the Fedora access service.</li>
 * <li>get - required verb of the Fedora service.</li>
 * <li>PID - required persistent identifier of the digital object.</li>
 * <li>dateTime - optional dateTime value indicating dissemination of a
 *                version of the digital object at the specified point in time.
 *                (NOT currently implemented.)
 * <li>xml - an optional parameter indicating the requested output format.
 *           A value of "true" indicates a return type of text/xml; the
 *           absence of the xml parameter or a value of "false"
 *           indicates format is to be text/html.</li>
 * </ul>
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
 * @author rlw@virginia.edu
 * @version $Id$
 */
public class ListMethodsServlet extends HttpServlet
{
  /** Content type for html. */
  private static final String CONTENT_TYPE_HTML = "text/html; charset=UTF-8";

  /** Content type for xml. */
  private static final String CONTENT_TYPE_XML  = "text/xml; charset=UTF-8";

  /** Instance of the Fedora server. */
  private static Server s_server = null;

  /** Instance of the access subsystem. */
  private static Access s_access = null;

  /** Portion of initial request URL from protocol up to query string */
  private String requestURI = null;

  /** Instance of URLDecoder */
  private URLDecoder decoder = new URLDecoder();

  /** Host name of the Fedora server **/
  private static String fedoraServerHost = null;

  /** Port number on which the Fedora server is running. **/
  private static String fedoraServerPort = null;

  /** Instance of Logger to log servlet events in Fedora server log */
  private static Logger logger = null;

  /**
   * Get the userId if the provided base64-encoded user:pass string
   * provides a correct user-to-password match.  Otherwise, return null.
   *
   * @param basicAuthString base64-encoder user:pass string.
   * @return the user id.
   */
  private String getAuthenticatedUser(String basicAuthString) {
      String authUser=null;
      if (basicAuthString!=null) {
          String userAndPass=new String(StreamUtility.decodeBase64(basicAuthString.substring(6).trim()));
          int i=userAndPass.indexOf(":");
          if (i>0) {
              String user=userAndPass.substring(0, i);
              String pass=userAndPass.substring(i+1);
              if (isUserPassword(user, pass)) {
                  authUser=user;
              }
          }
      }
      return authUser;
  }

  /**
   * Tell whether the password is the user's password.
   *
   * @param user the userId.
   * @param pass the possible password.
   * @return true if the password is correct, false otherwise.
   */
  private boolean isUserPassword(String user, String pass) {
      // currently only recognizes fedoraAdmin.
      return (user.equals("fedoraAdmin") && pass.equals(s_server.getParameter("adminPassword")));
  }

  /**
   * <p>Process Fedora Access Request. Parse and validate the servlet input
   * parameters and then execute the specified request.</p>
   *
   * @param request  The servlet request.
   * @param response servlet The servlet response.
   * @throws ServletException If an error occurs that effects the servlet's
   *         basic operation.
   * @throws IOException If an error occurrs with an input or output operation.
   */
  public void doGet(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException
  {
      String PID = null;
      String dsID = null;
      Date asOfDateTime = null;
      Date versDateTime = null;
      long servletStartTime = new Date().getTime();
      boolean isListMethodsRequest = false;
      boolean xml = false;

      HashMap h=new HashMap();
      h.put("application", "apia");
      h.put("useCachedObject", "false");
      String authenticatedUser=getAuthenticatedUser(request.getHeader("Authorization"));
      if (authenticatedUser!=null) {
          h.put("userId", authenticatedUser);
          // add permissions to context, based on userId.
          // FIXME:
          // For now, fedoraAdmin has all important permissions.  How to do this
          // eventually is to-be-decided, but for now it's hardcoded.
          if (authenticatedUser.equals("fedoraAdmin")) {
              h.put("canUseInactiveDatastream", "true");
              h.put("canUseDeletedDatastream", "true");
              h.put("canUseDeletedObject", "true");
              h.put("canUseInactiveObject", "true");
          }
      } else {
          h.put("userId", "guest");
      }
      h.put("host", request.getRemoteAddr());
      ReadOnlyContext context = new ReadOnlyContext(h);

      requestURI = request.getRequestURL().toString() + "?" + request.getQueryString();

      // Parse servlet URL.
      String[] URIArray = request.getRequestURL().toString().split("/");
      if (URIArray.length == 6 || URIArray.length == 7) {
          // Request is either unversioned or versioned listMethods request
          try {
              PID = Server.getPID(URIArray[5]).toString();  // normalize the PID
          } catch (Throwable th) {
              String message = "[FedoraAccessServlet] An error has occured in "
                      + "accessing the Fedora Access Subsystem. The error was \" "
                      + th.getClass().getName()
                      + " \". Reason: "  + th.getMessage()
                      + "  Input Request was: \"" + request.getRequestURL().toString();
              logger.logWarning(message);
              response.setContentType(CONTENT_TYPE_HTML);
              ServletOutputStream out = response.getOutputStream();
              out.println("<html><body><h3>" + message + "</h3></body></html>");
              return;
        }
          if (URIArray.length == 7) {
              // Request is a versioned listMethods request
              versDateTime = DateUtility.convertStringToDate(URIArray[6]);
              if (versDateTime == null) {
                  String message = "ListMethods Request Syntax Error: DateTime value "
                      + "of \"" + URIArray[6] + "\" is not a valid DateTime format. "
                      + " ----- The expected format for DateTime is \""
                      + "YYYY-MM-DDTHH:MM:SSZ\".  "
                      + " ----- The expected syntax for "
                      + "ListMethods requests is: \""
                      + URIArray[0] + "//" + URIArray[2] + "/"
                      + URIArray[3] + "/" + URIArray[4]
                      + "/PID[/dateTime] \"  ."
                      + " ----- Submitted request was: \"" + requestURI + "\"  .  ";
                  logger.logWarning(message);
                  response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                  response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message);
                  return;
              } else {
                  asOfDateTime=versDateTime;
              }
          }
          logger.logFinest("[ListMethodsServlet] ListMethods Syntax "
              + "Encountered: "+ requestURI);
          logger.logFinest("PID: " + PID + " asOfDate: " + versDateTime);
          isListMethodsRequest = true;
      } else {
          // Bad syntax; redirect to syntax documentation page.
          response.sendRedirect("/userdocs/client/browser/apialite/index.html");
          return;
      }

      // Separate out servlet parameters from method parameters
      Hashtable h_parms = new Hashtable();
      for ( Enumeration e = request.getParameterNames(); e.hasMoreElements();)
      {
          String name = decoder.decode((String)e.nextElement(), "UTF-8");
          if (isListMethodsRequest && name.equalsIgnoreCase("xml"))
          {
              xml = new Boolean(request.getParameter(name)).booleanValue();
          }
          else
          {
              String value = decoder.decode(request.getParameter(name), "UTF-8");
              h_parms.put(name,value);
          }
      }

      try {
          if (isListMethodsRequest) {
              listMethods(context, PID, asOfDateTime, xml, request, response);
              long stopTime = new Date().getTime();
              long interval = stopTime - servletStartTime;
              logger.logFiner("[ListMethodsServlet] Servlet Roundtrip "
                  + "listMethods: " + interval + " milliseconds.");
          }

          } catch (Throwable th)
          {
              String message = "[ListMethodsServlet] An error has occured in "
                             + "accessing the Fedora Access Subsystem. The error was \" "
                             + th.getClass().getName()
                             + " \". Reason: "  + th.getMessage()
                             + "  Input Request was: \"" + request.getRequestURL().toString();
              logger.logWarning(message);
              th.printStackTrace();
          }
  }

  public void listMethods(Context context, String PID, Date asOfDateTime,
      boolean xml, HttpServletRequest request,
      HttpServletResponse response) throws ServerException
  {

      OutputStreamWriter out = null;
      Date versDateTime = asOfDateTime;
      ObjectMethodsDef[] methodDefs = null;
      PipedWriter pw = null;
      PipedReader pr = null;

      try
      {
          pw = new PipedWriter();
          pr = new PipedReader(pw);
          methodDefs = s_access.listMethods(context, PID, asOfDateTime);
          if (methodDefs.length > 0)
          {
              // Object Profile found.
              // Serialize the ObjectProfile object into XML
              new ObjectMethodsDefSerializerThread(PID, methodDefs, versDateTime, pw).start();
              if (xml)
              {
                  // Return results as raw XML
                  response.setContentType(CONTENT_TYPE_XML);

                  // Insures stream read from PipedReader correctly translates utf-8
                  // encoded characters to OutputStreamWriter.
                  out = new OutputStreamWriter(response.getOutputStream(),"UTF-8");
                  int bufSize = 4096;
                  char[] buf=new char[bufSize];
                  int len=0;
                  while ( (len = pr.read(buf, 0, bufSize)) != -1) {
                      out.write(buf, 0, len);
                  }
                  out.flush();
              } else
              {
                  // Transform results into an html table
                  response.setContentType(CONTENT_TYPE_HTML);
                  out = new OutputStreamWriter(response.getOutputStream(),"UTF-8");
                  File xslFile = new File(s_server.getHomeDir(), "access/listMethods.xslt");
                  TransformerFactory factory = TransformerFactory.newInstance();
                  Templates template = factory.newTemplates(new StreamSource(xslFile));
                  Transformer transformer = template.newTransformer();
                  Properties details = template.getOutputProperties();
                  transformer.transform(new StreamSource(pr), new StreamResult(out));
              }
              out.flush();

          } else
          {
              // listMethods request returned nothing.
              String message = "[ListMethodsServlet] No Datastreams returned.";
              logger.logInfo(message);
          }
          } catch (Throwable th)
          {
              String message = "[ListMethodsServlet] An error has occured. "
                             + " The error was a \" "
                             + th.getClass().getName()
                             + " \". Reason: "  + th.getMessage();
              logger.logWarning(message);
              th.printStackTrace();
              throw new GeneralException(message);
          } finally
          {
              try
              {
                  if (pr != null) pr.close();
                  if (out != null) out.close();
                  } catch (Throwable th)
                  {
                      String message = "[ListMethodsServlet] An error has occured. "
                          + " The error was a \" "
                          + th.getClass().getName()
                          + " \". Reason: "  + th.getMessage();
                      throw new StreamIOException(message);
                  }
          }
  }

  /**
   * <p> A Thread to serialize an ObjectProfile object into XML.</p>
   *
   */
  public class ObjectMethodsDefSerializerThread extends Thread
  {
      private PipedWriter pw = null;
      private String PID = null;
      private ObjectMethodsDef[] methodDefs = null;
      private Date versDateTime = null;

      /**
       * <p> Constructor for ProfileSerializeThread.</p>
       *
       * @param PID The persistent identifier of the specified digital object.
       * @param methodDefs An array of ObjectMethodsDefs.
       * @param versDateTime The version datetime stamp of the request.
       * @param pw A PipedWriter to which the serialization info is written.
       */
      public ObjectMethodsDefSerializerThread(String PID, ObjectMethodsDef[] methodDefs,
              Date versDateTime, PipedWriter pw)
      {
          this.pw = pw;
          this.PID = PID;
          this.methodDefs = methodDefs;
          this.versDateTime = versDateTime;
      }

      /**
       * <p> This method executes the thread.</p>
       */
      public void run()
      {
          if (pw != null)
          {
              try
              {
                  pw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                  if (versDateTime == null || DateUtility.
                      convertDateToString(versDateTime).equalsIgnoreCase(""))
                  {
                      pw.write("<objectMethods "
                          + "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" "
                          + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                          + "xsi:schemaLocation=\"http://www.fedora.info/definitions/1/0/access/ "
                          + "http://" + fedoraServerHost + ":" + fedoraServerPort
                          + "/listMethods.xsd\"" + " pid=\"" + StreamUtility.enc(PID) + "\" "
                          + "baseURL=\"http://" + fedoraServerHost + ":" + fedoraServerPort
                          + "/fedora/\" >");
                  } else
                  {
                      pw.write("<objectMethods "
                          + "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" "
                          + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                          + "xsi:schemaLocation=\"http://www.fedora.info/definitions/1/0/access/ "
                          + "http://" + fedoraServerHost + ":" + fedoraServerPort
                          + "/listMethods.xsd\"" + " pid=\"" + StreamUtility.enc(PID) + "\" "
                          + "asOfDateTime=\"" + DateUtility.convertDateToString(versDateTime) + "\" "
                          + "baseURL=\"http://" + fedoraServerHost + ":" + fedoraServerPort + "/fedora/\""
                          + " >");
                  }

                  // ObjectMethodsDef SERIALIZATION
                  String nextBdef = "null";
                  String currentBdef = "";
                  for (int i=0; i<methodDefs.length; i++)
                  {
                      currentBdef = methodDefs[i].bDefPID;
                      if (!currentBdef.equalsIgnoreCase(nextBdef))
                      {
                          if (i != 0) pw.write("</bDef>");
                          pw.write("<bDef pid=\"" + StreamUtility.enc(methodDefs[i].bDefPID) + "\" >");
                      }
                      pw.write("<method name=\"" + StreamUtility.enc(methodDefs[i].methodName) + "\" >");
                      MethodParmDef[] methodParms = methodDefs[i].methodParmDefs;
                      for (int j=0; j<methodParms.length; j++)
                      {
                          pw.write("<methodParm parmName=\"" + StreamUtility.enc(methodParms[j].parmName)
                              + "\" parmDefaultValue=\"" + StreamUtility.enc(methodParms[j].parmDefaultValue)
                              + "\" parmRequired=\"" + methodParms[j].parmRequired
                              + "\" parmLabel=\"" + StreamUtility.enc(methodParms[j].parmLabel) + "\" >");
                          if (methodParms[j].parmDomainValues.length > 0 )
                          {
                              pw.write("<methodParmDomain>");
                              for (int k=0; k<methodParms[j].parmDomainValues.length; k++)
                              {
                                  pw.write("<methodParmValue>" + StreamUtility.enc(methodParms[j].parmDomainValues[k])
                                      + "</methodParmValue>");
                              }
                              pw.write("</methodParmDomain>");
                          }
                          pw.write("</methodParm>");
                      }

                      pw.write("</method>");
                      nextBdef = currentBdef;
                  }
                  pw.write("</bDef>");
                  pw.write("</objectMethods>");

                  pw.flush();
                  pw.close();
              } catch (IOException ioe) {
                  System.err.println("WriteThread IOException: " + ioe.getMessage());
              } finally
              {
                  try
                  {
                      if (pw != null) pw.close();
                      } catch (IOException ioe)
                      {
                          System.err.println("WriteThread IOException: " + ioe.getMessage());
                      }
              }
          }
      }
  }

  /**
   * <p>For now, treat a HTTP POST request just like a GET request.</p>
   *
   * @param request The servet request.
   * @param response The servlet response.
   * @throws ServletException If thrown by <code>doGet</code>.
   * @throws IOException If thrown by <code>doGet</code>.
   */
  public void doPost(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException
  {
      doGet(request, response);
  }

  /**
   * <p>Initialize servlet.</p>
   *
   * @throws ServletException If the servet cannot be initialized.
   */
  public void init() throws ServletException
  {
      try
      {
          s_server=Server.getInstance(new File(System.getProperty("fedora.home")));
          fedoraServerHost = s_server.getParameter("fedoraServerHost");
          fedoraServerPort = s_server.getParameter("fedoraServerPort");
          s_access = (Access) s_server.getModule("fedora.server.access.Access");
          logger = new Logger();
          } catch (InitializationException ie)
          {
              throw new ServletException("Unable to get Fedora Server instance."
                      + ie.getMessage());
          }

  }

  /**
   * <p>Cleans up servlet resources.</p>
   */
  public void destroy()
  {}

  private Server getServer() {
      return s_server;
  }

}