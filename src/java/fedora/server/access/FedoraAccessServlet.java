package fedora.server.access;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import java.util.Hashtable;
import java.util.Properties;

import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;

import org.apache.log4j.Logger;

import fedora.common.Constants;

import fedora.server.Context;
import fedora.server.ReadOnlyContext;
import fedora.server.Server;
import fedora.server.errors.InitializationException;
import fedora.server.errors.GeneralException;
import fedora.server.errors.ServerException;
import fedora.server.errors.StreamIOException;
import fedora.server.errors.authorization.AuthzException;
import fedora.server.errors.servletExceptionExtensions.InternalError500Exception;
import fedora.server.errors.servletExceptionExtensions.RootException;
import fedora.server.storage.DOManager;
import fedora.server.storage.types.MIMETypedStream;
import fedora.server.storage.types.Property;
import fedora.server.utilities.DateUtility;
import fedora.server.utilities.StreamUtility;

/**
 * <p>
 * <b>Title: </b>FedoraAccessServlet.java
 * </p>
 * <p>
 * <b>Description: </b>Implements the three methods GetObjectProfile,
 * GetDissemination, and GetDatastreamDissemination of the Fedora Access LITE
 * (API-A-LITE) interface using a java servlet front end. The syntax defined by
 * API-A-LITE defines three bindings for these methods:
 * <ol>
 * <li>GetDissemination URL syntax:
 * <p>
 * protocol://hostname:port/fedora/get/PID/bDefPID/methodName[/dateTime][?parmArray]
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
 * <li>fedora - required path name for the Fedora access service.</li>
 * <li>get - required path name for the Fedora service.</li>
 * <li>PID - required persistent idenitifer of the digital object.</li>
 * <li>bDefPID - required persistent identifier of the behavior definition
 * object to which the digital object subscribes.</li>
 * <li>methodName - required name of the method to be executed.</li>
 * <li>dateTime - optional dateTime value indicating dissemination of a version
 * of the digital object at the specified point in time.
 * <li>parmArray - optional array of method parameters consisting of name/value
 * pairs in the form parm1=value1&parm2=value2...</li>
 * </ul>
 * <li>GetObjectProfile URL syntax:
 * <p>
 * protocol://hostname:port/fedora/get/PID[/dateTime][?xml=BOOLEAN]
 * </p>
 * <p>
 * This syntax requests an object profile for the specified digital object. The
 * xml parameter determines the type of output returned. If the parameter is
 * omitted or has a value of "false", a MIME-typed stream consisting of an html
 * table is returned providing a browser-savvy means of viewing the object
 * profile. If the value specified is "true", then a MIME-typed stream
 * consisting of XML is returned.
 * </p>
 * </li>
 * <ul>
 * <li>protocol - either http or https</li>
 * <li>hostname - required hostname of the Fedora server.</li>
 * <li>port - required port number on which the Fedora server is running.</li>
 * <li>fedora - required name of the Fedora access service.</li>
 * <li>get - required verb of the Fedora service.</li>
 * <li>PID - required persistent identifier of the digital object.</li>
 * <li>dateTime - optional dateTime value indicating dissemination of a version
 * of the digital object at the specified point in time.
 * <li>xml - an optional parameter indicating the requested output format. A
 * value of "true" indicates a return type of text/xml; the absence of the xml
 * parameter or a value of "false" indicates format is to be text/html.</li>
 * </ul>
 * <li>GetDatastreamDissemination URL syntax:
 * <p>
 * protocol://hostname:port/fedora/get/PID/DSID[/dateTime]
 * </p>
 * This syntax requests a datastream dissemination for the specified digital
 * object. It is used to return the contents of a datastream.
 * <p>
 * </li>
 * <ul>
 * <li>protocol - either http or https.</li>
 * <li>hostname - required hostname of the Fedora server.</li>
 * <li>port - required port number on which the Fedora server is running.</li>
 * <li>fedora - required name of the Fedora access service.</li>
 * <li>get - required verb of the Fedora service.</li>
 * <li>PID - required persistent identifier of the digital object.</li>
 * <li>DSID - required datastream identifier for the datastream.</li>
 * <li>dateTime - optional dateTime value indicating dissemination of a version
 * of the digital object at the specified point in time.
 * </ul>
 * </ol>
 * 
 * @author rlw@virginia.edu
 * @version $Id$
 */
public class FedoraAccessServlet extends HttpServlet {

    /** Logger for this class. */
    private static final Logger LOG = Logger.getLogger(
            FedoraAccessServlet.class.getName());

	private static final long serialVersionUID = 1L;

	/** Content type for html. */
	private static final String CONTENT_TYPE_HTML = "text/html; charset=UTF-8";

	/** Content type for xml. */
	private static final String CONTENT_TYPE_XML = "text/xml; charset=UTF-8";

	/** Instance of the Fedora server. */
	private static Server s_server = null;

	/** Instance of the access subsystem. */
	private static Access s_access = null;

	/** Instance of DOManager. */
	private static DOManager m_manager = null;

	/** userInputParm hashtable */
	private Hashtable h_userParms = new Hashtable();

	/** Initial URL request by client */
	private String requestURL = null;

	/** Portion of initial request URL from protocol up to query string */
	private String requestURI = null;

	/** Instance of URLDecoder */
	private URLDecoder decoder = new URLDecoder();

	/** Fedora server protocl * */
	private String fedoraServerProtocol = null;

	/** HTTP protocol * */
	private static String HTTP = "http";

	/** HTTPS protocol * */
	private static String HTTPS = "https";

	/** Configured Fedora server hostname */
	private static String fedoraServerHost = null;

	/**
	 * <p>
	 * Process Fedora Access Request. Parse and validate the servlet input
	 * parameters and then execute the specified request.
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
	 *             If an error occurrs with an input or output operation.
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String PID = null;
		String bDefPID = null;
		String methodName = null;
		String dsID = null;
		Date asOfDateTime = null;
		Date versDateTime = null;
		String action = null;
		Property[] userParms = null;
		boolean isGetObjectProfileRequest = false;
		boolean isGetDisseminationRequest = false;
		boolean isGetDatastreamDisseminationRequest = false;
		boolean xml = false;

		requestURI = request.getRequestURL().toString() + "?"
				+ request.getQueryString();
        LOG.info("Got request: " + requestURI);
		fedoraServerProtocol = requestURI.substring(0, requestURI.indexOf(":"));

		// Parse servlet URL.
		// For the Fedora API-A-LITE "get" syntax, valid entries include:
		//
		// For dissemination requests:
		// http://host:port/fedora/get/pid/bDefPid/methodName
		// http://host:port/fedora/get/pid/bDefPid/methodName/timestamp
		// http://host:port/fedora/get/pid/bDefPid/methodName?parm=value[&parm=value]
		// http://host:port/fedora/get/pid/bDefPid/methodName/timestamp?parm=value[&parm=value]
		//
		// For object profile requests:
		// http://host:port/fedora/get/pid
		// http://host:port/fedora/get/pid/timestamp
		//
		// For datastream dissemination requests:
		// http://host:port/fedora/get/pid/dsID
		// http://host:port/fedora/get/pid/dsID/timestamp
		//
		String[] URIArray = request.getRequestURL().toString().split("/");
		if (URIArray.length == 6 || URIArray.length == 7) {
			// Request is either an ObjectProfile request or a datastream
			// request
			if (URIArray.length == 7) {
				// They either specified a date/time or a datastream id.
				if (URIArray[6].indexOf(":") == -1) {
					// If it doesn't contain a colon, they were after a
					// datastream,
					// so this is a DatastreamDissemination request
					dsID = URLDecoder.decode(URIArray[6], "UTF-8");
					isGetDatastreamDisseminationRequest = true;
				} else {
					// If it DOES contain a colon, they were after a
					// date/time-stamped object profile
					versDateTime = DateUtility.convertStringToDate(URIArray[6]);
					if (versDateTime == null) {
						String message = "ObjectProfile Request Syntax Error: DateTime value "
								+ "of \""
								+ URIArray[6]
								+ "\" is not a valid DateTime format. "
								+ " <br></br> The expected format for DateTime is \""
								+ "YYYY-MM-DDTHH:MM:SS.SSSZ\".  "
								+ " <br></br> The expected syntax for "
								+ "ObjectProfile requests is: \""
								+ URIArray[0]
								+ "//"
								+ URIArray[2]
								+ "/"
								+ URIArray[3]
								+ "/"
								+ URIArray[4]
								+ "/PID[/dateTime] \"  ."
								+ " <br></br> Submitted request was: \""
								+ requestURI + "\"  .  ";
						LOG.warn(message);
						throw new ServletException("from FedoraAccessServlet"
								+ message);
						/*
						 * commented out for exception.jsp test
						 * response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						 * response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
						 * message); return; commented out for exception.jsp
						 * test
						 */
					}
					asOfDateTime = versDateTime;
					isGetObjectProfileRequest = true;
				}
			} else {
				// URIArray.length==6 so this is a GetObjectProfile request
				isGetObjectProfileRequest = true;
			}
		} else if (URIArray.length > 7) {
			// Request is either dissemination request or timestamped get
			// datastream request
			methodName = URLDecoder.decode(URIArray[7], "UTF-8");
			if (URIArray.length == 8) {
				if (URIArray[6].indexOf(":") == -1) {
					// If it doesn't contain a colon, they were after a
					// timestamped
					// datastream, so this is a GetDatastreamDissemination
					// request.
					dsID = URLDecoder.decode(URIArray[6], "UTF-8");
					versDateTime = DateUtility.convertStringToDate(URIArray[7]);
					if (versDateTime == null) {
						String message = "GetDatastreamDissemination Request Syntax Error: DateTime value "
								+ "of \""
								+ URIArray[7]
								+ "\" is not a valid DateTime format. "
								+ " <br></br> The expected format for DateTime is \""
								+ "YYYY-MM-DDTHH:MM:SS.SSSZ\".  "
								+ " <br></br> The expected syntax for GetDatastreamDissemination requests is: \""
								+ URIArray[0]
								+ "//"
								+ URIArray[2]
								+ "/"
								+ URIArray[3]
								+ "/"
								+ URIArray[4]
								+ "/PID/dsID[/dateTime] \"  "
								+ " <br></br> Submitted request was: \""
								+ requestURI + "\"  .  ";
						LOG.warn(message);
						throw new ServletException("from FedoraAccessServlet"
								+ message);
						/*
						 * commented out for exception.jsp test
						 * response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						 * response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
						 * message); return; commented out for exception.jsp
						 * test
						 */
					}
					asOfDateTime = versDateTime;
					isGetDatastreamDisseminationRequest = true;
				} else {
					isGetDisseminationRequest = true;
				}
			} else if (URIArray.length == 9) {
				versDateTime = DateUtility.convertStringToDate(URIArray[8]);
				if (versDateTime == null) {
					String message = "Dissemination Request Syntax Error: DateTime value "
							+ "of \""
							+ URIArray[8]
							+ "\" is not a valid DateTime format. "
							+ " <br></br> The expected format for DateTime is \""
							+ "YYYY-MM-DDTHH:MM:SS.SSS\".  "
							+ " <br></br> The expected syntax for Dissemination requests is: \""
							+ URIArray[0]
							+ "//"
							+ URIArray[2]
							+ "/"
							+ URIArray[3]
							+ "/"
							+ URIArray[4]
							+ "/PID/bDefPID/methodName[/dateTime][?ParmArray] \"  "
							+ " <br></br> Submitted request was: \""
							+ requestURI + "\"  .  ";
					LOG.warn(message);
					throw new ServletException("from FedoraAccessServlet"
							+ message);
					/*
					 * commented out for exception.jsp test
					 * response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					 * response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					 * message); return; commented out for exception.jsp test
					 */
				}
				asOfDateTime = versDateTime;
				isGetDisseminationRequest = true;
			}
			if (URIArray.length > 9) {
				String message = "Dissemination Request Syntax Error: The expected "
						+ "syntax for Dissemination requests is: \""
						+ URIArray[0]
						+ "//"
						+ URIArray[2]
						+ "/"
						+ URIArray[3]
						+ "/"
						+ URIArray[4]
						+ "/PID/bDefPID/methodName[/dateTime][?ParmArray] \"  "
						+ " <br></br> Submitted request was: \""
						+ requestURI
						+ "\"  .  ";
				LOG.warn(message);
				throw new ServletException("from FedoraAccessServlet" + message);
				/*
				 * commented out for exception.jsp test
				 * response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				 * response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				 * message); return; commented out for exception.jsp test
				 */
			}
		} else {
			// Bad syntax; redirect to syntax documentation page.
			response
					.sendRedirect("/userdocs/client/browser/apialite/index.html");
			return;
		}

		// Separate out servlet parameters from method parameters
		Hashtable h_userParms = new Hashtable();
		for (Enumeration e = request.getParameterNames(); e.hasMoreElements();) {
			String name = URLDecoder.decode((String) e.nextElement(), "UTF-8");
			if (isGetObjectProfileRequest && name.equalsIgnoreCase("xml")) {
				xml = new Boolean(request.getParameter(name)).booleanValue();
			} else {
				String value = URLDecoder.decode(request.getParameter(name),
						"UTF-8");
				h_userParms.put(name, value);
			}
		}

		// API-A interface requires user-supplied parameters to be of type
		// Property[] so create Property[] from hashtable of user parameters.
		int userParmCounter = 0;
		userParms = new Property[h_userParms.size()];
		for (Enumeration e = h_userParms.keys(); e.hasMoreElements();) {
			Property userParm = new Property();
			userParm.name = (String) e.nextElement();
			userParm.value = (String) h_userParms.get(userParm.name);
			userParms[userParmCounter] = userParm;
			userParmCounter++;
		}

		PID = URIArray[5];
		String actionLabel = "Access";

		try {
			if (isGetObjectProfileRequest) {
                LOG.debug("Servicing getObjectProfile request "
                        + "(PID=" + PID + ", asOfDate=" + versDateTime + ")");

				Context context = ReadOnlyContext.getContext(
						Constants.HTTP_REQUEST.REST.uri, request);
				getObjectProfile(context, PID, asOfDateTime, xml, request,
						response);

                LOG.debug("Finished servicing getObjectProfile request");
			} else if (isGetDisseminationRequest) {
				bDefPID = URIArray[6];
                LOG.debug("Servicing getDissemination request (PID=" + PID
                        + ", bDefPID=" + bDefPID + ", methodName=" 
                        + methodName + ", asOfDate=" + versDateTime + ")");

				Context context = ReadOnlyContext.getContext(
						Constants.HTTP_REQUEST.REST.uri, request);
				getDissemination(context, PID, bDefPID, methodName, userParms,
						asOfDateTime, response, request);

                LOG.debug("Finished servicing getDissemination request");
			} else if (isGetDatastreamDisseminationRequest) {
                LOG.debug("Servicing getDatastreamDissemination request "
                        + "(PID=" + PID + ", dsID=" + dsID + ", asOfDate="
                        + versDateTime + ")");

				Context context = ReadOnlyContext.getContext(
						Constants.HTTP_REQUEST.REST.uri, request);
				getDatastreamDissemination(context, PID, dsID, asOfDateTime,
						response, request);

                LOG.debug("Finished servicing getDatastreamDissemination "
                        + "request");
			}
		} catch (AuthzException ae) {
            LOG.error("Authorization failed for request: " + requestURI
                    + " (actionLabel=" + actionLabel + ")", ae);
			throw RootException.getServletException(ae, request, actionLabel,
					new String[0]);
		} catch (Throwable th) {
            LOG.error("Unexpected error servicing API-A request", th);
			throw new InternalError500Exception("", th, request, actionLabel,
					"", new String[0]);
		}
	}

	public void getObjectProfile(Context context, String PID,
			Date asOfDateTime, boolean xml, HttpServletRequest request,
			HttpServletResponse response) throws ServerException {

		OutputStreamWriter out = null;
		Date versDateTime = asOfDateTime;
		ObjectProfile objProfile = null;
		PipedWriter pw = null;
		PipedReader pr = null;

		try {
			pw = new PipedWriter();
			pr = new PipedReader(pw);
			objProfile = s_access.getObjectProfile(context, PID, asOfDateTime);
			if (objProfile != null) {
				// Object Profile found.
				// Serialize the ObjectProfile object into XML
				new ProfileSerializerThread(context, PID, objProfile,
						versDateTime, pw).start();
				if (xml) {
					// Return results as raw XML
					response.setContentType(CONTENT_TYPE_XML);

					// Insures stream read from PipedReader correctly translates
					// utf-8
					// encoded characters to OutputStreamWriter.
					out = new OutputStreamWriter(response.getOutputStream(),
							"UTF-8");
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
					out = new OutputStreamWriter(response.getOutputStream(),
							"UTF-8");
					File xslFile = new File(s_server.getHomeDir(),
							"access/viewObjectProfile.xslt");
					TransformerFactory factory = TransformerFactory
							.newInstance();
					Templates template = factory.newTemplates(new StreamSource(
							xslFile));
					Transformer transformer = template.newTransformer();
					Properties details = template.getOutputProperties();
					transformer.transform(new StreamSource(pr),
							new StreamResult(out));
				}
				out.flush();

			} else {
				// Object Profile Definition request returned nothing.
				String message = "No object profile returned";
                LOG.error(message);
				showURLParms(PID, "", "", asOfDateTime, new Property[0],
						response, message);
			}
		} catch (AuthzException ae) {
			throw ae;
		} catch (Throwable th) {
            String message = "Error getting object profile";
            LOG.error(message, th);
			throw new GeneralException(message, th);
		} finally {
			try {
				if (pr != null)
					pr.close();
				if (out != null)
					out.close();
			} catch (Throwable th) {
                String message = "Error closing output";
                LOG.error(message, th);
				throw new StreamIOException(message);
			}
		}
	}

	public void getDatastreamDissemination(Context context, String PID,
			String dsID, Date asOfDateTime, HttpServletResponse response,
			HttpServletRequest request) throws IOException, ServerException {
		ServletOutputStream out = null;
		MIMETypedStream dissemination = null;
		dissemination = s_access.getDatastreamDissemination(context, PID, dsID,
				asOfDateTime);
		if (dissemination != null) {

			// testing to see what's in request header that might be of interest
			if (LOG.isDebugEnabled()) {
				for (Enumeration e = request.getHeaderNames(); e
						.hasMoreElements();) {
					String name = (String) e.nextElement();
					Enumeration headerValues = request.getHeaders(name);
					StringBuffer sb = new StringBuffer();
					while (headerValues.hasMoreElements()) {
						sb.append((String) headerValues.nextElement());
					}
					String value = sb.toString();
					LOG.debug("FEDORASERVLET REQUEST HEADER CONTAINED: "
									+ name + " : " + value);
				}
			}

			// Dissemination was successful;
			// Return MIMETypedStream back to browser client
			if (dissemination.MIMEType
					.equalsIgnoreCase("application/fedora-redirect")) {
				// A MIME type of application/fedora-redirect signals that the
				// MIMETypedStream returned from the dissemination is a special
				// Fedora-specific MIME type. In this case, the Fedora server
				// will
				// not proxy the datastream, but instead perform a simple
				// redirect to
				// the URL contained within the body of the MIMETypedStream.
				// This
				// special MIME type is used primarily for streaming media where
				// it
				// is more efficient to stream the data directly between the
				// streaming
				// server and the browser client rather than proxy it through
				// the
				// Fedora server.

				BufferedReader br = new BufferedReader(new InputStreamReader(
						dissemination.getStream()));
				StringBuffer sb = new StringBuffer();
				String line = null;
				while ((line = br.readLine()) != null) {
					sb.append(line);
				}

				response.sendRedirect(sb.toString());
			} else {

				response.setContentType(dissemination.MIMEType);
				Property[] headerArray = dissemination.header;
				if (headerArray != null) {
					for (int i = 0; i < headerArray.length; i++) {
						if (headerArray[i].name != null
								&& !(headerArray[i].name
										.equalsIgnoreCase("transfer-encoding"))
								&& !(headerArray[i].name
										.equalsIgnoreCase("content-type"))) {
							response.addHeader(headerArray[i].name,
									headerArray[i].value);
							LOG.debug("THIS WAS ADDED TO FEDORASERVLET RESPONSE HEADER FROM ORIGINATING PROVIDER "
										+ headerArray[i].name
										+ " : "
										+ headerArray[i].value);
						}
					}
				}
				out = response.getOutputStream();
				int byteStream = 0;
                LOG.debug("Started reading dissemination stream");
				InputStream dissemResult = dissemination.getStream();
				byte[] buffer = new byte[255];
				while ((byteStream = dissemResult.read(buffer)) != -1) {
					out.write(buffer, 0, byteStream);
				}
				buffer = null;
				dissemResult.close();
				dissemResult = null;
				out.flush();
				out.close();
                LOG.debug("Finished reading dissemination stream");
			}

		} else {
			// Dissemination request failed; echo back request parameter.
            LOG.error("No datastream dissemination result was returned");
		}
	}

	/**
	 * <p>
	 * This method calls the Fedora Access Subsystem to retrieve a MIME-typed
	 * stream corresponding to the dissemination request.
	 * </p>
	 * 
	 * @param context
	 *            The read only context of the request.
	 * @param PID
	 *            The persistent identifier of the Digital Object.
	 * @param bDefPID
	 *            The persistent identifier of the Behavior Definition object.
	 * @param methodName
	 *            The method name.
	 * @param userParms
	 *            An array of user-supplied method parameters.
	 * @param asOfDateTime
	 *            The version datetime stamp of the digital object.
	 * @param response
	 *            The servlet response.
	 * @param request
	 *            The servlet request.
	 * @throws IOException
	 *             If an error occurrs with an input or output operation.
	 * @throws ServerException
	 *             If an error occurs in the Access Subsystem.
	 */
	public void getDissemination(Context context, String PID, String bDefPID,
			String methodName, Property[] userParms, Date asOfDateTime,
			HttpServletResponse response, HttpServletRequest request)
			throws IOException, ServerException {
		ServletOutputStream out = null;
		MIMETypedStream dissemination = null;
		dissemination = s_access.getDissemination(context, PID, bDefPID,
				methodName, userParms, asOfDateTime);
		out = response.getOutputStream();
		if (dissemination != null) {

			// testing to see what's in request header that might be of interest
			if (LOG.isDebugEnabled()) {
				for (Enumeration e = request.getHeaderNames(); e
						.hasMoreElements();) {
					String name = (String) e.nextElement();
					Enumeration headerValues = request.getHeaders(name);
					StringBuffer sb = new StringBuffer();
					while (headerValues.hasMoreElements()) {
						sb.append((String) headerValues.nextElement());
					}
					String value = sb.toString();
					LOG.debug("FEDORASERVLET REQUEST HEADER CONTAINED: "
									+ name + " : " + value);
				}
			}

			// Dissemination was successful;
			// Return MIMETypedStream back to browser client
			if (dissemination.MIMEType
					.equalsIgnoreCase("application/fedora-redirect")) {
				// A MIME type of application/fedora-redirect signals that the
				// MIMETypedStream returned from the dissemination is a special
				// Fedora-specific MIME type. In this case, the Fedora server
				// will
				// not proxy the datastream, but instead perform a simple
				// redirect to
				// the URL contained within the body of the MIMETypedStream.
				// This
				// special MIME type is used primarily for streaming media where
				// it
				// is more efficient to stream the data directly between the
				// streaming
				// server and the browser client rather than proxy it through
				// the
				// Fedora server.

				BufferedReader br = new BufferedReader(new InputStreamReader(
						dissemination.getStream()));
				StringBuffer sb = new StringBuffer();
				String line = null;
				while ((line = br.readLine()) != null) {
					sb.append(line);
				}

				response.sendRedirect(sb.toString());
			} else {

				response.setContentType(dissemination.MIMEType);
				Property[] headerArray = dissemination.header;
				if (headerArray != null) {
					for (int i = 0; i < headerArray.length; i++) {
						if (headerArray[i].name != null
								&& !(headerArray[i].name
										.equalsIgnoreCase("transfer-encoding"))
								&& !(headerArray[i].name
										.equalsIgnoreCase("content-type"))) {
							response.addHeader(headerArray[i].name,
									headerArray[i].value);
    						LOG.debug("THIS WAS ADDED TO FEDORASERVLET RESPONSE HEADER FROM ORIGINATING PROVIDER "
										+ headerArray[i].name
										+ " : "
										+ headerArray[i].value);
						}
					}
				}
				int byteStream = 0;
                LOG.debug("Started reading dissemination stream");
				InputStream dissemResult = dissemination.getStream();
				byte[] buffer = new byte[255];
				while ((byteStream = dissemResult.read(buffer)) != -1) {
					out.write(buffer, 0, byteStream);
				}
				buffer = null;
				dissemResult.close();
				dissemResult = null;
				out.flush();
				out.close();
                LOG.debug("Finished reading dissemination stream");
			}
		} else {
			// Dissemination request failed; echo back request parameter.
            String message = "No dissemination result was returned";
            LOG.error(message);
			showURLParms(PID, bDefPID, methodName, asOfDateTime, userParms,
					response, message);
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

		private String fedoraServerProtocol = null;

		private String fedoraServerPort = null;

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
		public ProfileSerializerThread(Context context, String PID,
				ObjectProfile objProfile, Date versDateTime, PipedWriter pw) {
			this.pw = pw;
			this.PID = PID;
			this.objProfile = objProfile;
			this.versDateTime = versDateTime;
			fedoraServerPort = context
					.getEnvironmentValue(Constants.HTTP_REQUEST.SERVER_PORT.uri);
			if (Constants.HTTP_REQUEST.SECURE.uri.equals(context
					.getEnvironmentValue(Constants.HTTP_REQUEST.SECURITY.uri))) {
				fedoraServerProtocol = HTTPS;
			} else if (Constants.HTTP_REQUEST.INSECURE.uri.equals(context
					.getEnvironmentValue(Constants.HTTP_REQUEST.SECURITY.uri))) {
				fedoraServerProtocol = HTTP;
			}
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
										+ " xsi:schemaLocation=\"http://www.fedora.info/definitions/1/0/access/ "
										+ StreamUtility
												.enc(fedoraServerProtocol)
										+ "://"
										+ StreamUtility.enc(fedoraServerHost)
										+ ":"
										+ StreamUtility.enc(fedoraServerPort)
										+ "/objectProfile.xsd\"" + " pid=\""
										+ StreamUtility.enc(PID) + "\" >");
					} else {
						pw
								.write("<objectProfile "
										+ " xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\""
										+ " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
										+ " xsi:schemaLocation=\"http://www.fedora.info/definitions/1/0/access/ "
										+ StreamUtility
												.enc(fedoraServerProtocol)
										+ "://"
										+ StreamUtility.enc(fedoraServerHost)
										+ ":"
										+ StreamUtility.enc(fedoraServerPort)
										+ "/objectProfile.xsd\""
										+ " pid=\""
										+ StreamUtility.enc(PID)
										+ "\""
										+ " dateTime=\""
										+ DateUtility
												.convertDateToString(versDateTime)
										+ "\" >");
					}

					// PROFILE FIELDS SERIALIZATION
					pw.write("<objLabel>"
							+ StreamUtility.enc(objProfile.objectLabel)
							+ "</objLabel>");
					pw.write("<objContentModel>"
							+ StreamUtility.enc(objProfile.objectContentModel)
							+ "</objContentModel>");
					String cDate = DateUtility
							.convertDateToString(objProfile.objectCreateDate);
					pw.write("<objCreateDate>" + cDate + "</objCreateDate>");
					String mDate = DateUtility
							.convertDateToString(objProfile.objectLastModDate);
					pw.write("<objLastModDate>" + mDate + "</objLastModDate>");
					String objType = objProfile.objectType;
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
							+ StreamUtility.enc(objProfile.dissIndexViewURL)
							+ "</objDissIndexViewURL>");
					pw.write("<objItemIndexViewURL>"
							+ StreamUtility.enc(objProfile.itemIndexViewURL)
							+ "</objItemIndexViewURL>");
					pw.write("</objectProfile>");
					pw.flush();
					pw.close();
				} catch (IOException ioe) {
                    LOG.error("WriteThread IOException", ioe);
				} finally {
					try {
						if (pw != null)
							pw.close();
					} catch (IOException ioe) {
                        LOG.error("WriteThread IOException", ioe);
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
	 */
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
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
			s_server = Server.getInstance(new File(Constants.FEDORA_HOME),
                    false);
			fedoraServerHost = s_server.getParameter("fedoraServerHost");
			m_manager = (DOManager) s_server
					.getModule("fedora.server.storage.DOManager");
			s_access = (Access) s_server
					.getModule("fedora.server.access.Access");
		} catch (InitializationException ie) {
			throw new ServletException("Unable to get Fedora Server instance."
					+ ie.getMessage());
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
	 * Displays a list of the servlet input parameters. This method is generally
	 * called when a service request returns no data. Usually this is a result
	 * of an incorrect spelling of either a required URL parameter or in one of
	 * the user-supplied parameters. The output from this method can be used to
	 * help verify the URL parameters sent to the servlet and hopefully fix the
	 * problem.
	 * </p>
	 * 
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
	private void showURLParms(String PID, String bDefPID, String methodName,
			Date asOfDateTime, Property[] userParms,
			HttpServletResponse response, String message) throws IOException {
		String versDate = DateUtility.convertDateToString(asOfDateTime);
		response.setContentType(CONTENT_TYPE_HTML);
		ServletOutputStream out = response.getOutputStream();

		// Display servlet input parameters
		StringBuffer html = new StringBuffer();
		html.append("<html>");
		html.append("<head>");
		html.append("<title>FedoraAccessServlet</title>");
		html.append("</head>");
		html.append("<body>");
		html.append("<br></br><font size='+2'>" + message + "</font>");
		html.append("<br></br><font color='red'>Request Parameters</font>");
		html.append("<br></br>");
		html.append("<table cellpadding='5'>");
		html.append("<tr>");
		html.append("<td><font color='red'>PID</font></td>");
		html.append("<td> = </td>");
		html.append("<td>" + PID + "</td>");
		html.append("</tr>");
		html.append("<tr>");
		html.append("<td><font color='red'>bDefPID</font></td>");
		html.append("<td> = </td>");
		html.append("<td>" + bDefPID + "</td>");
		html.append("</tr>");
		html.append("<tr>");
		html.append("<td><font color='red'>methodName</font></td>");
		html.append("<td> = </td>");
		html.append("<td>" + methodName + "</td>");
		html.append("</tr>");
		html.append("<tr>");
		html.append("<td><font color='red'>asOfDateTime</font></td>");
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

		// List user-supplied parameters if any
		if (userParms != null) {
			for (int i = 0; i < userParms.length; i++) {
				html.append("<tr>");
				html.append("<td><font color='red'>" + userParms[i].name
						+ "</font></td>");
				html.append("<td> = </td>");
				html.append("<td>" + userParms[i].value + "</td>");
				html.append("</tr>");
			}
		}
		html.append("</table>");
		html.append("</body></html>");
		out.println(html.toString());

        LOG.debug("PID=" + PID + ", bDefPID=" + bDefPID + ", methodName="
                + methodName);
		if (userParms != null) {
			for (int i = 0; i < userParms.length; i++) {
                LOG.debug("userParm=" + userParms[i].name + ", userValue="
                        + userParms[i].value);
			}
		}
		html = null;
	}

}
