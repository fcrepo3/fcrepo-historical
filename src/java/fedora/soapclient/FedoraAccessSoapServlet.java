package fedora.soapclient;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.net.URL;
import java.net.URLDecoder;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.xml.namespace.QName;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;

import com.icl.saxon.expr.StringValue;

import fedora.server.types.gen.DatastreamDef;
import fedora.server.types.gen.FieldSearchQuery;
import fedora.server.types.gen.FieldSearchResult;
//import fedora.server.types.gen.MethodDef;
import fedora.server.types.gen.MethodParmDef;
import fedora.server.types.gen.MIMETypedStream;
import fedora.server.types.gen.ObjectMethodsDef;
import fedora.server.types.gen.ObjectProfile;
import fedora.server.types.gen.RepositoryInfo;
import fedora.server.types.gen.Property;
import fedora.server.utilities.DateUtility;
import fedora.server.utilities.StreamUtility;

import org.apache.axis.client.Service;
import org.apache.axis.client.Call;
import org.apache.axis.encoding.ser.BeanSerializerFactory;
import org.apache.axis.encoding.ser.BeanDeserializerFactory;

/**
 * <p>
 * <b>Title: </b>FedoraAccessSoapServlet.java
 * </p>
 * <p>
 * <b>Description: </b>An example of a web-based client that provides a front
 * end to the Fedora Access SOAP service. This servlet is designed to provide a
 * "browser centric" view of the Fedora Access interface. Return types from the
 * Fedora Access SOAP service are translated into a form suitable for viewing
 * with a web browser; in other words MIME-typed streams. Applications that can
 * readily handle SOAP requests and responses would most likely communicate
 * directly with the Fedora Access SOAP service rather than use a java servlet
 * as an intermediary. This servlet serves as an example of how to construct a
 * client that uses the Fedora Access API via SOAP.
 * </p>
 * 
 * <p>
 * Input parameters for the servlet include:
 * </p>
 * <ul>
 * <li>action_ name of Fedora service which must be one of the following:
 * <ol>
 * <li>GetDissemination - Gets a dissemination result</li>
 * <li>GetDatastreamDissemination - Gets the contents of a datastream in an
 * object.</li>
 * <li>GetObjectProfile - Gets object profile.</li>
 * <li>DescribeRepository - Gets information about the repository server.</li>
 * <li>GetObjectHistory - Gets the change history of an object.
 * <li>ListMethods - Gets a list of all methods of an object.</li>
 * <li>ListDatastreams - Gets a list of all datastreams of an object.</li>
 * </ol>
 * <li>PID_ - persistent identifier of the digital object</li>
 * <li>bDefPID_ - persistent identifier of the Behavior Definiton object</li>
 * <li>dsID_ - identifier of the datastream</li>
 * <li>methodName_ - name of the method</li>
 * <li>asOfDateTime_ - versioning datetime stamp</li>
 * <li>xml_ - boolean switch used in conjunction with ListDatastreams,
 * ListMethods, GetObjectProfile, and DescribeRepository that determines whether
 * output is formatted as XML or as HTML; value of "true" indicates XML format;
 * value of false or omission indicates HTML format.
 * <li>userParms - behavior methods may require or provide optional parameters
 * that may be input as arguments to the method; these method parameters are
 * entered as name/value pairs like the other serlvet parameters.Used in
 * conjunction with GetDissemination. (optional)</li>
 * </ul>
 * <p>
 * <i>Note that all servlet parameter names that are implementation specific end
 * with the underscore character ("_"). This is done to avoid possible name
 * clashes with user-supplied method parameter names. As a general rule,
 * user-supplied parameters should never contain names that end with the
 * underscore character to prevent possible name conflicts.</i>
 * </ul>
 * <p>
 * Example URLs
 * </p>
 * <ol>
 * <li>GetDissemination URL syntax:
 * <p>
 * protocol://hostname:port/soapclient/apia?action_=GetDissemination&PID_=pid&bDefPID_=bdefpid&methodName_=methodname[&asOfDateTime_=dateTime][?parmArray]
 * </p>
 * <p>
 * This syntax requests a dissemination of the specified object using the
 * specified method of the associated behavior definition object. The result is
 * returned as a MIME-typed stream.
 * </p>
 * </li>
 * <ul>
 * <li>protocol - either http or https.</li>
 * <li>hostname - required hostname of the Fedora server.</li>
 * <li>port - required port number on which the Fedora server is running.</li>
 * <li>soapclient - required name of the sample soap client.</li>
 * <li>apia - required name of the sample soap client</li>
 * <li> GetDissemination - required action name for getting a dissemination
 * request.</li>
 * <li>pid - required persistent idenitifer of the digital object.</li>
 * <li>bdefpid - required persistent identifier of the behavior definition
 * object to which the digital object subscribes.</li>
 * <li>methodname - required name of the method to be executed.</li>
 * <li>dateTime - optional dateTime value indicating dissemination of a version
 * of the digital object at the specified point in time.
 * <li>parmArray - optional array of method parameters consisting of name/value
 * pairs in the form parm1=value1&parm2=value2...</li>
 * </ul>
 * <li>GetDatastreamDissemination URL syntax:
 * <p>
 * protocol://hostname:port/soapclient/apia?action=GetDatastreamDissemination&PID_=pid&dsID_=dsid[&asOfDateTime_=dateTime]
 * </p>
 * <p>
 * This syntax requests a datastream dissemination for the specified digital
 * object. It is used to return the contents of a datastream.
 * </p>
 * </li>
 * <ul>
 * <li>protocol - either http or https.</li>
 * <li>hostname - required hostname of the Fedora server.</li>
 * <li>port - required port number on which the Fedora server is running.</li>
 * <li>soapclient - required name of the sample soap client.</li>
 * <li>apia - required name of the sample soap client</li>
 * <li> GetDatastreamDissemination - required action name for getting a
 * datastream dissemination request.</li>
 * <li>pid - required persistent identifier of the digital object.</li>
 * <li>dsid - required datastream identifier for the datastream.</li>
 * <li>dateTime - optional dateTime value indicating dissemination of a version
 * of the digital object at the specified point in time.
 * </ul>
 * <li>GetObjectProfile URL syntax:
 * <p>
 * protocol://hostname:port/soapclient/apia?action_=GetObjectProfile&PID_=pid[&asOfDateTime_=dateTime][&xml=boolean]
 * </p>
 * <p>
 * This syntax requests an object profile for the specified digital object.
 * </p>
 * </li>
 * <ul>
 * <li>protocol - either http or https.</li>
 * <li>hostname - required hostname of the Fedora server.</li>
 * <li>port - required port number on which the Fedora server is running.</li>
 * <li>soapclient - required name of the sample soap client.</li>
 * <li>apia - required name of the sample soap client</li>
 * <li> GetObjectProfile - required action name for getting an Object Profile
 * request.</li>
 * <li>pid - required persistent identifier of the digital object.</li>
 * <li>dateTime - optional dateTime value indicating dissemination of a version
 * of the digital object at the specified point in time.
 * </ul>
 * <li>GetObjectHistory URL syntax:
 * <p>
 * protocol://hostname:port/soapclient/apia?action_=GetObjectHistory&PID_=pid[&xml_=boolean]
 * </p>
 * <p>
 * This syntax requests an object history for the specified digital object.
 * </p>
 * </li>
 * <ul>
 * <li>protocol - either http or https.</li>
 * <li>hostname - required hostname of the Fedora server.</li>
 * <li>port - required port number on which the Fedora server is running.</li>
 * <li>soapclient - required name of the sample soap client.</li>
 * <li>apia - required name of the sample soap client</li>
 * <li> GetObjectHistory - required action name for getting an object history
 * request.</li>
 * <li>pid - required persistent identifier of the digital object.</li>
 * <li>boolean - an optional parameter indicating the requested output format.
 * A value of "true" indicates a return type of text/xml; the absence of the xml
 * parameter or a value of "false" indicates format is to be text/html.</li>
 * </ul>
 * <li>DescribeRepository URL syntax:
 * <p>
 * protocol://hostname:port/soapclient/apia?action_=DescribeRepository[&xml_=boolean]
 * </p>
 * <p>
 * This syntax requests an object profile for the specified digital object.
 * </p>
 * </li>
 * <ul>
 * <li>protocol - either http or https.</li>
 * <li>hostname - required hostname of the Fedora server.</li>
 * <li>port - required port number on which the Fedora server is running.</li>
 * <li>soapclient - required name of the sample soap client.</li>
 * <li>apia - required name of the sample soap client</li>
 * <li> DescribeRepository - required action name for getting an describe
 * repository request.</li>
 * <li>boolean - an optional parameter indicating the requested output format.
 * A value of "true" indicates a return type of text/xml; the absence of the xml
 * parameter or a value of "false" indicates format is to be text/html.</li>
 * </ul>
 * <li>ListDatastreams URL syntax:
 * <p>
 * protocol://hostname:port/soapclient/apia?action_=ListDatastreams&PID_=pid[&asOfDateTime_=dateTime][&xml_=boolean]
 * </p>
 * <p>
 * This syntax requests a list of datastreams for the specified digital object.
 * </p>
 * </li>
 * <ul>
 * <li>protocol - either http or https.</li>
 * <li>hostname - required hostname of the Fedora server.</li>
 * <li>port - required port number on which the Fedora server is running.</li>
 * <li>soapclient - required name of the sample soap client.</li>
 * <li>apia - required name of the sample soap client</li>
 * <li> ListDatastreams - required action name for getting a list datastreams
 * request.</li>
 * <li>pid - required persistent identifier of the digital object.</li>
 * <li>dateTime - optional dateTime value indicating dissemination of a version
 * of the digital object at the specified point in time.
 * <li>boolean - an optional parameter indicating the requested output format.
 * A value of "true" indicates a return type of text/xml; the absence of the xml
 * parameter or a value of "false" indicates format is to be text/html.</li>
 * </ul>
 * <li>ListMethods URL syntax:
 * <p>
 * protocol://hostname:port/soapclient/apia?action_=ListMethods&PID_=pid[&asOfDateTime_=ateTime][xml_=boolean]
 * </p>
 * <p>
 * This syntax requests a list of methods for the specified digital object.
 * </p>
 * </li>
 * <ul>
 * <li>protocol - either http or https.</li>
 * <li>hostname - required hostname of the Fedora server.</li>
 * <li>port - required port number on which the Fedora server is running.</li>
 * <li>soapclient - required name of the sample soap client.</li>
 * <li>apia - required name of the sample soap client</li>
 * <li> ListMethods - required action name for getting a list methods request.</li>
 * <li>pid - required persistent identifier of the digital object.</li>
 * <li>dateTime - optional dateTime value indicating dissemination of a version
 * of the digital object at the specified point in time.
 * <li>boolean - an optional parameter indicating the requested output format.
 * A value of "true" indicates a return type of text/xml; the absence of the xml
 * parameter or a value of "false" indicates format is to be text/html.</li>
 * </ul>
 * </ol>
 * 
 * @author rlw@virginia.edu
 * @version $Id$
 */
public class FedoraAccessSoapServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	/** Content type for html. */
	private static final String CONTENT_TYPE_HTML = "text/html; charset=UTF-8";

	/** Content type for xml. */
	private static final String CONTENT_TYPE_XML = "text/xml; charset=UTF-8";

	/** URI of Fedora API definitions. */
	private static final String FEDORA_API_URI = "http://www.fedora.info/definitions/1/0/api/";

	/** URI of Fedora Type definitions. */
	private static final String FEDORA_TYPE_URI = "http://www.fedora.info/definitions/1/0/types/";

	/** GetDissemination service name. */
	private static final String GET_DISSEMINATION = "GetDissemination";

	/** GetDatastreamDissemination service name. */
	private static final String GET_DATASTREAM_DISSEMINATION = "GetDatastreamDissemination";

	/** ListMethods service name. */
	private static final String LIST_METHODS = "ListMethods";

	/** GetObjectProfile service name. */
	private static final String GET_OBJECT_PROFILE = "GetObjectProfile";

	/** GetObjectProfile service name. */
	private static final String GET_OBJECT_HISTORY = "GetObjectHistory";

	/** DescribeRepository service name. */
	private static final String DESCRIBE_REPOSITORY = "DescribeRepository";

	/** ListDatastreams service name. */
	private static final String LIST_DATASTREAMS = "ListDatastreams";

	/** Properties file for soap client */
	private static final String soapClientPropertiesFile = "WEB-INF/soapclient.properties";

	/** URI of Fedora Access SOAP service. */
	private static String FEDORA_ACCESS_ENDPOINT = null;

	/** Servlet mapping for this servlet */
	private static String SOAP_CLIENT_SERVLET_PATH = null;

	/** Servlet mapping for MethodParmResolverServlet */
	private static String METHOD_PARM_RESOLVER_SERVLET_PATH = null;

	/** User-supplied method parameters from servlet URL. */
	private Hashtable h_userParms = null;

	/** Host name of the Fedora server * */
	private static String fedoraServerHost = null;

	/** Port number on which the Fedora server is running. * */
	private static String fedoraServerPort = null;

	/** Authenticated username to connect to Fedora server * */
	private static String fedoraServerUsername = null;

	/** Authenitcated user password to connecto to Fedora server * */
	private static String fedoraServerPassword = null;

	/** Protocol to use in connecting to Fedora server * */
	private static String fedoraServerProtocol;

	/**
	 * <p>
	 * Process Fedora Access Request. Parse and validate the servlet input
	 * parameters and then execute the specified request by calling the
	 * appropriate Fedora Access SOAP service.
	 * </p>
	 * 
	 * @param request
	 *            The servlet request.
	 * @param response
	 *            servlet The servlet response.
	 * @throws ServletException
	 *             If an error occurs that effects the servlet's basic
	 *             operation.
	 * @throws IOException
	 *             If an error occurs with an input or output operation.
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String asOfDateTime = null;
		Date versDateTime = null;
		String action = null;
		String bDefPID = null;
		String methodName = null;
		String PID = null;
		String dsID = null;
		Property[] userParms = null;
		boolean xml = false;
		long servletStartTime = new Date().getTime();
		h_userParms = new Hashtable();
		String requestURL = request.getRequestURL().toString();
		String requestProtocol = requestURL.substring(0, requestURL
				.indexOf(":"));
		if (!fedoraServerProtocol.equals((request.getRequestURL().toString())
				.substring(0, requestURL.indexOf(":")))) {
			response
					.sendError(
							HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
							"The protocol specified in the SoapClient "
									+ "properties file specifies the Fedora server protocol is: \""
									+ fedoraServerProtocol
									+ "\". The protocol "
									+ "of this request is: \""
									+ (request.getRequestURL().toString())
											.substring(0, requestURL
													.indexOf(":"))
									+ "\". "
									+ "The protocol of the initiating request must match that specified for the Fedora server. Either change "
									+ "the protocol of the initiating request or change the protocol specified in the SoapClient properties "
									+ "file so that the protocols agree.");
		}

		// Get servlet input parameters.
		Enumeration URLParms = request.getParameterNames();
		while (URLParms.hasMoreElements()) {
			String parm = URLDecoder.decode((String) URLParms.nextElement(),
					"UTF-8");
			if (parm.equals("action_")) {
				action = request.getParameter(parm);
			} else if (parm.equals("PID_")) {
				PID = URLDecoder.decode(request.getParameter(parm), "UTF-8");
			} else if (parm.equals("bDefPID_")) {
				bDefPID = URLDecoder
						.decode(request.getParameter(parm), "UTF-8");
			} else if (parm.equals("dsID_")) {
				dsID = URLDecoder.decode(request.getParameter(parm), "UTF-8");
			} else if (parm.equals("methodName_")) {
				methodName = URLDecoder.decode(request.getParameter(parm),
						"UTF-8");
			} else if (parm.equals("asOfDateTime_")) {
				asOfDateTime = request.getParameter(parm);
				versDateTime = DateUtility.convertStringToDate(asOfDateTime);
			} else if (parm.equals("xml_")) {
				xml = new Boolean(request.getParameter(parm)).booleanValue();
			} else {
				// Any remaining parameters are assumed to be user-supplied
				// method
				// parameters. Place user-supplied parameters in hashtable for
				// easier access.
				h_userParms.put(parm, URLDecoder.decode(request
						.getParameter(parm), "UTF-8"));
			}
		}

		// API-A interface requires user-supplied parameters to be of type
		// Property[]; create Property[] from hashtable of user parameters.
		int userParmCounter = 0;
		if (!h_userParms.isEmpty()) {
			userParms = new Property[h_userParms.size()];
			for (Enumeration e = h_userParms.keys(); e.hasMoreElements();) {
				Property userParm = new Property();
				userParm.setName((String) e.nextElement());
				userParm.setValue((String) h_userParms.get(userParm.getName()));
				userParms[userParmCounter] = userParm;
				userParmCounter++;
			}
		}

		// Validate servlet URL parameters to verify that all parameters
		// required
		// by the servlet are present and to verify that any other user-supplied
		// parameters are valid for the request.
		if (isValidURLParms(action, PID, bDefPID, dsID, methodName,
				versDateTime, h_userParms, response)) {

			if (action.equals(GET_DISSEMINATION)) {
				ServletOutputStream out = response.getOutputStream();
				ByteArrayInputStream dissemResult = null;
				try {
					// Call Fedora Access SOAP service to request dissemination.
					MIMETypedStream dissemination = null;
					dissemination = getDissemination(PID, bDefPID, methodName,
							userParms, asOfDateTime);
					if (dissemination != null) {
						// testing to see what's in request header that might be
						// of interest
						for (Enumeration e = request.getHeaderNames(); e
								.hasMoreElements();) {
							String name = (String) e.nextElement();
							Enumeration headerValues = request.getHeaders(name);
							StringBuffer sb = new StringBuffer();
							while (headerValues.hasMoreElements()) {
								sb.append((String) headerValues.nextElement());
							}
							String value = sb.toString();
							// System.out.println("FEDORASOAPSERVLET REQUEST
							// HEADER CONTAINED: "+name+" : "+value);
						}
						// Dissemination found. Output the mime-typed stream.
						//
						// Note that what is returned by the Fedora Access SOAP
						// service is
						// a data structure. In a browser-based environment, it
						// makes more
						// sense to return something that is "browser-friendly"
						// so the
						// returned datastructure is written back to the serlvet
						// response.
						// In a nonbrowser-based environment, one would use the
						// returned
						// data structure directly and most likely forgo this
						// transformation step.
						//
						if (dissemination.getMIMEType().equalsIgnoreCase(
								"application/fedora-redirect")) {
							// A MIME type of application/fedora-redirect
							// signals that the
							// MIMETypedStream returned from the dissemination
							// is a special
							// Fedora-specific MIME type. In this case, teh
							// Fedora server
							// will not proxy the stream, but instead perform a
							// simple
							// redirect to the URL contained within the body of
							// the
							// MIMETypedStream. This special MIME type is used
							// primarily
							// for streaming media.
							BufferedReader br = new BufferedReader(
									new InputStreamReader(
											new ByteArrayInputStream(
													dissemination.getStream())));
							StringBuffer sb = new StringBuffer();
							String line = null;
							while ((line = br.readLine()) != null) {
								sb.append(line);
							}
							response.sendRedirect(sb.toString());
						} else {
							response
									.setContentType(dissemination.getMIMEType());
							Property[] headerArray = dissemination.getHeader();
							if (headerArray != null) {
								for (int i = 0; i < headerArray.length; i++) {
									if (headerArray[i].getName() != null
											&& !(headerArray[i].getName()
													.equalsIgnoreCase("content-type"))) {
										response.addHeader(headerArray[i]
												.getName(), headerArray[i]
												.getValue());
										// System.out.println("THIS WAS ADDED TO
										// FEDORASOAPSERVLET RESPONSE HEADER
										// FROM ORIGINATING PROVIDER
										// "+headerArray[i].getName()+" :
										// "+headerArray[i].getValue());
									}
								}
							}
							int byteStream = 0;
							dissemResult = new ByteArrayInputStream(
									dissemination.getStream());
							byte[] buffer = new byte[255];
							while ((byteStream = dissemResult.read(buffer)) != -1) {
								out.write(buffer, 0, byteStream);
							}
							out.flush();
							buffer = null;
						}
					} else {
						// Dissemination request returned nothing.
						String message = "[FedoraAccessSoapServlet] No Dissemination "
								+ "result returned.  See server logs for additional info";
						System.err.println(message);
						showURLParms(action, PID, bDefPID, methodName,
								asOfDateTime, userParms, response, message);
					}
				} catch (Exception e) {
					// FIXME!! Needs more refined Exception handling.
					e.printStackTrace();
					String message = "[FedoraAccessSoapServlet] No Dissemination "
							+ "result returned.  Exception: "
							+ e.getClass().getName()
							+ "  Reason: "
							+ e.getMessage()
							+ "  See server logs for additional info";
					System.err.println(message);
					showURLParms(action, PID, bDefPID, methodName,
							asOfDateTime, userParms, response, message);
				} finally {
					try {
						if (dissemResult != null)
							dissemResult.close();
					} catch (Throwable th) {
						String message = "[FedoraAccessSoapServlet] An error has occured. "
								+ " The error was a \" "
								+ th.getClass().getName()
								+ " \". Reason: "
								+ th.getMessage();
						throw new ServletException(message);
					}
				}
				long stopTime = new Date().getTime();
				long interval = stopTime - servletStartTime;
				// System.out.println("[FedoraAccessSoapServlet] Roundtrip "
				// + "GetDissemination: " + interval + " milliseconds.");
			} else if (action.equals(GET_DATASTREAM_DISSEMINATION)) {
				ServletOutputStream out = response.getOutputStream();
				ByteArrayInputStream dissemResult = null;
				try {
					// Call Fedora Access SOAP service to request dissemination.
					MIMETypedStream dsDissemination = null;
					dsDissemination = getDatastreamDissemination(PID, dsID,
							asOfDateTime);
					if (dsDissemination != null) {
						// testing to see what's in request header that might be
						// of interest
						for (Enumeration e = request.getHeaderNames(); e
								.hasMoreElements();) {
							String name = (String) e.nextElement();
							Enumeration headerValues = request.getHeaders(name);
							StringBuffer sb = new StringBuffer();
							while (headerValues.hasMoreElements()) {
								sb.append((String) headerValues.nextElement());
							}
							String value = sb.toString();
							// System.out.println("FEDORASOAPSERVLET REQUEST
							// HEADER CONTAINED: "+name+" : "+value);
						}
						// Dissemination found. Output the mime-typed stream.
						//
						// Note that what is returned by the Fedora Access SOAP
						// service is
						// a data structure. In a browser-based environment, it
						// makes more
						// sense to return something that is "browser-friendly"
						// so the
						// returned datastructure is written back to the serlvet
						// response.
						// In a nonbrowser-based environment, one would use the
						// returned
						// data structure directly and most likely forgo this
						// transformation step.
						//
						response.setContentType(dsDissemination.getMIMEType());
						Property[] headerArray = dsDissemination.getHeader();
						if (headerArray != null) {
							for (int i = 0; i < headerArray.length; i++) {
								if (headerArray[i].getName() != null
										&& !(headerArray[i].getName()
												.equalsIgnoreCase("content-type"))) {
									response.addHeader(
											headerArray[i].getName(),
											headerArray[i].getValue());
									// System.out.println("THIS WAS ADDED TO
									// FEDORASOAPSERVLET RESPONSE HEADER FROM
									// ORIGINATING PROVIDER
									// "+headerArray[i].getName()+" :
									// "+headerArray[i].getValue());
								}
							}
						}
						int byteStream = 0;
						dissemResult = new ByteArrayInputStream(dsDissemination
								.getStream());
						byte[] buffer = new byte[255];
						while ((byteStream = dissemResult.read(buffer)) != -1) {
							out.write(buffer, 0, byteStream);
						}
						out.flush();
						buffer = null;
					} else {
						// Dissemination request returned nothing.
						String message = "[FedoraAccessSoapServlet] No Datastream Dissemination "
								+ "result returned.  See server logs for additional info";
						System.err.println(message);
						showURLParms(action, PID, dsID, methodName,
								asOfDateTime, userParms, response, message);
					}
				} catch (Exception e) {
					String message = "[FedoraAccessSoapServlet] An error has occured in "
							+ "accessing the Fedora Access Subsystem. The error was \" "
							+ e.getClass().getName()
							+ " \". Reason: "
							+ e.getMessage()
							+ "  Input Request was: \""
							+ request.getRequestURL().toString();

					// logger.logWarning(message);
					response
							.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					response.sendError(
							HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
							message);
					e.printStackTrace();
				} finally {
					try {
						if (dissemResult != null)
							dissemResult.close();
					} catch (Throwable th) {
						String message = "[FedoraAccessSoapServlet] An error has occured. "
								+ " The error was a \" "
								+ th.getClass().getName()
								+ " \". Reason: "
								+ th.getMessage();
						throw new ServletException(message);
					}
				}
				long stopTime = new Date().getTime();
				long interval = stopTime - servletStartTime;
				// System.out.println("[FedoraAccessSoapServlet] Roundtrip "
				// + "GetDissemination: " + interval + " milliseconds.");
			} else if (action.equals(LIST_METHODS)) {
				ObjectMethodsDef[] objMethDefArray = null;
				PipedWriter pw = new PipedWriter();
				PipedReader pr = new PipedReader(pw);
				OutputStreamWriter out = null;

				try {
					pw = new PipedWriter();
					pr = new PipedReader(pw);
					objMethDefArray = listMethods(PID, asOfDateTime);
					if (objMethDefArray != null) {
						// Object Methods found.
						// Deserialize ObjectmethodsDef datastructure into XML
						new ObjectMethodDefSerializerThread(PID,
								objMethDefArray, versDateTime, pw).start();
						if (xml) {
							// Return results as raw XML
							response.setContentType(CONTENT_TYPE_XML);
							out = new OutputStreamWriter(response
									.getOutputStream(), "UTF-8");
							int bufSize = 4096;
							char[] buf = new char[bufSize];
							int len = 0;
							while ((len = pr.read(buf, 0, bufSize)) != -1) {
								out.write(buf, 0, len);
							}
							out.flush();
							// int bytestream = 0;
							// while ( (bytestream = pr.read()) >= 0)
							// {
							// out.write(bytestream);
							// }
							// out.flush();
						} else {
							// Transform results into an html table
							response.setContentType(CONTENT_TYPE_HTML);
							out = new OutputStreamWriter(response
									.getOutputStream(), "UTF-8");
							TransformerFactory factory = TransformerFactory
									.newInstance();
							Templates template = factory
									.newTemplates(new StreamSource(
											this
													.getServletContext()
													.getRealPath(
															"WEB-INF/xsl/listMethods.xslt")));
							Transformer transformer = template.newTransformer();
							Properties details = template.getOutputProperties();
							transformer.setParameter("title_", new StringValue(
									"Fedora Digital Object"));
							transformer.setParameter("subtitle_",
									new StringValue("Object Methods View"));
							transformer.setParameter("soapClientServletPath",
									new StringValue(SOAP_CLIENT_SERVLET_PATH));
							transformer.setParameter(
									"soapClientMethodParmResolverServletPath",
									new StringValue(
											METHOD_PARM_RESOLVER_SERVLET_PATH));
							transformer.transform(new StreamSource(pr),
									new StreamResult(out));
						}
						out.flush();
					} else {
						// Object Methods Definition request returned nothing.
						String message = "[FedoraAccessSoapServlet] No Object Method "
								+ "Definitions returned.";
						System.out.println(message);
						showURLParms(action, PID, "", "", asOfDateTime,
								new Property[0], response, message);
						// response.setStatus(HttpServletResponse.SC_NO_CONTENT);
						// response.sendError(response.SC_NO_CONTENT, message);
					}
				} catch (Throwable th) {
					String message = "[FedoraAccessSoapServlet] An error has occured. "
							+ " The error was a \" "
							+ th.getClass().getName()
							+ " \". Reason: " + th.getMessage();
					System.out.println(message);
					th.printStackTrace();
					System.out.println(message);
					showURLParms(action, PID, "", "", asOfDateTime,
							new Property[0], response, message);
					// response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					// response.sendError(response.SC_INTERNAL_SERVER_ERROR,
					// message);
				} finally {
					try {
						if (pr != null)
							pr.close();
						if (out != null)
							out.close();
					} catch (Throwable th) {
						String message = "[FedoraAccessSoapServlet] An error has occured. "
								+ " The error was a \" "
								+ th.getClass().getName()
								+ " \". Reason: "
								+ th.getMessage();
						throw new ServletException(message);
					}
				}
				long stopTime = new Date().getTime();
				long interval = stopTime - servletStartTime;
				// System.out.println("[FedoraAccessSoapServlet] Roundtrip "
				// + "GetObjectMethods: " + interval + " milliseconds.");
			} else if (action.equals(LIST_DATASTREAMS)) {
				DatastreamDef[] datastreamDefArray = null;
				PipedWriter pw = new PipedWriter();
				PipedReader pr = new PipedReader(pw);
				OutputStreamWriter out = null;

				try {
					pw = new PipedWriter();
					pr = new PipedReader(pw);
					datastreamDefArray = listDatastreams(PID, asOfDateTime);
					if (datastreamDefArray != null) {
						// Object Methods found.
						// Deserialize ObjectmethodsDef datastructure into XML
						new DatastreamDefSerializerThread(PID,
								datastreamDefArray, versDateTime, pw).start();
						if (xml) {
							// Return results as raw XML
							response.setContentType(CONTENT_TYPE_XML);
							out = new OutputStreamWriter(response
									.getOutputStream(), "UTF-8");
							int bufSize = 4096;
							char[] buf = new char[bufSize];
							int len = 0;
							while ((len = pr.read(buf, 0, bufSize)) != -1) {
								out.write(buf, 0, len);
							}
							out.flush();
							// int bytestream = 0;
							// while ( (bytestream = pr.read()) >= 0)
							// {
							// out.write(bytestream);
							// }
							// out.flush();
						} else {
							// Transform results into an html table
							response.setContentType(CONTENT_TYPE_HTML);
							out = new OutputStreamWriter(response
									.getOutputStream(), "UTF-8");
							TransformerFactory factory = TransformerFactory
									.newInstance();
							Templates template = factory
									.newTemplates(new StreamSource(
											this
													.getServletContext()
													.getRealPath(
															"WEB-INF/xsl/listDatastreams.xslt")));
							Transformer transformer = template.newTransformer();
							Properties details = template.getOutputProperties();
							transformer.setParameter("title_", new StringValue(
									"Fedora Digital Object"));
							transformer.setParameter("subtitle_",
									new StringValue("List Methods"));
							transformer.setParameter("soapClientServletPath",
									new StringValue(SOAP_CLIENT_SERVLET_PATH));
							transformer.setParameter(
									"soapClientMethodParmResolverServletPath",
									new StringValue(
											METHOD_PARM_RESOLVER_SERVLET_PATH));
							transformer.transform(new StreamSource(pr),
									new StreamResult(out));
						}
						out.flush();
					} else {
						// Datastream Definition request returned nothing.
						String message = "[FedoraAccessSoapServlet] No Datastream "
								+ "Definitions returned.";
						System.out.println(message);
						showURLParms(action, PID, "", "", asOfDateTime,
								new Property[0], response, message);
						// response.setStatus(HttpServletResponse.SC_NO_CONTENT);
						// response.sendError(response.SC_NO_CONTENT, message);
					}
				} catch (Throwable th) {
					String message = "[FedoraAccessSoapServlet] An error has occured. "
							+ " The error was a \" "
							+ th.getClass().getName()
							+ " \". Reason: " + th.getMessage();
					System.out.println(message);
					th.printStackTrace();
					System.out.println(message);
					showURLParms(action, PID, "", "", asOfDateTime,
							new Property[0], response, message);
					// response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					// response.sendError(response.SC_INTERNAL_SERVER_ERROR,
					// message);
				} finally {
					try {
						if (pr != null)
							pr.close();
						if (out != null)
							out.close();
					} catch (Throwable th) {
						String message = "[FedoraAccessSoapServlet] An error has occured. "
								+ " The error was a \" "
								+ th.getClass().getName()
								+ " \". Reason: "
								+ th.getMessage();
						throw new ServletException(message);
					}
				}
				long stopTime = new Date().getTime();
				long interval = stopTime - servletStartTime;
				// System.out.println("[FedoraAccessSoapServlet] Roundtrip "
				// + "ListDatastreams: " + interval + " milliseconds.");
			} else if (action.equals(GET_OBJECT_PROFILE)) {
				ObjectProfile objProfile = null;
				PipedWriter pw = new PipedWriter();
				PipedReader pr = new PipedReader(pw);
				OutputStreamWriter out = null;

				try {
					// out = response.getOutputStream();
					pw = new PipedWriter();
					pr = new PipedReader(pw);
					objProfile = getObjectProfile(PID, asOfDateTime);
					if (objProfile != null) {
						// Object Profile found.
						// Deserialize ObjectProfile datastructure into XML
						new ProfileSerializerThread(PID, objProfile,
								versDateTime, pw).start();
						if (xml) {
							// Return results as raw XML
							response.setContentType(CONTENT_TYPE_XML);
							out = new OutputStreamWriter(response
									.getOutputStream(), "UTF-8");
							int bufSize = 4096;
							char[] buf = new char[bufSize];
							int len = 0;
							while ((len = pr.read(buf, 0, bufSize)) != -1) {
								out.write(buf, 0, len);
							}
							out.flush();
							// int bytestream = 0;
							// while ( (bytestream = pr.read()) >= 0)
							// {
							// out.write(bytestream);
							// }
							// out.flush();
						} else {
							// Transform results into an html table
							response.setContentType(CONTENT_TYPE_HTML);
							out = new OutputStreamWriter(response
									.getOutputStream(), "UTF-8");
							File xslFile = new File(
									this
											.getServletContext()
											.getRealPath(
													"WEB-INF/xsl/viewObjectProfile.xslt"));
							TransformerFactory factory = TransformerFactory
									.newInstance();
							Templates template = factory
									.newTemplates(new StreamSource(xslFile));
							Transformer transformer = template.newTransformer();
							Properties details = template.getOutputProperties();
							transformer.setParameter("title_", new StringValue(
									"Fedora Digital Object"));
							transformer.setParameter("subtitle_",
									new StringValue("Object Profile View"));
							transformer.setParameter("soapClientServletPath",
									new StringValue(SOAP_CLIENT_SERVLET_PATH));
							transformer.setParameter(
									"soapClientMethodParmResolverServletPath",
									new StringValue(
											METHOD_PARM_RESOLVER_SERVLET_PATH));
							transformer.transform(new StreamSource(pr),
									new StreamResult(out));
						}
						out.flush();

					} else {
						// No Object Profile returned
						String message = "[FedoraAccessSoapServlet] No Object Profile returned.";
						System.out.println(message);
						showURLParms(action, PID, "", "", asOfDateTime,
								new Property[0], response, message);
					}
				} catch (Throwable th) {
					String message = "[FedoraAccessSoapServlet] An error has occured. "
							+ " The error was a \" "
							+ th.getClass().getName()
							+ " \". Reason: " + th.getMessage();
					System.out.println(message);
					th.printStackTrace();
					System.out.println(message);
					showURLParms(action, PID, "", "", asOfDateTime,
							new Property[0], response, message);
					// response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					// response.sendError(response.SC_INTERNAL_SERVER_ERROR,
					// message);
				} finally {
					try {
						if (pr != null)
							pr.close();
						if (out != null)
							out.close();
					} catch (Throwable th) {
						String message = "[FedoraAccessSoapServlet] An error has occured. "
								+ " The error was a \" "
								+ th.getClass().getName()
								+ " \". Reason: "
								+ th.getMessage();
						throw new ServletException(message);
					}
				}
				long stopTime = new Date().getTime();
				long interval = stopTime - servletStartTime;
				// System.out.println("[FedoraAccessSoapServlet] Roundtrip "
				// + "GetObjectProfile: " + interval + " milliseconds.");
				// end Object Profile processing
			} else if (action.equals(DESCRIBE_REPOSITORY)) {
				RepositoryInfo repositoryInfo = null;
				PipedWriter pw = new PipedWriter();
				PipedReader pr = new PipedReader(pw);
				OutputStreamWriter out = null;

				try {
					pw = new PipedWriter();
					pr = new PipedReader(pw);
					repositoryInfo = describeRepository();
					if (repositoryInfo != null) {
						// Repository Info found.
						// Deserialize RepositoryInfo datastructure into XML
						new ReposInfoSerializerThread(repositoryInfo, pw)
								.start();
						if (xml) {
							// Return results as raw XML
							response.setContentType(CONTENT_TYPE_XML);
							out = new OutputStreamWriter(response
									.getOutputStream(), "UTF-8");
							int bufSize = 4096;
							char[] buf = new char[bufSize];
							int len = 0;
							while ((len = pr.read(buf, 0, bufSize)) != -1) {
								out.write(buf, 0, len);
							}
							out.flush();
						} else {
							// Transform results into an html table
							response.setContentType(CONTENT_TYPE_HTML);
							out = new OutputStreamWriter(response
									.getOutputStream(), "UTF-8");
							File xslFile = new File(
									this
											.getServletContext()
											.getRealPath(
													"WEB-INF/xsl/viewRepositoryInfo.xslt"));
							TransformerFactory factory = TransformerFactory
									.newInstance();
							Templates template = factory
									.newTemplates(new StreamSource(xslFile));
							Transformer transformer = template.newTransformer();
							Properties details = template.getOutputProperties();
							transformer.setParameter("title_", new StringValue(
									"Fedora"));
							transformer
									.setParameter("subtitle_", new StringValue(
											"Describe Repository View"));
							transformer.setParameter("soapClientServletPath",
									new StringValue(SOAP_CLIENT_SERVLET_PATH));
							transformer.setParameter(
									"soapClientMethodParmResolverServletPath",
									new StringValue(
											METHOD_PARM_RESOLVER_SERVLET_PATH));
							transformer.transform(new StreamSource(pr),
									new StreamResult(out));
						}
						out.flush();

					} else {
						// No Repository Info returned
						String message = "[FedoraAccessSoapServlet] No Repository Info returned.";
						System.out.println(message);
						showURLParms(action, "", "", "", null, new Property[0],
								response, message);
					}
				} catch (Throwable th) {
					String message = "[FedoraAccessSoapServlet] An error has occured. "
							+ " The error was a \" "
							+ th.getClass().getName()
							+ " \". Reason: " + th.getMessage();
					System.out.println(message);
					th.printStackTrace();
					System.out.println(message);
					showURLParms(action, "", "", "", null, new Property[0],
							response, message);
					// response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					// response.sendError(response.SC_INTERNAL_SERVER_ERROR,
					// message);
				} finally {
					try {
						if (pr != null)
							pr.close();
						if (out != null)
							out.close();
					} catch (Throwable th) {
						String message = "[FedoraAccessSoapServlet] An error has occured. "
								+ " The error was a \" "
								+ th.getClass().getName()
								+ " \". Reason: "
								+ th.getMessage();
						throw new ServletException(message);
					}
				}
				long stopTime = new Date().getTime();
				long interval = stopTime - servletStartTime;
			} else if (action.equals(GET_OBJECT_HISTORY)) {
				String[] objectHistory = null;
				PipedWriter pw = new PipedWriter();
				PipedReader pr = new PipedReader(pw);
				OutputStreamWriter out = null;

				try {
					pw = new PipedWriter();
					pr = new PipedReader(pw);
					objectHistory = getObjectHistory(PID);
					if (objectHistory.length > 0) {
						// Object History found.
						// Deserialize Object History datastructure into XML
						new ObjectHistorySerializerThread(PID, objectHistory,
								pw).start();
						if (xml) {
							// Return results as raw XML
							response.setContentType(CONTENT_TYPE_XML);
							out = new OutputStreamWriter(response
									.getOutputStream(), "UTF-8");
							int bufSize = 4096;
							char[] buf = new char[bufSize];
							int len = 0;
							while ((len = pr.read(buf, 0, bufSize)) != -1) {
								out.write(buf, 0, len);
							}
							out.flush();
						} else {
							// Transform results into an html table
							response.setContentType(CONTENT_TYPE_HTML);
							out = new OutputStreamWriter(response
									.getOutputStream(), "UTF-8");
							File xslFile = new File(
									this
											.getServletContext()
											.getRealPath(
													"WEB-INF/xsl/viewObjectHistory.xslt"));
							TransformerFactory factory = TransformerFactory
									.newInstance();
							Templates template = factory
									.newTemplates(new StreamSource(xslFile));
							Transformer transformer = template.newTransformer();
							Properties details = template.getOutputProperties();
							transformer.setParameter("title_", new StringValue(
									"Fedora"));
							transformer.setParameter("subtitle_",
									new StringValue("Object History View"));
							transformer.setParameter("soapClientServletPath",
									new StringValue(SOAP_CLIENT_SERVLET_PATH));
							transformer.setParameter(
									"soapClientMethodParmResolverServletPath",
									new StringValue(
											METHOD_PARM_RESOLVER_SERVLET_PATH));
							transformer.transform(new StreamSource(pr),
									new StreamResult(out));
						}
						out.flush();

					} else {
						// No Repository Info returned
						String message = "[FedoraAccessSoapServlet] No Object History returned.";
						System.out.println(message);
						showURLParms(action, "", "", "", null, new Property[0],
								response, message);
					}
				} catch (Throwable th) {
					String message = "[FedoraAccessSoapServlet] An error has occured. "
							+ " The error was a \" "
							+ th.getClass().getName()
							+ " \". Reason: " + th.getMessage();
					System.out.println(message);
					th.printStackTrace();
					System.out.println(message);
					showURLParms(action, PID, "", "", null, new Property[0],
							response, message);
				} finally {
					try {
						if (pr != null)
							pr.close();
						if (out != null)
							out.close();
					} catch (Throwable th) {
						String message = "[FedoraAccessSoapServlet] An error has occured. "
								+ " The error was a \" "
								+ th.getClass().getName()
								+ " \". Reason: "
								+ th.getMessage();
						throw new ServletException(message);
					}
				}
				long stopTime = new Date().getTime();
				long interval = stopTime - servletStartTime;
			} else {
				// Action not recognized
				String message = "[FedoraAccessSoapServlet] Requested action not recognized.";
				System.out.println(message);
				showURLParms(action, PID, "", "", asOfDateTime,
						new Property[0], response, message);
			}
		}
	}

	/**
	 * <p>
	 * A Thread to serialize a DatastreamDef object into XML.
	 * </p>
	 * 
	 */
	public class DatastreamDefSerializerThread extends Thread {
		private PipedWriter pw = null;

		private String PID = null;

		private DatastreamDef[] dsDefs = null;

		private Date versDateTime = null;

		/**
		 * <p>
		 * Constructor for ProfileSerializeThread.
		 * </p>
		 * 
		 * @param PID
		 *            The persistent identifier of the specified digital object.
		 * @param dsDefs
		 *            An array of DatastreamDefs.
		 * @param versDateTime
		 *            The version datetime stamp of the request.
		 * @param pw
		 *            A PipedWriter to which the serialization info is written.
		 */
		public DatastreamDefSerializerThread(String PID,
				DatastreamDef[] dsDefs, Date versDateTime, PipedWriter pw) {
			this.pw = pw;
			this.PID = PID;
			this.dsDefs = dsDefs;
			this.versDateTime = versDateTime;
		}

		/**
		 * <p>
		 * This method executes the thread.
		 * </p>
		 */
		public void run() {
			if (pw != null) {
				try {
					pw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
					if (versDateTime == null
							|| DateUtility.convertDateToString(versDateTime)
									.equalsIgnoreCase("")) {
						pw
								.write("<objectDatastreams "
										+ "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" "
										+ "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
										+ "xsi:schemaLocation=\"http://www.fedora.info/definitions/1/0/access/ "
										+ fedoraServerProtocol + "://"
										+ fedoraServerHost + ":"
										+ fedoraServerPort
										+ "/listDatastreams.xsd\"" + " pid=\""
										+ PID + "\" " + "baseURL=\""
										+ fedoraServerProtocol + "://"
										+ fedoraServerHost + ":"
										+ fedoraServerPort + "/fedora/\" "
										+ ">");
					} else {
						pw
								.write("<objectDatastreams "
										+ "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" "
										+ "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
										+ "xsi:schemaLocation=\"http://www.fedora.info/definitions/1/0/access/ "
										+ fedoraServerProtocol
										+ "://"
										+ fedoraServerHost
										+ ":"
										+ fedoraServerPort
										+ "/listDatastreams.xsd\""
										+ " pid=\""
										+ StreamUtility.enc(PID)
										+ "\" "
										+ "asOfDateTime=\""
										+ DateUtility
												.convertDateToString(versDateTime)
										+ "\" " + "baseURL=\""
										+ fedoraServerProtocol + "://"
										+ fedoraServerHost + ":"
										+ fedoraServerPort + "/fedora/\" "
										+ ">");
					}

					// DatastreamDef SERIALIZATION
					for (int i = 0; i < dsDefs.length; i++) {
						pw.write("    <datastream " + "dsid=\""
								+ StreamUtility.enc(dsDefs[i].getID()) + "\" "
								+ "label=\""
								+ StreamUtility.enc(dsDefs[i].getLabel())
								+ "\" " + "mimeType=\""
								+ StreamUtility.enc(dsDefs[i].getMIMEType())
								+ "\" />");
					}
					pw.write("</objectDatastreams>");
					pw.flush();
					pw.close();
				} catch (IOException ioe) {
					System.err.println("WriteThread IOException: "
							+ ioe.getMessage());
				} finally {
					try {
						if (pw != null)
							pw.close();
					} catch (IOException ioe) {
						System.err.println("WriteThread IOException: "
								+ ioe.getMessage());
					}
				}
			}
		}
	}

	/**
	 * <p>
	 * A Thread to serialize an ObjectMethodDef object into XML.
	 * </p>
	 * 
	 */
	public class ObjectMethodDefSerializerThread extends Thread {
		private PipedWriter pw = null;

		private String PID = null;

		private ObjectMethodsDef[] methodDefs = null;

		private Date versDateTime = null;

		/**
		 * <p>
		 * Constructor for ProfileSerializeThread.
		 * </p>
		 * 
		 * @param PID
		 *            The persistent identifier of the specified digital object.
		 * @param methodDefs
		 *            An array of ObjectMethodsDefs.
		 * @param versDateTime
		 *            The version datetime stamp of the request.
		 * @param pw
		 *            A PipedWriter to which the serialization info is written.
		 */
		public ObjectMethodDefSerializerThread(String PID,
				ObjectMethodsDef[] methodDefs, Date versDateTime, PipedWriter pw) {
			this.pw = pw;
			this.PID = PID;
			this.methodDefs = methodDefs;
			this.versDateTime = versDateTime;
		}

		/**
		 * <p>
		 * This method executes the thread.
		 * </p>
		 */
		public void run() {
			if (pw != null) {
				try {
					pw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
					if (versDateTime == null
							|| DateUtility.convertDateToString(versDateTime)
									.equalsIgnoreCase("")) {
						pw
								.write("<objectMethods "
										+ "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" "
										+ "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
										+ "xsi:schemaLocation=\"http://www.fedora.info/definitions/1/0/access/ "
										+ fedoraServerProtocol + "://"
										+ fedoraServerHost + ":"
										+ fedoraServerPort
										+ "/listMethods.xsd\"" + " pid=\""
										+ StreamUtility.enc(PID) + "\" "
										+ "baseURL=\"" + fedoraServerProtocol
										+ "://" + fedoraServerHost + ":"
										+ fedoraServerPort + "/fedora/\" >");
					} else {
						pw
								.write("<objectMethods "
										+ "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" "
										+ "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
										+ "xsi:schemaLocation=\"http://www.fedora.info/definitions/1/0/access/ "
										+ fedoraServerProtocol
										+ "://"
										+ fedoraServerHost
										+ ":"
										+ fedoraServerPort
										+ "/listMethods.xsd\""
										+ " pid=\""
										+ StreamUtility.enc(PID)
										+ "\" "
										+ "asOfDateTime=\""
										+ DateUtility
												.convertDateToString(versDateTime)
										+ "\" " + "baseURL=\""
										+ fedoraServerProtocol + "://"
										+ fedoraServerHost + ":"
										+ fedoraServerPort + "/fedora/\""
										+ " >");
					}

					// ObjectMethodsDef SERIALIZATION
					String nextBdef = "null";
					String currentBdef = "";
					for (int i = 0; i < methodDefs.length; i++) {
						currentBdef = methodDefs[i].getBDefPID();
						if (!currentBdef.equalsIgnoreCase(nextBdef)) {
							if (i != 0)
								pw.write("</bDef>");
							pw.write("<bDef pid=\""
									+ StreamUtility.enc(methodDefs[i]
											.getBDefPID()) + "\" >");
						}
						pw.write("<method name=\""
								+ StreamUtility.enc(methodDefs[i]
										.getMethodName()) + "\" >");
						MethodParmDef[] methodParms = methodDefs[i]
								.getMethodParmDefs();
						for (int j = 0; j < methodParms.length; j++) {
							pw.write("<methodParm parmName=\""
									+ StreamUtility.enc(methodParms[j]
											.getParmName())
									+ "\" parmDefaultValue=\""
									+ StreamUtility.enc(methodParms[j]
											.getParmDefaultValue())
									+ "\" parmRequired=\""
									+ methodParms[j].isParmRequired()
									+ "\" parmLabel=\""
									+ StreamUtility.enc(methodParms[j]
											.getParmLabel()) + "\" >");
							if (methodParms[j].getParmDomainValues().length > 0) {
								pw.write("<methodParmDomain>");
								for (int k = 0; k < methodParms[j]
										.getParmDomainValues().length; k++) {
									pw.write("<methodParmValue>"
											+ StreamUtility.enc(methodParms[j]
													.getParmDomainValues()[k])
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
					System.err.println("WriteThread IOException: "
							+ ioe.getMessage());
				} finally {
					try {
						if (pw != null)
							pw.close();
					} catch (IOException ioe) {
						System.err.println("WriteThread IOException: "
								+ ioe.getMessage());
					}
				}
			}
		}
	}

	/**
	 * <p>
	 * A Thread to serialize an ObjectProfile object into XML.
	 * </p>
	 * 
	 */
	public class ProfileSerializerThread extends Thread {
		private PipedWriter pw = null;

		private String PID = null;

		private ObjectProfile objProfile = null;

		private Date versDateTime = null;

		/**
		 * <p>
		 * Constructor for ProfileSerializeThread.
		 * </p>
		 * 
		 * @param PID
		 *            The persistent identifier of the specified digital object.
		 * @param objProfile
		 *            An object profile data structure.
		 * @param versDateTime
		 *            The version datetime stamp of the request.
		 * @param pw
		 *            A PipedWriter to which the serialization info is written.
		 */
		public ProfileSerializerThread(String PID, ObjectProfile objProfile,
				Date versDateTime, PipedWriter pw) {
			this.pw = pw;
			this.PID = PID;
			this.objProfile = objProfile;
			this.versDateTime = versDateTime;
		}

		/**
		 * <p>
		 * This method executes the thread.
		 * </p>
		 */
		public void run() {
			if (pw != null) {
				try {
					pw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
					if (versDateTime == null
							|| DateUtility.convertDateToString(versDateTime)
									.equalsIgnoreCase("")) {
						pw
								.write("<objectProfile "
										+ " xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\""
										+ " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
										+ " xsi:schemaLocation=\"http://www.fedora.info/definitions/1/0/access/"
										+ fedoraServerProtocol + "://"
										+ fedoraServerHost + ":"
										+ fedoraServerPort
										+ "/objectProfile.xsd\"" + " pid=\""
										+ PID + "\" >");
					} else {
						pw
								.write("<objectProfile "
										+ " xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\""
										+ " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
										+ " xsi:schemaLocation=\"http://www.fedora.info/definitions/1/0/access/"
										+ fedoraServerProtocol
										+ "://"
										+ fedoraServerHost
										+ ":"
										+ fedoraServerPort
										+ "/objectProfile.xsd\""
										+ " pid=\""
										+ PID
										+ "\""
										+ " dateTime=\""
										+ DateUtility
												.convertDateToString(versDateTime)
										+ "\" >");
					}

					// PROFILE FIELDS SERIALIZATION
					pw.write("<objLabel>" + objProfile.getObjLabel()
							+ "</objLabel>");
					pw.write("<objContentModel>"
							+ objProfile.getObjContentModel()
							+ "</objContentModel>");
					String cDate = objProfile.getObjCreateDate();
					pw.write("<objCreateDate>" + cDate + "</objCreateDate>");
					String mDate = objProfile.getObjLastModDate();
					pw.write("<objLastModDate>" + mDate + "</objLastModDate>");
					String objType = objProfile.getObjType();
					pw.write("<objType>");
					if (objType.equalsIgnoreCase("O")) {
						pw.write("Fedora Data Object");
					} else if (objType.equalsIgnoreCase("D")) {
						pw.write("Fedora Behavior Definition Object");
					} else if (objType.equalsIgnoreCase("M")) {
						pw.write("Fedora Behavior Mechanism Object");
					}
					pw.write("</objType>");
					pw.write("<objDissIndexViewURL>"
							+ objProfile.getObjDissIndexViewURL()
							+ "</objDissIndexViewURL>");
					pw.write("<objItemIndexViewURL>"
							+ objProfile.getObjItemIndexViewURL()
							+ "</objItemIndexViewURL>");
					pw.write("</objectProfile>");
					pw.flush();
					pw.close();
				} catch (IOException ioe) {
					System.err.println("WriteThread IOException: "
							+ ioe.getMessage());
				} finally {
					try {
						if (pw != null)
							pw.close();
					} catch (IOException ioe) {
						System.err.println("WriteThread IOException: "
								+ ioe.getMessage());
					}
				}
			}
		}
	}

	/**
	 * <p>
	 * A Thread to serialize a RepositoryInfo object into XML.
	 * </p>
	 * 
	 */
	public class ReposInfoSerializerThread extends Thread {
		private PipedWriter pw = null;

		private RepositoryInfo repositoryInfo = null;

		/**
		 * <p>
		 * Constructor for ReposInfoSerializeThread.
		 * </p>
		 * 
		 * @param repositoryInfo
		 *            A repository info data structure.
		 * @param pw
		 *            A PipedWriter to which the serialization info is written.
		 */
		public ReposInfoSerializerThread(RepositoryInfo repositoryInfo,
				PipedWriter pw) {
			this.pw = pw;
			this.repositoryInfo = repositoryInfo;
		}

		/**
		 * <p>
		 * This method executes the thread.
		 * </p>
		 */
		public void run() {
			if (pw != null) {
				try {
					pw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
					pw
							.write("<fedoraRepository "
									+ " xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\""
									+ " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
									+ " xsi:schemaLocation=\"http://www.fedora.info/definitions/1/0/access/"
									+ fedoraServerProtocol + "://"
									+ fedoraServerHost + ":" + fedoraServerPort
									+ "/fedoraRepository.xsd\">");

					// REPOSITORY INFO FIELDS SERIALIZATION
					pw.write("<repositoryName>"
							+ repositoryInfo.getRepositoryName()
							+ "</repositoryName>");
					pw.write("<repositoryBaseURL>"
							+ repositoryInfo.getRepositoryBaseURL()
							+ "</repositoryBaseURL>");
					pw.write("<repositoryVersion>"
							+ repositoryInfo.getRepositoryVersion()
							+ "</repositoryVersion>");
					pw.write("<repositoryPID>");
					pw.write("    <PID-namespaceIdentifier>"
							+ repositoryInfo.getRepositoryPIDNamespace()
							+ "</PID-namespaceIdentifier>");
					pw.write("    <PID-delimiter>" + ":" + "</PID-delimiter>");
					pw.write("    <PID-sample>" + repositoryInfo.getSamplePID()
							+ "</PID-sample>");
					String[] retainPIDs = repositoryInfo.getRetainPIDs();
					for (int i = 0; i < retainPIDs.length; i++) {
						pw.write("    <retainPID>" + retainPIDs[i]
								+ "</retainPID>");
					}
					pw.write("</repositoryPID>");
					pw.write("<repositoryOAI-identifier>");
					pw.write("    <OAI-namespaceIdentifier>"
							+ repositoryInfo.getOAINamespace()
							+ "</OAI-namespaceIdentifier>");
					pw.write("    <OAI-delimiter>" + ":" + "</OAI-delimiter>");
					pw.write("    <OAI-sample>"
							+ repositoryInfo.getSampleOAIIdentifier()
							+ "</OAI-sample>");
					pw.write("</repositoryOAI-identifier>");
					pw.write("<sampleSearch-URL>"
							+ repositoryInfo.getSampleSearchURL()
							+ "</sampleSearch-URL>");
					pw.write("<sampleAccess-URL>"
							+ repositoryInfo.getSampleAccessURL()
							+ "</sampleAccess-URL>");
					pw.write("<sampleOAI-URL>"
							+ repositoryInfo.getSampleOAIURL()
							+ "</sampleOAI-URL>");
					String[] emails = repositoryInfo.getAdminEmailList();
					for (int i = 0; i < emails.length; i++) {
						pw.write("<adminEmail>" + emails[i] + "</adminEmail>");
					}
					pw.write("</fedoraRepository>");
					pw.flush();
					pw.close();
				} catch (IOException ioe) {
					System.err.println("WriteThread IOException: "
							+ ioe.getMessage());
				} finally {
					try {
						if (pw != null)
							pw.close();
					} catch (IOException ioe) {
						System.err.println("WriteThread IOException: "
								+ ioe.getMessage());
					}
				}
			}
		}
	}

	/**
	 * <p>
	 * A Thread to serialize an ObjectHistory object into XML.
	 * </p>
	 * 
	 */
	public class ObjectHistorySerializerThread extends Thread {
		private PipedWriter pw = null;

		private String PID = null;

		private String[] objectHistoryArray = new String[0];

		/**
		 * <p>
		 * Constructor for SerializeThread.
		 * </p>
		 * 
		 * @param PID
		 *            The persistent identifier of the specified digital object.
		 * @param objectHistoryArray
		 *            An array of behavior method definitions.
		 * @param pw
		 *            A PipedWriter to which the serialization info is written.
		 */
		public ObjectHistorySerializerThread(String PID,
				String[] objectHistoryArray, PipedWriter pw) {
			this.pw = pw;
			this.PID = PID;
			this.objectHistoryArray = objectHistoryArray;
		}

		/**
		 * <p>
		 * This method executes the thread.
		 * </p>
		 */
		public void run() {
			if (pw != null) {
				try {
					pw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
					pw
							.write("<fedoraObjectHistory "
									+ " xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\""
									+ " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
									+ " xsi:schemaLocation=\"http://www.fedora.info/definitions/1/0/access/"
									+ fedoraServerProtocol + "://"
									+ fedoraServerHost + ":" + fedoraServerPort
									+ "/fedoraObjectHistory.xsd\" " + "pid=\""
									+ PID + "\" >");
					String nextBdef = "null";
					String currentBdef = "";
					for (int i = 0; i < objectHistoryArray.length; i++) {
						pw.write("<objectChangeDate>" + objectHistoryArray[i]
								+ "</objectChangeDate>");
					}
					pw.write("</fedoraObjectHistory>");
					pw.flush();
					pw.close();
				} catch (IOException ioe) {
					System.err.println("WriteThread IOException: "
							+ ioe.getMessage());
				} finally {
					try {
						if (pw != null)
							pw.close();
					} catch (IOException ioe) {
						System.err.println("WriteThread IOException: "
								+ ioe.getMessage());
					}
				}
			}
		}
	}

	/**
	 * <p>
	 * For now, treat a HTTP POST request just like a GET request.
	 * </p>
	 * 
	 * @param request
	 *            The servet request.
	 * @param response
	 *            The servlet response.
	 * @throws ServletException
	 *             If thrown by <code>doGet</code>.
	 * @throws IOException
	 *             If thrown by <code>doGet</code>.
	 * @throws ServletException
	 *             If an error occurs that effects the servlet's basic
	 *             operation.
	 */
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	/**
	 * <p>
	 * Gets a list of Behavior Definition object PIDs for the specified digital
	 * object by invoking the appropriate Fedora Access SOAP service.
	 * </p>
	 * 
	 * @param PID
	 *            The persistent identifier of the digital object.
	 * @return An array of timestamps indicating when an object component
	 *         changed.
	 * @throws Exception
	 *             If an error occurs in communicating with the Fedora Access
	 *             SOAP service.
	 */
	public String[] getObjectHistory(String PID) throws Exception {
		Service service = new Service();
		Call call = (Call) service.createCall();
		call.setUsername(fedoraServerUsername);
		call.setPassword(fedoraServerPassword);
		call.setTargetEndpointAddress(new URL(FEDORA_ACCESS_ENDPOINT));
		call.setOperationName(new QName(FEDORA_API_URI, GET_OBJECT_HISTORY));
		String[] objectHistory = (String[]) call.invoke(new Object[] { PID });
		return objectHistory;
	}

	/**
	 * <p>
	 * Gets a MIME-typed bytestream containing the result of a dissemination by
	 * invoking the appropriate Fedora Access SOAP service.
	 * 
	 * @param PID
	 *            The persistent identifier of the digital object.
	 * @param bDefPID
	 *            The persistent identifier of the Behavior Definition object.
	 * @param methodName
	 *            The name of the method.
	 * @param asOfDateTime
	 *            The version datetime stamp of the digital object.
	 * @param userParms
	 *            An array of user-supplied method parameters and values.
	 * @return A MIME-typed stream containing the dissemination result.
	 * @throws Exception
	 *             If an error occurs in communicating with the Fedora Access
	 *             SOAP service.
	 */
	public MIMETypedStream getDissemination(String PID, String bDefPID,
			String methodName, Property[] userParms, String asOfDateTime)
			throws Exception {
		// Generate a call to the Fedora SOAP service requesting the
		// GetDissemination method
		MIMETypedStream dissemination = null;
		Service service = new Service();
		Call call = (Call) service.createCall();
		call.setUsername(fedoraServerUsername);
		call.setPassword(fedoraServerPassword);
		call.setTargetEndpointAddress(new URL(FEDORA_ACCESS_ENDPOINT));
		call.setOperationName(new QName(FEDORA_API_URI, GET_DISSEMINATION));
		QName qn = new QName(FEDORA_TYPE_URI, "MIMETypedStream");
		QName qn2 = new QName(FEDORA_TYPE_URI, "Property");

		// Any Fedora-defined types required by the SOAP service must be
		// registered
		// prior to invocation so the SOAP service knows the appropriate
		// serializer/deserializer to use for these types.
		call.registerTypeMapping(MIMETypedStream.class, qn,
				new BeanSerializerFactory(MIMETypedStream.class, qn),
				new BeanDeserializerFactory(MIMETypedStream.class, qn));
		call.registerTypeMapping(fedora.server.types.gen.Property.class, qn2,
				new BeanSerializerFactory(Property.class, qn2),
				new BeanDeserializerFactory(Property.class, qn2));
		dissemination = (MIMETypedStream) call.invoke(new Object[] { PID,
				bDefPID, methodName, userParms, asOfDateTime });
		return dissemination;
	}

	public MIMETypedStream getDatastreamDissemination(String PID, String dsID,
			String asOfDateTime) throws Exception {
		// Generate a call to the Fedora SOAP service requesting the
		// GetDissemination method
		MIMETypedStream dsDissemination = null;
		Service service = new Service();
		Call call = (Call) service.createCall();
		call.setUsername(fedoraServerUsername);
		call.setPassword(fedoraServerPassword);
		call.setTargetEndpointAddress(new URL(FEDORA_ACCESS_ENDPOINT));
		call.setOperationName(new QName(FEDORA_API_URI,
				GET_DATASTREAM_DISSEMINATION));
		QName qn = new QName(FEDORA_TYPE_URI, "MIMETypedStream");

		// Any Fedora-defined types required by the SOAP service must be
		// registered
		// prior to invocation so the SOAP service knows the appropriate
		// serializer/deserializer to use for these types.
		call.registerTypeMapping(MIMETypedStream.class, qn,
				new BeanSerializerFactory(MIMETypedStream.class, qn),
				new BeanDeserializerFactory(MIMETypedStream.class, qn));
		dsDissemination = (MIMETypedStream) call.invoke(new Object[] { PID,
				dsID, asOfDateTime });
		return dsDissemination;
	}

	/**
	 * <p>
	 * Gets a list of all method definitions for the specified object by
	 * invoking the appropriate Fedora Access SOAP service.
	 * </p>
	 * 
	 * @param PID
	 *            The persistent identifier for the digital object.
	 * @param asOfDateTime
	 *            The versioning datetime stamp.
	 * @return An array of object method definitions.
	 * @throws Exception
	 *             If an error occurs in communicating with the Fedora Access
	 *             SOAP service.
	 */
	public ObjectMethodsDef[] listMethods(String PID, String asOfDateTime)
			throws Exception {
		ObjectMethodsDef[] objMethDefArray = null;
		Service service = new Service();
		Call call = (Call) service.createCall();
		call.setUsername(fedoraServerUsername);
		call.setPassword(fedoraServerPassword);
		call.setOperationName(new QName(FEDORA_API_URI, LIST_METHODS));
		QName qn = new QName(FEDORA_TYPE_URI, "ObjectMethodsDef");
		QName qn2 = new QName(FEDORA_TYPE_URI, "MethodParmDef");
		call.setTargetEndpointAddress(new URL(FEDORA_ACCESS_ENDPOINT));

		// Any Fedora-defined types required by the SOAP service must be
		// registered
		// prior to invocation so the SOAP service knows the appropriate
		// serializer/deserializer to use for these types.
		call.registerTypeMapping(ObjectMethodsDef.class, qn,
				new BeanSerializerFactory(ObjectMethodsDef.class, qn),
				new BeanDeserializerFactory(ObjectMethodsDef.class, qn));
		call.registerTypeMapping(MethodParmDef.class, qn2,
				new BeanSerializerFactory(MethodParmDef.class, qn2),
				new BeanDeserializerFactory(MethodParmDef.class, qn2));
		objMethDefArray = (ObjectMethodsDef[]) call.invoke(new Object[] { PID,
				asOfDateTime });
		return objMethDefArray;
	}

	/**
	 * <p>
	 * Gets a list of all datastream for the specified object by invoking the
	 * appropriate Fedora Access SOAP service.
	 * </p>
	 * 
	 * @param PID
	 *            The persistent identifier of the digital object.
	 * @param asOfDateTime
	 *            The versioning datetime stamp.
	 * @return An array of datastream definitions.
	 * @throws Exception
	 *             If an error occurs in communicating with the Fedora Access
	 *             SOAP service.
	 */
	public DatastreamDef[] listDatastreams(String PID, String asOfDateTime)
			throws Exception {
		DatastreamDef[] datastreamDefArray = null;
		Service service = new Service();
		Call call = (Call) service.createCall();
		call.setUsername(fedoraServerUsername);
		call.setPassword(fedoraServerPassword);
		call.setOperationName(new QName(FEDORA_API_URI, LIST_DATASTREAMS));
		QName qn = new QName(FEDORA_TYPE_URI, "DatastreamDef");
		call.setTargetEndpointAddress(new URL(FEDORA_ACCESS_ENDPOINT));

		// Any Fedora-defined types required by the SOAP service must be
		// registered
		// prior to invocation so the SOAP service knows the appropriate
		// serializer/deserializer to use for these types.
		call.registerTypeMapping(DatastreamDef.class, qn,
				new BeanSerializerFactory(DatastreamDef.class, qn),
				new BeanDeserializerFactory(DatastreamDef.class, qn));
		datastreamDefArray = (DatastreamDef[]) call.invoke(new Object[] { PID,
				asOfDateTime });
		return datastreamDefArray;
	}

	/**
	 * <p>
	 * Gets a object profile for the specified object by invoking the
	 * appropriate Fedora Access SOAP service.
	 * </p>
	 * 
	 * @param PID
	 *            The persistent identifier for the digital object.
	 * @param asOfDateTime
	 *            The versioning datetime stamp.
	 * @return An object profile data structure.
	 * @throws Exception
	 *             If an error occurs in communicating with the Fedora Access
	 *             SOAP service.
	 */
	public ObjectProfile getObjectProfile(String PID, String asOfDateTime)
			throws Exception {
		ObjectProfile objProfile = null;
		Service service = new Service();
		Call call = (Call) service.createCall();
		call.setUsername(fedoraServerUsername);
		call.setPassword(fedoraServerPassword);
		call.setOperationName(new QName(FEDORA_API_URI, GET_OBJECT_PROFILE));
		QName qn = new QName(FEDORA_TYPE_URI, "ObjectProfile");
		call.setTargetEndpointAddress(new URL(FEDORA_ACCESS_ENDPOINT));

		// Any Fedora-defined types required by the SOAP service must be
		// registered
		// prior to invocation so the SOAP service knows the appropriate
		// serializer/deserializer to use for these types.
		call.registerTypeMapping(ObjectProfile.class, qn,
				new BeanSerializerFactory(ObjectProfile.class, qn),
				new BeanDeserializerFactory(ObjectProfile.class, qn));
		objProfile = (ObjectProfile) call.invoke(new Object[] { PID,
				asOfDateTime });
		return objProfile;
	}

	/**
	 * <p>
	 * Gets repository information for the server by invoking the appropriate
	 * Fedora Access SOAP service.
	 * </p>
	 * 
	 * @return A repository information data structure.
	 * @throws Exception
	 *             If an error occurs in communicating with the Fedora Access
	 *             SOAP service.
	 */
	public RepositoryInfo describeRepository() throws Exception {
		RepositoryInfo repositoryInfo = null;
		Service service = new Service();
		Call call = (Call) service.createCall();
		call.setUsername(fedoraServerUsername);
		call.setPassword(fedoraServerPassword);
		call.setOperationName(new QName(FEDORA_API_URI, DESCRIBE_REPOSITORY));
		QName qn = new QName(FEDORA_TYPE_URI, "RepositoryInfo");
		call.setTargetEndpointAddress(new URL(FEDORA_ACCESS_ENDPOINT));

		// Any Fedora-defined types required by the SOAP service must be
		// registered
		// prior to invocation so the SOAP service knows the appropriate
		// serializer/deserializer to use for these types.
		call.registerTypeMapping(RepositoryInfo.class, qn,
				new BeanSerializerFactory(RepositoryInfo.class, qn),
				new BeanDeserializerFactory(RepositoryInfo.class, qn));
		repositoryInfo = (RepositoryInfo) call.invoke(new Object[] {});
		return repositoryInfo;
	}

	/**
	 * <p>
	 * Lists the specified fields of each object matching the given criteria.
	 * </p>
	 * 
	 * @param resultFields
	 *            the names of the fields to return
	 * @param maxResults
	 *            the maximum number of results to return at a time
	 * @param query
	 *            the query
	 * @return the specified fields of each object matching the given criteria.
	 * @throws Exception
	 *             If an error occurs in communicating with the Fedora Access
	 *             SOAP service.
	 */
	public FieldSearchResult findObjects(String[] resultFields, int maxResults,
			FieldSearchQuery query) throws Exception {
		FieldSearchResult fieldSearchResult = null;
		Service service = new Service();
		Call call = (Call) service.createCall();
		call.setUsername(fedoraServerUsername);
		call.setPassword(fedoraServerPassword);
		call.setOperationName(new QName(FEDORA_API_URI, DESCRIBE_REPOSITORY));
		QName qn = new QName(FEDORA_TYPE_URI, "FieldSearchResult");
		QName qn2 = new QName(FEDORA_TYPE_URI, "FieldSearchQuery");
		call.setTargetEndpointAddress(new URL(FEDORA_ACCESS_ENDPOINT));

		// Any Fedora-defined types required by the SOAP service must be
		// registered
		// prior to invocation so the SOAP service knows the appropriate
		// serializer/deserializer to use for these types.
		call.registerTypeMapping(FieldSearchResult.class, qn,
				new BeanSerializerFactory(FieldSearchResult.class, qn),
				new BeanDeserializerFactory(FieldSearchResult.class, qn));
		call.registerTypeMapping(FieldSearchQuery.class, qn2,
				new BeanSerializerFactory(FieldSearchQuery.class, qn2),
				new BeanDeserializerFactory(FieldSearchQuery.class, qn2));
		fieldSearchResult = (FieldSearchResult) call.invoke(new Object[] {
				resultFields, new Integer(maxResults), query });
		return fieldSearchResult;
	}

	/**
	 * <p>
	 * Resumes an in-progress listing of object fields.
	 * </p>
	 * 
	 * @param sessionToken
	 *            the token of the session in which the remaining results can be
	 *            obtained
	 * @return the remaining specified fields of each object matching the given
	 *         criteria.
	 * @throws Exception
	 *             If an error occurs in communicating with the Fedora Access
	 *             SOAP service.
	 */
	public FieldSearchResult resumeFindObjects(String sessionToken)
			throws Exception {
		FieldSearchResult fieldSearchResult = null;
		Service service = new Service();
		Call call = (Call) service.createCall();
		call.setUsername(fedoraServerUsername);
		call.setPassword(fedoraServerPassword);
		call.setOperationName(new QName(FEDORA_API_URI, DESCRIBE_REPOSITORY));
		QName qn = new QName(FEDORA_TYPE_URI, "FieldSearchResult");
		call.setTargetEndpointAddress(new URL(FEDORA_ACCESS_ENDPOINT));

		// Any Fedora-defined types required by the SOAP service must be
		// registered
		// prior to invocation so the SOAP service knows the appropriate
		// serializer/deserializer to use for these types.
		call.registerTypeMapping(FieldSearchResult.class, qn,
				new BeanSerializerFactory(FieldSearchResult.class, qn),
				new BeanDeserializerFactory(FieldSearchResult.class, qn));
		fieldSearchResult = (FieldSearchResult) call
				.invoke(new Object[] { sessionToken });
		return fieldSearchResult;
	}

	/**
	 * <p>
	 * Initialize servlet.
	 * </p>
	 * 
	 * @throws ServletException
	 *             If the servet cannot be initialized.
	 */
	public void init() throws ServletException {
		try {
			System.out
					.println("Realpath Properties File: "
							+ getServletContext().getRealPath(
									soapClientPropertiesFile));
			FileInputStream fis = new FileInputStream(this.getServletContext()
					.getRealPath(soapClientPropertiesFile));
			Properties p = new Properties();
			p.load(fis);
			FEDORA_ACCESS_ENDPOINT = p.getProperty("fedoraEndpoint");
			SOAP_CLIENT_SERVLET_PATH = p.getProperty("soapClientServletPath");
			METHOD_PARM_RESOLVER_SERVLET_PATH = p
					.getProperty("soapClientMethodParmResolverServletPath");
			fedoraServerUsername = p.getProperty("fedoraServerUsername");
			fedoraServerPassword = p.getProperty("fedoraServerPassword");
			System.out.println("FedoraEndpoint: " + FEDORA_ACCESS_ENDPOINT);
			System.out.println("soapClientServletPath: "
					+ SOAP_CLIENT_SERVLET_PATH);
			System.out.println("soapClientMethodParmResolverServletPath: "
					+ METHOD_PARM_RESOLVER_SERVLET_PATH);
			System.out.println("fedoraServerUsername: " + fedoraServerUsername);
			System.out.println("fedoraServerPassword: " + fedoraServerPassword);
			// Locations of the internal Fedora XML schemas are local to the
			// Fedora server so it is
			// the Fedora server hostname and port number are extracted from the
			// FEDORA_ACCESS_ENDPOINT string for easier access within the
			// servlet.
			int i = FEDORA_ACCESS_ENDPOINT.indexOf(":", 8);
			int j = FEDORA_ACCESS_ENDPOINT.indexOf("/", i);
			int k = FEDORA_ACCESS_ENDPOINT.indexOf(":");
			fedoraServerHost = FEDORA_ACCESS_ENDPOINT.substring(k + 3, i);
			fedoraServerPort = FEDORA_ACCESS_ENDPOINT.substring(i + 1, j);
			fedoraServerProtocol = FEDORA_ACCESS_ENDPOINT.substring(0, k);
			System.out.println("fedoraServerHost: " + fedoraServerHost);
			System.out.println("fedoraServerPort: " + fedoraServerPort);
			System.out.println("fedoraServerProtocol: " + fedoraServerProtocol);

		} catch (Throwable th) {
			String message = "[FedoraSOAPServlet] An error has occurred. "
					+ "The error was a \"" + th.getClass().getName()
					+ "\"  . The " + "Reason: \"" + th.getMessage() + "\"  .";
			throw new ServletException(message);
		}
	}

	/**
	 * <p>
	 * Cleans up servlet resources.
	 * </p>
	 */
	public void destroy() {
	}

	/**
	 * <p>
	 * Validates required servlet URL parameters. Different parameters are
	 * required based on the requested action.
	 * </p>
	 * 
	 * @param action
	 *            The Fedora service to be executed
	 * @param PID
	 *            The persistent identifier of the Digital Object.
	 * @param bDefPID
	 *            The persistent identifier of the Behavior Definition object.
	 * @param methodName
	 *            The method name.
	 * @param versDateTime
	 *            The version datetime stamp of the digital object.
	 * @param h_userParms
	 *            A hashtabe of user-supplied method parameters.
	 * @param response
	 *            The servlet response.
	 * @return True if required parameters are valid; false otherwise.
	 * @throws IOException
	 *             If an error occurrs with an input or output operation.
	 */
	private boolean isValidURLParms(String action, String PID, String bDefPID,
			String dsID, String methodName, Date versDateTime,
			Hashtable h_userParms, HttpServletResponse response)
			throws IOException {
		// Check for missing parameters required either by the servlet or the
		// requested Fedora Access SOAP service.
		boolean isValid = true;
		ServletOutputStream out = response.getOutputStream();
		String versDate = DateUtility.convertDateToString(versDateTime);
		StringBuffer html = new StringBuffer();
		if (action != null && action.equals(GET_DISSEMINATION)) {
			if (PID == null || bDefPID == null || methodName == null) {
				// Dissemination requires PID, bDefPID, and methodName;
				// asOfDateTime is optional.
				response.setContentType(CONTENT_TYPE_HTML);
				html.append("<html>");
				html.append("<head>");
				html.append("<title>FedoraAccessSOAPServlet</title>");
				html.append("</head>");
				html.append("<body>");
				html.append("<p><font size='+1' color='red'>"
						+ "Required parameter missing "
						+ "in Dissemination Request:</font></p>");
				html.append("<table cellpadding='5'>");
				html.append("<tr>");
				html.append("<td><font color='red'>action_</font></td>");
				html.append("<td> = </td>");
				html.append("<td>" + action + "</td>");
				html.append("<td><font color='blue'>(REQUIRED)</font></td>");
				html.append("</tr>");
				html.append("<tr>");
				html.append("<td><font color='red'>PID_</font></td>");
				html.append("<td> = </td>");
				html.append("<td>" + PID + "</td>");
				html.append("<td><font color='blue'>(REQUIRED)</font></td>");
				html.append("</tr>");
				html.append("<tr>");
				html.append("<td><font color='red'>bDefPID_</font></td>");
				html.append("<td> = </td><td>" + bDefPID + "</td>");
				html.append("<td><font color='blue'>(REQUIRED)</font></td>");
				html.append("</tr>");
				html.append("<tr>");
				html.append("<td><font color='red'>methodName_</font></td>");
				html.append("<td> = </td>");
				html.append("<td>" + methodName + "</td>");
				html.append("<td><font color='blue'>(REQUIRED)</font></td>");
				html.append("</tr>");
				html.append("<tr>");
				html.append("<td><font color='red'>asOfDateTime_</font></td>");
				html.append("<td> = </td>");
				html.append("<td>" + versDate + "</td>");
				html.append("<td><font color='green'>(OPTIONAL)</font></td>");
				html.append("</tr>");
				html.append("<tr>");
				html.append("</tr>");
				html.append("<tr>");
				html.append("<td colspan='5'><font size='+1' color='blue'>"
						+ "Other Parameters Found:</font></td>");
				html.append("</tr>");
				html.append("<tr>");
				html.append("</tr>");
				for (Enumeration e = h_userParms.keys(); e.hasMoreElements();) {
					String name = (String) e.nextElement();
					html.append("<tr>");
					html.append("<td><font color='red'>" + name
							+ "</font></td>");
					html.append("<td>= </td>");
					html.append("<td>" + h_userParms.get(name) + "</td>");
					html.append("</tr>");
				}
				html.append("</table>");
				html.append("</body>");
				html.append("</html>");
				out.println(html.toString());
				isValid = false;
			}
			// FIXME!! Validation for any user-supplied parameters not
			// implemented.
		} else if (action != null
				&& (action.equals(GET_DATASTREAM_DISSEMINATION))) {
			if (PID == null || dsID == null) {

				response.setContentType(CONTENT_TYPE_HTML);
				html.append("<html>");
				html.append("<head>");
				html.append("<title>FedoraAccessSOAPServlet</title>");
				html.append("</head>");
				html.append("<body>");
				html.append("<p><font size='+1' color='red'>"
						+ "Required parameter missing in " + action
						+ " Request:</font></p>");
				html.append("<table cellpadding='5'>");
				html.append("<tr>");
				html.append("<td><font color='red'>action_</td>");
				html.append("<td> = </td>");
				html.append("<td>" + action + "</td>");
				html.append("<td><font color='blue'>(REQUIRED)</font></td>");
				html.append("</tr>");
				html.append("<tr>");
				html.append("<td><font color='red'>PID_</td>");
				html.append("<td> = </td>");
				html.append("<td>" + PID + "</td>");
				html.append("<td><font color='blue'>(REQUIRED)</font></td>");
				html.append("</tr>");
				html.append("<tr>");
				html.append("<td><font color='red'>dsID_</td>");
				html.append("<td> = </td>");
				html.append("<td>" + PID + "</td>");
				html.append("<td><font color='blue'>(REQUIRED)</font></td>");
				html.append("</tr>");
				html.append("<tr>");
				html.append("<td><font color='red'>asOfDateTime_</td>");
				html.append("<td> = </td>");
				html.append("<td>" + versDate + "</td>");
				html.append("<td><font color='green'>(OPTIONAL)</font></td>");
				html.append("</tr>");
				html.append("<tr>");
				html.append("</tr>");
				html.append("<tr>");
				html.append("<td colspan='5'><font size='+1' color='blue'>"
						+ "Other Parameters Found:</font></td>");
				html.append("</tr>");
				html.append("<tr>");
				html.append("</tr>");
				for (Enumeration e = h_userParms.keys(); e.hasMoreElements();) {
					String name = (String) e.nextElement();
					html.append("<tr>");
					html.append("<td><font color='red'>" + name
							+ "</font></td>");
					html.append("<td>= </td>");
					html.append("<td>" + h_userParms.get(name) + "</td>");
					html.append("</tr>");
				}
				html.append("</table>");
				html.append("</body>");
				html.append("</html>");
				out.println(html.toString());
				isValid = false;
			}
		} else if (action != null
				&& (action.equals(LIST_DATASTREAMS)
						|| action.equals(LIST_METHODS) || action
						.equals(GET_OBJECT_PROFILE))) {
			if (PID == null) {

				response.setContentType(CONTENT_TYPE_HTML);
				html.append("<html>");
				html.append("<head>");
				html.append("<title>FedoraAccessSOAPServlet</title>");
				html.append("</head>");
				html.append("<body>");
				html.append("<p><font size='+1' color='red'>"
						+ "Required parameter missing in " + action
						+ " Request:</font></p>");
				html.append("<table cellpadding='5'>");
				html.append("<tr>");
				html.append("<td><font color='red'>action_</td>");
				html.append("<td> = </td>");
				html.append("<td>" + action + "</td>");
				html.append("<td><font color='blue'>(REQUIRED)</font></td>");
				html.append("</tr>");
				html.append("<tr>");
				html.append("<td><font color='red'>PID_</td>");
				html.append("<td> = </td>");
				html.append("<td>" + PID + "</td>");
				html.append("<td><font color='blue'>(REQUIRED)</font></td>");
				html.append("</tr>");
				html.append("<tr>");
				html.append("<td><font color='red'>asOfDateTime_</td>");
				html.append("<td> = </td>");
				html.append("<td>" + versDate + "</td>");
				html.append("<td><font color='green'>(OPTIONAL)</font></td>");
				html.append("</tr>");
				html.append("<tr>");
				html.append("</tr>");
				html.append("<tr>");
				html.append("<td colspan='5'><font size='+1' color='blue'>"
						+ "Other Parameters Found:</font></td>");
				html.append("</tr>");
				html.append("<tr>");
				html.append("</tr>");
				for (Enumeration e = h_userParms.keys(); e.hasMoreElements();) {
					String name = (String) e.nextElement();
					html.append("<tr>");
					html.append("<td><font color='red'>" + name
							+ "</font></td>");
					html.append("<td>= </td>");
					html.append("<td>" + h_userParms.get(name) + "</td>");
					html.append("</tr>");
				}
				html.append("</table>");
				html.append("</body>");
				html.append("</html>");
				out.println(html.toString());
				isValid = false;
			}
		} else if (action != null && action.equals(GET_OBJECT_HISTORY)) {
			System.out.println("action: " + action + " PID: " + PID
					+ "isValid: " + isValid);
			if (PID == null) {
				// GetObjectHistory requires PID;
				// xml is optional.
				response.setContentType(CONTENT_TYPE_HTML);
				html.append("<html>");
				html.append("<head>");
				html.append("<title>FedoraAccessSOAPServlet</title>");
				html.append("</head>");
				html.append("<body>");
				html.append("<p><font size='+1' color='red'>"
						+ "Required parameter missing in " + action
						+ " Request:</font></p>");
				html.append("<table cellpadding='5'>");
				html.append("<tr>");
				html.append("<td><font color='red'>action_</td>");
				html.append("<td> = </td>");
				html.append("<td>" + action + "</td>");
				html.append("<td><font color='blue'>(REQUIRED)</font></td>");
				html.append("</tr>");
				html.append("<tr>");
				html.append("<td><font color='red'>PID_</td>");
				html.append("<td> = </td>");
				html.append("<td>" + PID + "</td>");
				html.append("<td><font color='blue'>(REQUIRED)</font></td>");
				html.append("</tr>");
				html.append("<tr>");
				html.append("<td><font color='red'>asOfDateTime_</td>");
				html.append("<td> = </td>");
				html.append("<td>" + versDate + "</td>");
				html.append("<td><font color='green'>(OPTIONAL)</font></td>");
				html.append("</tr>");
				html.append("<tr>");
				html.append("</tr>");
				html.append("<tr>");
				html.append("<td colspan='5'><font size='+1' color='blue'>"
						+ "Other Parameters Found:</font></td>");
				html.append("</tr>");
				html.append("<tr>");
				html.append("</tr>");
				for (Enumeration e = h_userParms.keys(); e.hasMoreElements();) {
					String name = (String) e.nextElement();
					html.append("<tr>");
					html.append("<td><font color='red'>" + name
							+ "</font></td>");
					html.append("<td>= </td>");
					html.append("<td>" + h_userParms.get(name) + "</td>");
					html.append("</tr>");
				}
				html.append("</table>");
				html.append("</body>");
				html.append("</html>");
				out.println(html.toString());
				isValid = false;
			}
		} else if (action != null
				&& (action.equalsIgnoreCase(DESCRIBE_REPOSITORY))) {
			System.out
					.println("Validated DESCRIBE_REPOSITORY as good request w/no parms");
			isValid = true;
		} else {
			System.out.println("action: " + action + " PID: " + PID
					+ " isValid: " + isValid);
			System.out.println("Unknown API-A request encountered.");
			// Unknown Fedora service has been requested.
			response.setContentType(CONTENT_TYPE_HTML);
			html.append("<html>");
			html.append("<head>");
			html.append("<title>FedoraAccessSOAPServlet</title>");
			html.append("</head>");
			html.append("<body>");
			html.append("<p><font size='+1' color='red'>Invalid 'action' "
					+ "parameter specified in Servlet Request: action= "
					+ action + "<p>");
			html.append("<br></br><font color='blue'>Reserved parameters "
					+ "in Request:</font>");
			html.append("<table cellpadding='5'>");
			html.append("<tr>");
			html.append("<td><font color='red'>action_</td>");
			html.append("<td> = </td>");
			html.append("<td>" + action + "</td>");
			html.append("</tr>");
			html.append("<tr>");
			html.append("<td><font color='red'>PID_</td>");
			html.append("<td> = </td>");
			html.append("<td>" + PID + "</td>");
			html.append("</tr>");
			html.append("<tr>");
			html.append("<td><font color='red'>bDefPID_</td>");
			html.append("<td> = </td>");
			html.append("<td>" + bDefPID + "</td>");
			html.append("</tr>");
			html.append("<tr>");
			html.append("<td><font color='red'>methodName_</td>");
			html.append("<td> = </td>");
			html.append("<td>" + methodName + "</td>");
			html.append("</tr>");
			html.append("<tr>");
			html.append("<td><font color='red'>asOfDateTime_</td>");
			html.append("<td> = </td>");
			html.append("<td>" + versDate + "</td>");
			html.append("</tr>");
			html.append("<tr>");
			html.append("</tr>");
			html.append("<tr>");
			html.append("<td colspan='5'><font size='+1' color='blue'>"
					+ "Other Parameters Found:</font></td>");
			html.append("</tr>");
			html.append("<tr>");
			html.append("</tr>");
			for (Enumeration e = h_userParms.keys(); e.hasMoreElements();) {
				String name = (String) e.nextElement();
				html.append("<tr>");
				html.append("<td><font color='red'>" + name + "</font></td>");
				html.append("<td>= </td>");
				html.append("<td>" + h_userParms.get(name) + "</td>");
				html.append("</tr>");
			}
			html.append("</table>");
			html.append("</body>");
			html.append("</html>");
			out.println(html.toString());
			isValid = false;
		}

		return isValid;
	}

	/**
	 * <p>
	 * Displays a list of the servlet input parameters. This method is generally
	 * called when a service request returns no data. Usually this is a result
	 * of an incorrect spelling of either a required URL parameter or in one of
	 * the user-supplied parameters. The output from this method can be used to
	 * help verify the URL parameters sent to the servlet and hopefully fix the
	 * problem.
	 * </p>
	 * 
	 * @param action
	 *            The Fedora service requested.
	 * @param PID
	 *            The persistent identifier of the digital object.
	 * @param bDefPID
	 *            The persistent identifier of the Behavior Definition object.
	 * @param methodName
	 *            the name of the method.
	 * @param asOfDateTime
	 *            The version datetime stamp of the digital object.
	 * @param userParms
	 *            An array of user-supplied method parameters and values.
	 * @param response
	 *            The servlet response.
	 * @param message
	 *            The message text to include at the top of the output page.
	 * @throws IOException
	 *             If an error occurrs with an input or output operation.
	 */
	private void showURLParms(String action, String PID, String bDefPID,
			String methodName, String asOfDateTime, Property[] userParms,
			HttpServletResponse response, String message) throws IOException {

		ServletOutputStream out = response.getOutputStream();
		response.setContentType(CONTENT_TYPE_HTML);

		// Display servlet input parameters
		StringBuffer html = new StringBuffer();
		html.append("<html>");
		html.append("<head>");
		html.append("<title>FedoraAccessSOAPServlet</title>");
		html.append("</head>");
		html.append("<body>");
		html.append("<br></br><font size='+2'>" + message + "</font>");
		html.append("<br></br><font color='red'>Request Parameters</font>");
		html.append("<br></br>");
		html.append("<table cellpadding='5'>");
		html.append("<tr>");
		html.append("<td><font color='red'>action_</font></td>");
		html.append("<td> = </td>");
		html.append("<td>" + action + "</td>");
		html.append("</tr>");
		html.append("<tr>");
		html.append("<td><font color='red'>PID_</font></td>");
		html.append("<td> = </td>");
		html.append("<td>" + PID + "</td>");
		html.append("</tr>");
		html.append("<tr>");
		html.append("<td><font color='red'>bDefPID_</font></td>");
		html.append("<td> = </td>");
		html.append("<td>" + bDefPID + "</td>");
		html.append("</tr>");
		html.append("<tr>");
		html.append("<td><font color='red'>methodName_</font></td>");
		html.append("<td> = </td>");
		html.append("<td>" + methodName + "</td>");
		html.append("</tr>");
		html.append("<tr>");
		html.append("<td><font color='red'>asOfDateTime_</font></td>");
		html.append("<td> = </td>");
		html.append("<td>" + asOfDateTime + "</td>");
		html.append("</tr>");
		html.append("<tr>");
		html.append("</tr>");
		html.append("<tr>");
		html.append("<td colspan='5'><font size='+1' color='blue'>"
				+ "Other Parameters Found:</font></td>");
		html.append("</tr>");
		html.append("<tr>");
		html.append("</tr>");

		// List user-supplied parameters if any
		if (userParms != null) {
			for (int i = 0; i < userParms.length; i++) {
				html.append("<tr>");
				html.append("<td><font color='red'>" + userParms[i].getName()
						+ "</font></td>");
				html.append("<td> = </td>");
				html.append("<td>" + userParms[i].getValue() + "</td>");
				html.append("</tr>");
			}
		}
		html.append("</table>");
		html.append("</body></html>");
		out.println(html.toString());

		System.err.println("REQUEST Returned NO Data");
	}
}
