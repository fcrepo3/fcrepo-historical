package fedora.server.access;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletException;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
//import java.util.Vector;

import fedora.server.access.localservices.HttpService;
import fedora.server.errors.GeneralException;
import fedora.server.errors.HttpServiceNotFoundException;
import fedora.server.errors.InitializationException;
import fedora.server.errors.ObjectNotFoundException;
import fedora.server.errors.MethodNotFoundException;
import fedora.server.errors.ServerException;
//import fedora.server.errors.MethodParmNotFoundException;
import fedora.server.Context;
import fedora.server.ReadOnlyContext;
import fedora.server.Server;
//import fedora.server.storage.DefinitiveBMechReader;
import fedora.server.storage.DefinitiveDOReader;
import fedora.server.storage.DOManager;
//import fedora.server.storage.FastDOReader;
import fedora.server.storage.FastDOReader;
import fedora.server.storage.types.DisseminationBindingInfo;
import fedora.server.storage.types.MIMETypedStream;
import fedora.server.storage.types.Property;
import fedora.server.storage.types.MethodParmDef;
import fedora.server.storage.types.MethodDef;
import fedora.server.storage.types.ObjectMethodsDef;
import fedora.server.utilities.DateUtility;

/**
 * <p>Title: FedoraAccessServlet.java</p>
 * <p>Description: Implements Fedora Access interface via a java Servlet
 * front end. Input parameters for the servlet include:
 * <ul>
 * <li>action_ name of Fedora service which must be one of the following:
 * <ol>
 * <li>GetBehaviorDefinitions - Gets list of Behavior Defintions</li>
 * <li>GetBehaviorMethods - Gets list of Behavior Methods</li>
 * <li>GetBehaviorMethodsAsWSDL - Gets Behavior Method Definitions as XML</li>
 * <li>GetDissemination - Gets a dissemination result</li>
 * </ol>
 * <li>PID_ - persistent identifier of the digital object</li>
 * <li>bDefPID_ - persistent identifier of the Behavior Definiiton object</li>
 * <li>methodName_ - name of the method</li>
 * <li>asOfDate_ - versioning datetime stamp</li>
 * <li>clearCache_ - signal to flush the dissemination cache; value of "yes"
 * will clear the cache.
 * <li>methodParms - some methods require or provide optional parameters that
 * may be provided by the user; these parameters are entered as name/value
 * pairs like the other serlvet parameters. (optional)
 * </ul>
 * <i><b>Note that all servlet parameter names that are implementation specific
 * end with the underscore character ("_"). This is done to avoid possible
 * name clashes with user-supplied method parameter names. As a general rule,
 * user-supplied parameters should never contain names that end with the
 * underscore character to prevent possible name conflicts.</b></i>
 * <p>If a dissemination request is successful, it is placed into the
 * dissemination cache which has a default size of 100. This default can be
 * changed by setting the <code>disseminationCacheSize</code> parameter in
 * the <code>fedora.fcfg</code> configuration file. If this parameter is not
 * present or cannot be parsed, the cache size will default to 100.</p>
 * </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author Ross Wayland
 * @version 1.0
 */
public class FedoraAccessServlet extends HttpServlet implements FedoraAccess
{

  private static boolean debug = false;
  private static final String CONTENT_TYPE_HTML = "text/html";
  private static final String CONTENT_TYPE_XML  = "text/xml";
  private static final String GET_BEHAVIOR_DEFINITIONS =
      "GetBehaviorDefinitions";
  private static final String GET_BEHAVIOR_METHODS =
      "GetBehaviorMethods";
  private static final String GET_BEHAVIOR_METHODS_AS_WSDL =
      "GetBehaviorMethodsAsWSDL";
  private static final String GET_DISSEMINATION =
      "GetDissemination";
  private static final String GET_OBJECT_METHODS =
      "GetObjectMethods";
  private static final String LOCAL_ADDRESS_LOCATION = "LOCAL";
  private static Server s_server = null;
  private static final String YES = "yes";
  private static int DISS_CACHE_SIZE = 100;
  private static DOManager m_manager = null;
  private static Context s_context = null;

  private Hashtable disseminationCache = new Hashtable();
  private HttpSession session = null;
  private Hashtable h_userParms = new Hashtable();
  private String requestURL = null;
  private String requestURI = null;

  static
  {
    try
    {
      //FIXME!! - need to think about most appropriate place for dissemination
      // cache size parameter in config file; for now, put at top level.
      s_server=Server.getInstance(new File(System.getProperty("fedora.home")));
      Integer I1 = new Integer(s_server.getParameter("disseminationCacheSize"));
      DISS_CACHE_SIZE = I1.intValue();
      s_server.logInfo("Dissemination cache size: " + DISS_CACHE_SIZE);
      Boolean B1 = new Boolean(s_server.getParameter("debug"));
      debug = B1.booleanValue();
      m_manager=(DOManager) s_server.getModule(
              "fedora.server.storage.DOManager");
      HashMap h = new HashMap();
      h.put("application", "apia");
      h.put("useCachedObject", "false");
      h.put("userId", "fedoraAdmin");
      s_context = new ReadOnlyContext(h);
    } catch (InitializationException ie)
    {
      System.err.println(ie.getMessage());
    } catch (NumberFormatException nfe)
    {
      System.err.println("disseminationCacheSize parameter not found. Cache" +
                         "size set to 100." + nfe.getMessage());
    }
  }

  /**
   * <p>Process Fedora Access Request. Parse and validate the servlet input
   * parameters and then execute the specified request.</p>
   *
   * @param request  The servlet request.
   * @param response servlet The servlet response.
   * @throws ServletException If an error occurs that effects the servlet's
   * basic operation.
   * @throws IOException If an error occurrs with an input or output operation.
   */
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException
  {
    Calendar asOfDate = null;
    Date versDateTime = null;
    String action = null;
    String bDefPID = null;
    String clearCache = null;
    String methodName = null;
    String PID = null;
    Property[] userParms = null;

    // FIXME!! getRequestURL() not available in all releases of servlet API
    //requestURL = request.getRequestURL().toString()+"?";
    requestURL = "http://"+request.getServerName()+":"+request.getServerPort()+
                 request.getRequestURI()+"?";
    requestURI = requestURL+request.getQueryString();
    session = request.getSession(true);
    PrintWriter out = response.getWriter();
    if (debug) System.out.println("RequestURL: "+requestURL+
                                  "RequestURI: "+requestURI+
                                  "Session: "+session);

    // Get servlet input parameters
    Enumeration URLParms = request.getParameterNames();
    while ( URLParms.hasMoreElements())
    {
      String parm = (String) URLParms.nextElement();
      if (parm.equals("action_"))
      {
        action = request.getParameter(parm);
      } else if (parm.equals("PID_"))
      {
        PID = request.getParameter(parm);
      } else if (parm.equals("bDefPID_"))
      {
        bDefPID = request.getParameter(parm);
      } else if (parm.equals("methodName_"))
      {
        methodName = request.getParameter(parm);
      } else if (parm.equals("asOfDate_"))
      {
        asOfDate = DateUtility.
                   convertStringToCalendar(request.getParameter(parm));
      } else if (parm.equals("clearCache_"))
      {
        clearCache = request.getParameter(parm);
      } else
      {
        // Any remaining parameters are assumed to be user-supplied method
        // parameters. Place user-supplied parameters in hashtable for easy
        // access.
        h_userParms.put(parm, request.getParameter(parm));
      }
    }

    // API-A interface requires user-supplied paramters to be of type
    // Property[] so create Property[] from hashtable of user parameters.
    int userParmCounter = 0;
    userParms = new Property[h_userParms.size()];
    for ( Enumeration e = h_userParms.keys(); e.hasMoreElements();)
    {
      Property userParm = new Property();
      userParm.name = (String)e.nextElement();
      userParm.value = (String)h_userParms.get(userParm.name);
      userParms[userParmCounter] = userParm;
      userParmCounter++;
    }

    // Validate servlet URL parameters to verify that all parameters required
    // by the servlet implementation of API-A are present and to verify
    // that any other user-supplied method parameters are valid for the request.
    if (isValidURLParms(action, PID, bDefPID, methodName, versDateTime,
                        h_userParms, clearCache, response))
    {
      // FIXME!! May need to deal with session management in the future

      // Have valid request.
      if (action.equals(GET_DISSEMINATION))
      {
        try
        {
          // See if dissemination request is in local cache
          MIMETypedStream dissemination = null;
          dissemination = getDisseminationFromCache(action, PID, bDefPID,
              methodName, userParms, asOfDate, clearCache, response);
          if (dissemination != null)
          {
            // Dissemination was successful;
            // Return MIMETypedStream back to browser client
            response.setContentType(dissemination.MIMEType);
            int byteStream = 0;
            ByteArrayInputStream dissemResult =
                new ByteArrayInputStream(dissemination.stream);
            while ((byteStream = dissemResult.read()) >= 0)
            {
              out.write(byteStream);
            }
        } else
        {
          // Dissemination request failed; echo back request parameters
          // FIXME!! need to decide on exception handling
          showURLParms(action, PID, bDefPID, methodName, asOfDate, userParms,
                      clearCache, response);
          System.out.println("Dissemination Result: NULL");
        }
        // FIXME!! Decide on exception handling
        } catch (Exception e)
        {
          System.err.println(e.getMessage());
        }
      } else if (action.equals(GET_BEHAVIOR_DEFINITIONS))
      {
        try
        {
          String[] bDefs = null;
          bDefs = GetBehaviorDefinitions(PID, asOfDate);
          if (bDefs != null)
          {
            response.setContentType(CONTENT_TYPE_HTML);

            // Return HTML table containing results; include links to digital
            // object PID to further explore object.
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Behavior Definitions</title>");
            out.println("</head>");
            out.println("<br></br>");
            out.println("<center>");
            out.println("<table border='1' cellpadding='5'>");
            out.println("<tr>");
            out.println("<td><b><font size='+2'><b>PID</font></td></b>");
            out.println("<td><b><font size='+2'>Version Date</font></b></td>");
            out.println("<td><b><font size='+2'>Behavior Definitions</font>"+
                        "</b></td");
            out.println("</tr>");

            // Format table such that repeating fields display only once
            int rows = bDefs.length - 1;
            for (int i=0; i<bDefs.length; i++)
            {
              out.println("<tr>");
              if (i == 0)
              {
                out.println("<td><font color='blue'><a href='" + requestURL +
                            "action_=GetObjectMethods&PID_=" + PID+ "'>" + PID +
                            "</a></font></td>");
                out.println("<td><font color='green'>" +
                            DateUtility.convertDateToString(versDateTime) +
                            "</font></td>");
                out.println("<td><font color='red'>" + bDefs[i] +
                            "</font></td>");
              } else if (i == 1)
              {
                out.println("<td colspan='2' rowspan='" + rows +
                            "'></td><td><font color='red'>" + bDefs[i] +
                            "</font></td>");
              } else
              {
                out.println("<td><font color='red'>" + bDefs[i] +
                            "</font></td>");
              }
              out.println("</tr>");
            }
            out.println("</table>");
            out.println("</center>");
            out.println("<br></br>");
            out.println("</body>");
            out.println("</html>");
          } else
          {
            // No method BehaviorDefs found; echo back request parameters
            showURLParms(action, PID, bDefPID, methodName, asOfDate, userParms,
                         clearCache, response);
            System.out.println("GetBehaviorDefinitions: NO Definitions Found");
          }

        // FIXME!! Decide on Exception handling
        } catch (Exception e)
        {
          System.out.println(e.getMessage());
          this.getServletContext().log(e.getMessage(), e.getCause());
        }
      } else if (action.equalsIgnoreCase(GET_BEHAVIOR_METHODS_AS_WSDL))
      {
        MIMETypedStream methodWSDL = null;
        methodWSDL = GetBehaviorMethodsAsWSDL(PID, bDefPID, asOfDate);
        if (methodWSDL != null)
        {
          // Method WSDL found; output WSDL as xml
          response.setContentType(methodWSDL.MIMEType);
          ByteArrayInputStream bais =
              new ByteArrayInputStream(methodWSDL.stream);
          // WSDL is actually just an XML fragment so add appropriate
          // XML namespace and XML declaration to make a valid XML
          // output stream
          // FIXME!! Should these be added automatically in
          // the class DefinitiveBMechReader
          out.println("<?xml version=\"1.0\"?>");
          out.println("<definitions " +
              "xmlns:xsd=\"http://www.w3.org/2000/10/XMLSchema-instance\" "+
              "xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\" "+
              "xmlns:http=\"http://schemas.xmlsoap.org/wsdl/http/\" "+
              "xmlns:mime=\"http://schemas.xmlsoap.org/wsdl/mime/\" "+
              "xmlns:soap=\"http://schemas.xmlsoap.org/wsdl/soap/\">");
          int byteStream = 0;
          while ((byteStream = bais.read()) >= 0)
          {
            out.write(byteStream);
          }
          out.println("</definitions>");
        } else
        {
          // No method WSDL found; echo back request parameters
          showURLParms(action, PID, bDefPID, methodName, asOfDate, userParms,
                       clearCache, response);
          System.out.println("GetBehaviorMethodsAsWSDL: NO METHODS FOUND");
        }
      } else if (action.equals(GET_BEHAVIOR_METHODS))
      {
        try
        {
          MethodDef[] bDefMethods = GetBehaviorMethods(PID, bDefPID, asOfDate);
          if (bDefMethods != null)
          {
            // Behavior Definitions found; output HTML table of results
            // with links to each method enabling dissemination of the
            // method and a link to the object PID enabling further
            // discovery about the object.
            response.setContentType(CONTENT_TYPE_HTML);
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Behavior Methods</title>");
            out.println("</head>");
            out.println("<br></br>");
            out.println("<center>");
            out.println("<table border='1' cellpadding='5'>");
            out.println("<tr>");
            out.println("<td><b><font size='+2'> Object PID " +
                        " </font></b></td>");
            out.println("<td><b><font size='+2'> BDEF PID" +
                        " </font></b></td>");
            out.println("<td><b><font size='+2'> Version Date" +
                        " </font></b></td>");
            out.println("<td><b><font size='+2'> Method Name" +
                        " </font></b></td>");
            out.println("</tr>");

            // Format table such that repeating fields display only once
            int rows = bDefMethods.length - 1;
            for (int i=0; i<bDefMethods.length; i++)
            {
              MethodDef results = bDefMethods[i];
              out.println("<tr>");
              if (i == 0)
              {
                out.println("<td><font color=\"blue\"> " + "<a href=\""+
                            requestURL + "action_=GetObjectMethods&PID_=" +
                            PID + "\"> " + PID + " </a></font></td>");
                out.println("<td><font color=\"green\"> " + bDefPID +
                            " </font></td>");
                out.println("<td><font color=\"green\"> " +
                            DateUtility.convertDateToString(versDateTime) +
                            "</font></td>");
                out.println("<td><font color=\"red\"> " + "<a href=\""+
                            requestURL + "action_=GetDissemination&PID_=" +
                            PID + "&bDefPID_=" + bDefPID + "&methodName_=" +
                            results.methodName + "\"> " + results.methodName +
                            " </a></td>");
              } else if (i == 1)
              {
                out.println("<td colspan='3' rowspan='"+rows+"'></td>");
                out.println("<td><font color=\"red\"> " + "<a href=\""+
                            requestURL + "action_=GetDissemination&PID_=" +
                            PID + "&bDefPID_=" + bDefPID + "&methodName_=" +
                            results.methodName + "\"> " + results.methodName +
                            " </a></td>");
              } else
              {
                out.println("<td><font color=\"red\"> " + "<a href=\""+
                            requestURL + "action_=GetDissemination&PID_=" +
                            PID + "&bDefPID_=" + bDefPID + "&methodName_=" +
                            results.methodName + "\"> " + results.methodName +
                            " </a></td>");
              }
              out.println("</tr>");
            }
            out.println("</table>");
            out.println("</center>");
            out.println("</body>");
            out.println("</html>");
          } else
          {
            // No Behavior Definitions were found; echo back request parameters
            showURLParms(action, PID, bDefPID, methodName, asOfDate, userParms,
                        clearCache, response);
          }

        // FIXME!! Need to decide on Exception handling
        } catch (Exception e)
        {
          System.out.println(e.getMessage());
        }
      } else if (action.equals(GET_OBJECT_METHODS))
      {
        ObjectMethodsDef objMethDef = null;
        ObjectMethodsDef[] objMethDefArray = null;
        try
        {
          objMethDefArray = GetObjectMethods(PID, asOfDate);
          if (objMethDefArray != null)
          {
            // Object methods found; output HTML table containing all object
            // methods with links on each method enabling dissemination of
            // that particular method.
            response.setContentType(CONTENT_TYPE_HTML);
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Object Methods</title>");
            out.println("</head>");
            out.println("<br></br>");
            out.println("<center>");
            out.println("<table border='1' cellpadding='5' >");
            out.println("<tr>");
            out.println("<td><b><font size='+2'> Object PID "+
                                    " </font></b></td>");
            out.println("<td><b><font size='+2'> Version Date"+
                                    " </font></b></td>");
            out.println("<td><b><font size='+2'> BDEF PID"+
                                    " </font></b></td>");
            out.println("<td><b><font size='+2'> Method Name"+
                                    " </font></b></td>");
            out.println("</tr>");

            // Format table such that repeating fields only display once
            int rows = objMethDefArray.length-1;
            for (int i=0; i<objMethDefArray.length; i++)
            {
              out.println("<tr>");
              if (i == 0)
              {
                out.println("<td><font color=\"blue\"> " +
                            objMethDefArray[i].PID + "</font></td>");
                out.flush();
                out.println("<td><font color=\"green\"> " +
                            versDateTime + " </font></td>");
                out.println("<td><font color=\"green\"> " +
                            objMethDefArray[i].bDefPID + " </font></td>");
                out.println("<td><font color=\"red\"> " +
                            "<a href=\""+requestURL+
                            "action_=GetDissemination&PID_=" +
                            objMethDefArray[i].PID + "&bDefPID_=" +
                            objMethDefArray[i].bDefPID + "&methodName_=" +
                            objMethDefArray[i].methodName + "\"> " +
                            objMethDefArray[i].methodName + " </a></td>");
              } else if (i == 1)
              {
                out.println("<td colspan='2' rowspan='" + rows + "'></td>");
                out.println("<td><font color=\"green\"> " +
                            objMethDefArray[i].bDefPID + " </font></td>");
                out.println("<td><font color=\"red\"> " +
                            "<a href=\""+requestURL+
                            "action_=GetDissemination&PID_=" +
                            objMethDefArray[i].PID + "&bDefPID_=" +
                            objMethDefArray[i].bDefPID + "&methodName_=" +
                            objMethDefArray[i].methodName + "\"> " +
                            objMethDefArray[i].methodName + " </a></td>");
              } else
              {
                out.println("<td><font color=\"green\"> " +
                            objMethDefArray[i].bDefPID + " </font></td>");
                out.println("<td><font color=\"red\"> " +
                            "<a href=\""+requestURL+
                            "action_=GetDissemination&PID_=" +
                            objMethDefArray[i].PID + "&bDefPID_=" +
                            objMethDefArray[i].bDefPID + "&methodName_=" +
                            objMethDefArray[i].methodName + "\"> " +
                            objMethDefArray[i].methodName + " </a></td>");
              }
              out.println("</tr>");
            }
            out.println("</table>");
            out.println("</center>");
            out.println("</body>");
            out.println("</html>");
          } else
          {
            // No object methods were found; echo back request parameters
            showURLParms(action, PID, bDefPID, methodName, asOfDate, userParms,
                        clearCache, response);
          }
        } catch (Exception e)
        {
          // FIXME!! Need to decide on Exception handling
          System.out.println(e.getMessage());
        }
      }
    } else
    {
      // URL parameters failed validation check.
      System.out.println("URLParametersInvalid");
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
   * <p>Implements GetBehaviorDefinitions in the FedoraAccess interface. Gets a
   * list of Behavior Definition object PIDs for the specified digital object.
   * </p>
   *
   * @param PID The persistent identifier of the digital object.
   * @param asOfDate The versioning datetime stamp.
   * @return An array of Behavior Definition PIDs.
   */
  public String[] GetBehaviorDefinitions(String PID, Calendar asOfDate)
  {
    String[] behaviorDefs = null;
    try
    {
      FastDOReader fastReader = new FastDOReader(s_context, PID);
      Date versDateTime = DateUtility.convertCalendarToDate(asOfDate);
      behaviorDefs = fastReader.GetBehaviorDefs(versDateTime);
      return behaviorDefs;
    } catch (Exception e)
    {
      // FIXME!! - need to decide on exception handling
      System.err.println("GetBehaviorDefinitions: Object Not Found");
      System.err.println("GetBehaviorDefinitions: "+e.getMessage());
      return behaviorDefs;
    }
  }

  /**
   * <p>Implements GetBehaviorMethods in the FedoraAccess interface.
   * Gets a list of Behavior Methods associated with the specified
   * Behavior Mechanism object.</p>
   *
   * @param PID The persistent identifier of Digital Object.
   * @param bDefPID The persistent identifier of Behavior Definition object.
   * @param asOfDate The versioning datetime stamp.
   * @return An array of method definitions.
   */
  public MethodDef[] GetBehaviorMethods(String PID, String bDefPID,
      Calendar asOfDate)
  {
    Date versDateTime = DateUtility.convertCalendarToDate(asOfDate);
    ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
    MIMETypedStream bDefMethods = null;
    MethodDef[] methodDefs = null;
    try
    {
      FastDOReader fastReader = new FastDOReader(s_context, PID);
      methodDefs = fastReader.GetBMechMethods(bDefPID, versDateTime);
    } catch (Exception e)
    {
      // FIXME!! - need to decide on exception handling
      System.err.println("GetBehaviorMethods: Object Not Found");
      System.err.println(e.getMessage());
      return methodDefs;
    }
    return methodDefs;
  }

  /**
   * <p>Implements GetBehaviorMethodsAsWSDL in the FedoraAccess interface.
   * Gets a bytestream containing the WSDL that defines the Behavior Methods
   * of the associated Behavior Mechanism object.
   *
   * @param PID The persistent identifier of digital object.
   * @param bDefPID The persistent identifier of Behavior Definition object.
   * @param asOfDate The versioning datetime stamp.
   * @return MIME-typed stream containing XML-encoded method definitions
   * from WSDL.
   */
  public MIMETypedStream GetBehaviorMethodsAsWSDL(String PID, String bDefPID,
      Calendar asOfDate)
  {
    Date versDateTime = DateUtility.convertCalendarToDate(asOfDate);
    ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
    MIMETypedStream methodDefs = null;
    try
    {
      DefinitiveDOReader doReader = new DefinitiveDOReader(PID);
      InputStream methodResults = doReader.GetBMechMethodsWSDL(bDefPID,
          versDateTime);
      int byteStream = 0;
      while ((byteStream = methodResults.read()) >= 0)
      {
        baos.write(byteStream);
      }
    //} catch (IOException ioe)
    //{
    //  System.err.println(ioe);
    //  System.err.println("GetBehaviorMethodsAsWSDL: Object Not Found");
    //  this.getServletContext().log(ioe.getMessage(), ioe.getCause());
    } catch (Exception e)
    {
      // FIXME!! - need to decide on exception handling
      System.err.println("GetBehaviorMethodsAsWSDL: Object Not Found");
      System.err.println("GetBehaviorMethodsAsWSDL: "+e.getMessage());
      return methodDefs;
    }
    methodDefs = new MIMETypedStream(CONTENT_TYPE_XML,baos.toByteArray());
    return methodDefs;
  }

  /**
   * <p>Implements GetDissemination in the Fedora Access interface.
   * Gets a MIME-typed bytestream containing the result of a dissemination.
   *
   * @param PID The persistent identifier of the digital object.
   * @param bDefPID The persistent identifier of the Behavior Definition object.
   * @param methodName The name of the method.
   * @param asOfDate The version datetime stamp of the digital object.
   * @param userParms An array of user-supplied method parameters and values.
   * @return A MIME-typed stream containing the dissemination result.
   */
  public MIMETypedStream GetDissemination(String PID, String bDefPID,
       String methodName, Property[] userParms, Calendar asOfDate)
      throws ServerException
   {
     String protocolType = null;
     DisseminationBindingInfo[] dissResults = null;
     DisseminationBindingInfo dissResult = null;
     String dissURL = null;
     String operationLocation = null;
     MIMETypedStream dissemination = null;
     Date versDateTime = DateUtility.convertCalendarToDate(asOfDate);
     FastDOReader fastReader = null;
     try
     {
       fastReader = new FastDOReader(s_context, PID);
       dissResults = fastReader.getDissemination(PID, bDefPID, methodName,
           versDateTime);
       DisseminationService dissService = new DisseminationService();
       dissemination = dissService.assembleDissemination(userParms, dissResults);
       /*
       String replaceString = null;
       int numElements = dissResults.length;

       // Get row(s) of WSDL results and perform string substitution
       // on DSBindingKey and method parameter values in WSDL
       // Note: In case where more than one datastream matches the
       // DSBindingKey or there are multiple DSBindingKeys for the
       // method, multiple rows will be returned; otherwise
       // a single row is returned.
       for (int i=0; i<dissResults.length; i++)
       {
         dissResult = dissResults[i];

         // If AddressLocation has a value of "LOCAL", this is a flag to
         // indicate the associated OperationLocation requires no
         // AddressLocation. i.e., the OperationLocation contains all
         // information necessary to perform the dissemination request.
         if (dissResult.AddressLocation.equalsIgnoreCase(LOCAL_ADDRESS_LOCATION))
         {
           dissResult.AddressLocation = "";
         }

         // Match DSBindingKey pattern in WSDL
         String bindingKeyPattern = "\\("+dissResult.DSBindKey+"\\)";
         if (i == 0)
         {
           operationLocation = dissResult.OperationLocation;
           dissURL = dissResult.AddressLocation+dissResult.OperationLocation;
           protocolType = dissResult.ProtocolType;
         }
         if (debug) System.out.println("counter: "+i+" numelem: "+numElements);
         String currentKey = dissResult.DSBindKey;
         String nextKey = "";
         if (i != numElements-1)
         {
           // Except for last row, get the value of the next binding key
           // to compare with the value of the current binding key.
           if (debug) System.out.println("currentKey: '"+currentKey+"'");
           nextKey = dissResults[i+1].DSBindKey;
           if (debug) System.out.println("' nextKey: '"+nextKey+"'");
         }

         // In most cases, there is only a single datastream that matches a
         // given DSBindingKey so the substitution process is to just replace
         // the occurence of (BINDING_KEY) with the value of the datastream
         // location. However, when multiple datastreams match the same
         // DSBindingKey, the occurrence of (BINDING_KEY) is replaced with the
         // value of the datastream location and the value +(BINDING_KEY) is
         // appended so that subsequent datastreams matching the binding key
         // will be substituted. The end result is that the binding key will
         // be replaced by a string datastream locations separated by a plus(+)
         // sign. e.g.,
         //
         // file=(PHOTO) becomes
         // file=dslocation1+dslocation2+dslocation3
         //
         // It is the responsibility of the Behavior Mechanism to know how to
         // handle an input parameter with multiple datastreams.
         //
         // In the case of a method containing multiple binding keys,
         // substitutions are performed on each binding key. e.g.,
         //
         // image=(PHOTO)&watermark=(WATERMARK) becomes
         // image=dslocation1&watermark=dslocation2
         //
         // In the case with mutliple binding keys and multiple datastreams,
         // the substitution might appear like the following:
         //
         // image=(PHOTO)&watermark=(WATERMARK) becomes
         // image=dslocation1+dslocation2&watermark=dslocation3
         if (nextKey.equalsIgnoreCase(currentKey) & i != numElements)
         {
           replaceString = dissResult.DSLocation+"+("+dissResult.DSBindKey+")";
         } else
         {
           replaceString = dissResult.DSLocation;
         }
         if (debug) System.out.println("replaceString: "+replaceString);
         dissURL = substituteString(dissURL, bindingKeyPattern, replaceString);
         if (debug) System.out.println("replaced dissURL = "+
                                      dissURL.toString()+
                                      " counter = "+i);
       }

       // User-supplied parameters have already been validated.
       // Substitute user-supplied parameter values in dissemination URL
       Enumeration e = h_userParms.keys();
       while (e.hasMoreElements())
       {
         String name = (String)e.nextElement();
         String value = (String)h_userParms.get(name);
         String pattern = "\\("+name+"\\)";
         dissURL = substituteString(dissURL, pattern, value);
         if (debug) System.out.println("UserParmSubstitution dissURL: "+
                                       dissURL);
       }

       // Resolve content referenced by dissemination result
       if (debug) System.out.println("ProtocolType = "+protocolType);
       if (protocolType.equalsIgnoreCase("http"))
       {
         // FIXME!! need to implement Access Policy control.
         // If access is based on restrictions to content,
         // this is the last chance to apply those restrictions
         // before returnign dissemination result to client.
         HttpService httpService = new HttpService(dissURL);
         try
         {
           dissemination = httpService.getHttpContent(dissURL);
         } catch (HttpServiceNotFoundException onfe)
         {
           // FIXME!! -- Decide on Exception handling
           System.out.println(onfe.getMessage());
         }
       } else if (protocolType.equalsIgnoreCase("soap"))
       {
         // FIXME!! future handling by soap interface
         System.out.println("Protocol type specified: "+protocolType);
         dissemination = null;
       } else
       {
         System.out.println("Unknown protocol type: "+protocolType);
         dissemination = null;
       }
       */
     } catch (ServerException se)
     {
       throw se;
     }
     return dissemination;
   }

   /**
    * <p>Implements GetObjectMethods in the Fedora Access Interface.
    * Gets a list of all method definitions for the specified object.</p>
    *
    * @param PID The persistent identifier for the digital object.
    * @param asOfDate The versioning datetime stamp.
    * @return An array of object method definitions.
    */
  public ObjectMethodsDef[] GetObjectMethods(String PID, Calendar asOfDate)
  {
    Date versDateTime = DateUtility.convertCalendarToDate(asOfDate);
    FastDOReader fastReader = null;
    ObjectMethodsDef[] objMethDefArray = null;
    try
    {
      fastReader = new FastDOReader(s_context, PID);
      objMethDefArray = fastReader.getObjectMethods(PID, versDateTime);
    } catch (Exception e)
    {
      // FIXME!! Decide on Exception handling
      System.out.println(e.getMessage());
      //this.getServletContext().log(onfe.getMessage(), onfe.getCause());
    }
    return objMethDefArray;
  }

  /**
   * <p>Initialize servlet.</p>
   *
   * @throws ServletException If the servet cannot be initialized.
   */
  public void init() throws ServletException
  {}

  /**
   * <p>Cleans up servlet resources.</p>
   */
  public void destroy()
  {}

  /**
   * <p>Instantiates a new dissemination cache.</p>
   */
  private synchronized void clearDisseminationCache() {
    disseminationCache = new Hashtable();
  }

  /**
   * <p>Gets dissemination from cache. This method attempts to retrieve
   * a dissemination from the cache. If found, the dissemination is
   * returned. If not found, this method calls <code>GetDissemination</code>
   * to get the dissemination. If the retrieval is successful, the
   * dissemination is added to the cache. The cache may be cleared by
   * setting the URL servlet parameter <code>clearCache</code> to a value
   * of "yes". The cache is also flushed when it reaches the limit
   * specified by <code>DISS_CACHE_SIZE</code>.</p>
   *
   * @param dissRequestID The originating URI request used as hash key.
   * @param PID The persistent identifier of the Digital Object.
   * @param bDefPID The persistent identifier of the Behavior Definition object.
   * @param methodName The method name.
   * @param userParms An array of user-supplied method parameters.
   * @param asOfDate The version datetime stamp of the digital object.
   * @return The MIME-typed stream containing dissemination result.
   */
  private synchronized MIMETypedStream getDisseminationFromCache(String action,
      String PID, String bDefPID, String methodName,
      Property[] userParms, Calendar asOfDate, String clearCache,
      HttpServletResponse response) throws IOException
  {
    // Clear cache if size gets larger than DISS_CACHE_SIZE
    // FIXME!! This needs to part of the Fedora server config parameters
    if (disseminationCache.size() > DISS_CACHE_SIZE ||
        (clearCache != null && clearCache.equalsIgnoreCase(YES)))
    {
      clearDisseminationCache();
    }
    MIMETypedStream disseminationResult = null;
    // See if dissemination request is in local cache
    disseminationResult =
        (MIMETypedStream)disseminationCache.get(requestURI);
    if (disseminationResult == null)
    {
      // Dissemination request NOT in local cache.
      // Try reading from relational database
      try {
      disseminationResult = GetDissemination(PID, bDefPID, methodName,
          userParms, asOfDate);
      } catch (ServerException se)
      {
        try{
        response.setStatus(404);
          response.sendError(404, "Dissemination Error: "+
                             se.getMessage());
          //response.sendError(404, this.requestURL);
          PrintWriter out = response.getWriter();
          out.println(se.getMessage());
          se.printStackTrace(out);
        } catch (IOException ioe)
        {}
      }
      if (disseminationResult != null)
      {
        // Dissemination request succeeded, so add to local cache
        disseminationCache.put(requestURI, disseminationResult);
         if (debug) System.out.println("ADDED to CACHE: "+requestURI);
      } /* else
      {
        // Dissemination request failed
        // FIXME!! need to decide on exception handling
        showURLParms(action, PID, bDefPID, methodName, asOfDate, userParms,
                    clearCache, response);
        System.out.println("Dissemination Result: NULL");
        this.getServletContext().log("Dissemination Result: NULL");
      } */
      if (debug) System.out.println("CACHE SIZE: "+disseminationCache.size());
    }
    return disseminationResult;
  }

  /**
   * <p>Validates required servlet URL parameters. Different parameters
   * are required based on the requested action.</p>
   *
   * @param action The Fedora service to be executed
   * @param PID The persistent identifier of the Digital Object.
   * @param bDefPID The persistent identifier of the Behavior Definition object.
   * @param methodName The method name.
   * @param versDate The version datetime stamp of the digital object.
   * @param userParms An array of user-supplied method parameters.
   * @param clearCache A boolean flag to clear dissemination cache.
   * @param response The servlet response.
   * @return True if required parameters are valid; false otherwise.
   * @throws IOException If an error occurrs with an input or output operation.
   */
  private boolean isValidURLParms(String action, String PID, String bDefPID,
                          String methodName, Date versDateTime,
                          Hashtable h_userParms, String clearCache,
                          HttpServletResponse response)
      throws IOException
  {
    boolean checkOK = true;
    PrintWriter out = response.getWriter();
    String versDate = DateUtility.convertDateToString(versDateTime);
    if (action != null && action.equals(GET_DISSEMINATION))
    {
      if (PID == null || bDefPID == null || methodName == null)
      {
        // Dissemination requires PID, bDefPID, and methodName
        // asOfDate is optional
        response.setContentType(CONTENT_TYPE_HTML);
        out.println("<html>");
        out.println("<head>");
        out.println("<title>FedoraServlet</title>");
        out.println("</head>");
        out.println("<body>");
        out.println("<p><font size='+1' color='red'>"+
                    "Required parameter missing "+
                    "in Dissemination Request:</font></p>");
        out.println("<table cellpadding='5'>");
        out.println("<tr>");
        out.println("<td><font color='red'>action_</font></td>");
        out.println("<td> = </td>");
        out.println("<td>"+action+"</td>");
        out.println("<td><font color='blue'>(REQUIRED)</font></td>");
        out.println("</tr>");
        out.println("<tr>");
        out.println("<td><font color='red'>PID_</font></td>");
        out.println("<td> = </td>");
        out.println("<td>"+PID+"</td>");
        out.println("<td><font color='blue'>(REQUIRED)</font></td>");
        out.println("</tr>");
        out.println("<tr>");
        out.println("<td><font color='red'>bDefPID_</font></td>");
        out.println("<td> = </td><td>"+bDefPID+"</td>");
        out.println("<td><font color='blue'>(REQUIRED)</font></td>");
        out.println("</tr>");
        out.println("</font><tr>");
        out.println("<td><font color='red'>methodName_</font></td>");
        out.println("<td> = </td>");
        out.println("<td>"+methodName+"</td>");
        out.println("<td><font color='blue'>(REQUIRED)</font></td>");
        out.println("</tr>");
        out.println("</font><tr>");
        out.println("<td><font color='red'>AsOfDate_</font></td>");
        out.println("<td> = </td>");
        out.println("<td>"+versDate+"</td>");
        out.println("<td><font color='green'>(OPTIONAL)</font></td>");
        out.println("</tr>");
        out.println("<tr>");
        out.println("<td><font color='red'>clearCache_</font></td>");
        out.println("<td> = </td>");
        out.println("<td>"+clearCache+"</td>");
        out.println("<td><font color='green'>(OPTIONAL)</font></td>");
        out.println("</tr></font>");
        out.println("<tr>");
        out.println("</tr>");
        out.println("<tr>");
        out.println("<td colspan='5'><font size='+1' color='blue'>"+
                    "Other Parameters Found:</font></td>");
        out.println("</tr>");
        out.println("<tr>");
        out.println("</tr>");
        for (Enumeration e = h_userParms.keys() ; e.hasMoreElements(); )
        {
          String name = (String)e.nextElement();
          out.println("<tr>");
          out.println("<td><font color='red'>"+name+"</font></td>");
          out.println("<td>= </td>");
          out.println("<td>"+h_userParms.get(name)+"</td>");
          out.println("</tr>");
        }
        out.println("</table>");
        out.println("</body>");
        out.println("</html>");
        checkOK = false;
      } else
      {
        // Required parameters are present;
        // Check user supplied method parms:
        //   1) For all parameters required by the method
        //   2) For any parameters with null values for which the method
        //      defines default values
        //   3) For any parameters not definined by the method
        if(!isValidUserParms(PID, bDefPID, methodName, h_userParms,
                              versDateTime, response))
        {
          checkOK = false;
        }
      }
    } else if (action != null &&
               (action.equals(GET_BEHAVIOR_DEFINITIONS) ||
               action.equals(GET_OBJECT_METHODS)))
    {
      if (PID == null)
      {
        // GetBehaviorDefinitions and GetObjectMethods require PID
        // asOfDate is optional
        response.setContentType(CONTENT_TYPE_HTML);
        out.println("<html>");
        out.println("<head>");
        out.println("<title>FedoraServlet</title>");
        out.println("</head>");
        out.println("<body>");
        out.println("<p><font size='+1' color='red'>"+
                    "Required parameter missing in Behavior "+
                    "Definition Request:</font></p>");
        out.println("<table cellpadding='5'>");
        out.println("<tr>");
        out.println("<td><font color='red'>action_</td>");
        out.println("<td> = </td>");
        out.println("<td>"+action+"</td>");
        out.println("<td><font color='blue'>(REQUIRED)</font></td>");
        out.println("</tr>");
        out.println("<tr>");
        out.println("<td><font color='red'>PID_</td>");
        out.println("<td> = </td>");
        out.println("<td>"+PID+"</td>");
        out.println("<td><font color='blue'>(REQUIRED)</font></td>");
        out.println("</tr>");
        out.println("<tr>");
        /*
        out.println("<td><font color='red'>bDefPID_</td>");
        out.println("<td> = </td>");
        out.println("<td>"+bDefPID+"</td>");
        out.println("<td><font color='green'>(OPTIONAL)</font></td>");
        out.println("</tr>");
        out.println("<tr>");
        out.println("<td><font color='red'>methodName_</td>");
        out.println("<td> = </td>");
        out.println("<td>"+methodName+"</td>");
        out.println("<td><font color='green'>(OPTIONAL)</font></td>");
        out.println("</tr>");
        */
        out.println("<tr>");
        out.println("<td><font color='red'>AsOfDate_</td>");
        out.println("<td> = </td>");
        out.println("<td>"+versDate+"</td>");
        out.println("<td><font color='green'>(OPTIONAL)</font></td>");
        out.println("</tr>");
        out.println("<tr>");
        out.println("<td><font color='red'>clearCache_</td>");
        out.println("<td> = </td>");
        out.println("<td>"+clearCache+"</td>");
        out.println("<td><font color='green'>(OPTIONAL)</font></td>");
        out.println("</tr>");
        out.println("<tr>");
        out.println("</tr>");
        out.println("<tr>");
        out.println("<td colspan='5'><font size='+1' color='blue'>"+
                    "Other Parameters Found:</font></td>");
        out.println("</tr>");
        out.println("<tr>");
        out.println("</tr>");
        for (Enumeration e = h_userParms.keys() ; e.hasMoreElements(); )
        {
          String name = (String)e.nextElement();
          out.println("<tr>");
          out.println("<td><font color='red'>"+name+"</font></td>");
          out.println("<td>= </td>");
          out.println("<td>"+h_userParms.get(name)+"</td>");
          out.println("</tr>");
        }
        out.println("</table>");
        out.println("</body>");
        out.println("</html>");
        checkOK = false;
      }
    } else if (action != null &&
               (action.equalsIgnoreCase(GET_BEHAVIOR_METHODS) ||
               action.equalsIgnoreCase(GET_BEHAVIOR_METHODS_AS_WSDL)))
    {
      if (PID == null || bDefPID == null)
      {
        // GetBehaviorMethods and GetBehaviorMethodsAsWSDL require PID, bDefPID
        // asOfDate is optional
        response.setContentType(CONTENT_TYPE_HTML);
        out.println("<html>");
        out.println("<head>");
        out.println("<title>FedoraServlet</title>");
        out.println("</head>");
        out.println("<body>");
        out.println("<p><font size='+1' color='red'>"+
                    "Required parameter missing in Behavior "+
                    "Methods Request:</font></p>");
        out.println("<table cellpadding='5'>");
        out.println("<tr>");
        out.println("<td><font color='red'>action_</td>");
        out.println("<td> = </td>");
        out.println("<td>"+action+"</td>");
        out.println("<td><font color='blue'>(REQUIRED)</font></td>");
        out.println("</tr>");
        out.println("<tr>");
        out.println("<td><font color='red'>PID_</td>");
        out.println("<td> = </td>");
        out.println("<td>"+PID+"</td>");
        out.println("<td><font color='blue'>(REQUIRED)</font></td>");
        out.println("</tr>");
        out.println("<tr>");
        out.println("<td><font color='red'>bDefPID_</td>");
        out.println("<td> = </td>");
        out.println("<td>"+bDefPID+"</td>");
        out.println("<td><font color='blue'>(REQUIRED)</font></td>");
        out.println("</tr>");
        /*
        out.println("<tr>");
        out.println("<td><font color='red'>methodName_</td>");
        out.println("<td> = </td>");
        out.println("<td>"+methodName+"</td>");
        out.println("<td><font color='green'>(OPTIONAL)</font></td>");
        out.println("</tr>");
        */
        out.println("<tr>");
        out.println("<td><font color='red'>AsOfDate_</td>");
        out.println("<td> = </td>");
        out.println("<td>"+versDate+"</td>");
        out.println("<td><font color='green'>(OPTIONAL)</font></td>");
        out.println("</tr>");
        out.println("<tr>");
        out.println("<td><font color='red'>clearCache_</td>");
        out.println("<td> = </td>");
        out.println("<td>"+clearCache+"</td>");
        out.println("<td><font color='green'>(OPTIONAL)</font></td>");
        out.println("</tr>");
        out.println("<tr>");
        out.println("</tr>");
        out.println("<tr>");
        out.println("<td colspan='5'><font size='+1' color='blue'>"+
                    "Other Parameters Found:</font></td>");
        out.println("</tr>");
        out.println("<tr>");
        out.println("</tr>");
        for (Enumeration e = h_userParms.keys() ; e.hasMoreElements(); )
        {
          String name = (String)e.nextElement();
          out.println("<tr>");
          out.println("<td><font color='red'>"+name+"</font></td>");
          out.println("<td>= </td>");
          out.println("<td>"+h_userParms.get(name)+"</td>");
          out.println("</tr>");
        }
        out.println("</table>");
        out.println("</body>");
        out.println("</html>");
        checkOK = false;
      }
    } else
    {
      // Unknown Fedora service has been requested
      response.setContentType(CONTENT_TYPE_HTML);
      out.println("<html>");
      out.println("<head>");
      out.println("<title>FedoraServlet</title>");
      out.println("</head>");
      out.println("<body>");
      out.println("<p><font size='+1' color='red'>Invalid 'action' "+
                  "parameter specified in Servlet Request: action= "+
                  action+"<p>");
      out.println("<br></br><font color='blue'>Reserved parameters "+
                  "in Request:</font>");
      out.println("<table cellpadding='5'>");
      out.println("<tr>");
      out.println("<td><font color='red'>action_</td>");
      out.println("<td> = </td>");
      out.println("<td>"+action+"</td>");
      out.println("</tr>");
      out.println("<tr>");
      out.println("<td><font color='red'>PID_</td>");
      out.println("<td> = </td>");
      out.println("<td>"+PID+"</td>");
      out.println("</tr>");
      out.println("<tr>");
      out.println("<td><font color='red'>bDefPID_</td>");
      out.println("<td> = </td>");
      out.println("<td>"+bDefPID+"</td>");
      out.println("</tr>");
      out.println("<tr>");
      out.println("<td><font color='red'>methodName_</td>");
      out.println("<td> = </td>");
      out.println("<td>"+methodName+"</td>");
      out.println("</tr>");
      out.println("<tr>");
      out.println("<td><font color='red'>AsOfDate_</td>");
      out.println("<td> = </td>");
      out.println("<td>"+versDate+"</td>");
      out.println("</tr>");
      out.println("<tr>");
      out.println("<td><font color='red'>clearCache_</td>");
      out.println("<td> = </td>");
      out.println("<td>"+clearCache+"</td>");
      out.println("</tr>");
      out.println("<tr>");
      out.println("</tr>");
      out.println("<tr>");
      out.println("<td colspan='5'><font size='+1' color='blue'>"+
                  "Other Parameters Found:</font></td>");
      out.println("</tr>");
      out.println("<tr>");
      out.println("</tr>");
      for (Enumeration e = h_userParms.keys() ; e.hasMoreElements(); )
      {
        String name = (String)e.nextElement();
        out.println("<tr>");
        out.println("<td><font color='red'>"+name+"</font></td>");
        out.println("<td>= </td>");
        out.println("<td>"+h_userParms.get(name)+"</td>");
        out.println("</tr>");
      }
      out.println("</table>");
      out.println("</body>");
      out.println("</html>");
      checkOK = false;
    }

    if (debug)
    {
      System.out.println("PID: "+PID+"bDEF: "+bDefPID+"methodName: "+methodName+
                         "action: "+action);

      for ( Enumeration e = h_userParms.keys(); e.hasMoreElements(); )
      {
        String name = (String)e.nextElement();
        System.out.println("<p>userParm: "+name+
                           " userValue: "+h_userParms.get(name));
      }
    }
    return checkOK;
  }

  /**
   * <p>Validates user-supplied method parameters against values
   * in the corresponding Behavior Definition object. The method will validate
   * for:</p>
   * <ol>
   * <li> Valid name - each name must match a valid method parameter name</li>
   * <li> DefaultValue - any specified parameters with valid default values
   * will have the default value substituted if the user-supplied value is null
   * </li>
   * <li> Required name - each required method parameter name must be present
   * </ol>
   *
   * @param PID The persistent identifier of the digital object.
   * @param bDefPID The persistent identifier of the Behavior Definition object.
   * @param methodName The name of the method.
   * @param A hashtable of user-supplied method parameter name/value.
   *                      pairs
   * @return True if method parameters are valid; false otherwise.
   *
   */
  private boolean isValidUserParms(String PID, String bDefPID,
                                    String methodName, Hashtable h_userParms,
                                    Date versDateTime,
                                    HttpServletResponse response)
      throws IOException
  {
    boolean valid = true;
    FastDOReader fdor = null;
    MethodParmDef[] methodParms = null;
    MethodParmDef methodParm = null;

    try
    {
      fdor = new FastDOReader(s_context, PID);
      methodParms = fdor.GetBMechMethodParm(bDefPID, methodName, versDateTime);

      // FIXME!! Decide on Exception handling
    //} catch(MethodNotFoundException mpnfe)
    } catch (Exception e)
    {
      //System.out.println(mpnfe.getMessage());
      System.err.println(e.getMessage());
      //this.getServletContext().log(mpnfe.getMessage(), mpnfe.getCause());
    //} catch (ObjectNotFoundException onfe)
    //{
    //  System.out.println(onfe.getMessage());
    //  this.getServletContext().log(onfe.getMessage(), onfe.getCause());
    }

    // Put valid method parameters and their attributes into hashtable
    Hashtable v_validParms = new Hashtable();
    if (methodParms != null)
    {
      for (int i=0; i<methodParms.length; i++)
      {
        methodParm = methodParms[i];
        v_validParms.put(methodParm.parmName,methodParm);
      }
    }

    // check if no user supplied parameters
    if (!h_userParms.isEmpty())
    {
      // Iterate over each user supplied parameter name
      Enumeration parmNames = h_userParms.keys();
      while (parmNames.hasMoreElements())
      {
        String name = (String)parmNames.nextElement();
        methodParm = (MethodParmDef)v_validParms.get(name);
        if (methodParm != null && methodParm.parmName != null)
        {
          // Method has at least one parameter
          if (methodParm.parmRequired)
          {
            // Method parm is required
            if (h_userParms.get(methodParm.parmName) == null)
            {
              // Error: required method parameter not in user-supplied list
              System.out.println("REQUIRED PARAMETER:" + methodParm.parmName +
                                 " NOT FOUND");
              response.setContentType(CONTENT_TYPE_HTML);
              PrintWriter out = response.getWriter();
              out.println("<br></br><b><font size=\"+1\" color=\"green\">"+
                          "*****REQUIRED PARAMETER NOT FOUND: "+
                          methodParm.parmName + "</font></b>");
              valid = false;
            } else
            {
              // Required parameter found
              if (debug) System.out.println("Required parameter FOUND: " +
                  methodParm.parmName);
            }
          }

          // Method parameter is not required
          // Check for default value if user-supplied value is null or empty
          String value = (String)h_userParms.get(methodParm.parmName);
          if (value == null && value.equalsIgnoreCase(""))
          {
            // Value of user-supplied parameter is  null or empty
            if(methodParm.parmDefaultValue != null)
            {
              // Default value is specified for this parameter.
              // Substitute default value.
              h_userParms.put(methodParm.parmName, methodParm.parmDefaultValue);
              if (debug) System.out.println("SET DEFAULT VALUE: "+
                  methodParm.parmDefaultValue);
            }
          } else
          {
            // Value of user-supplied parameter is NOT null or empty
            if (debug) System.out.println("NO DEFAULT VALUE APPLIED");
          }
          if (!h_userParms.isEmpty() &&
              (h_userParms.get(methodParm.parmName) == null) )
          {
            // User-supplied parameter name does not match any valid parameter
            // names for this method.
            System.out.println("USER SUPPLIED PARAMETER NOT VALID FOR THIS " +
                              "METHOD: "+methodParm.parmName);
            response.setContentType(CONTENT_TYPE_HTML);
            PrintWriter out = response.getWriter();
            out.println("<br><b><font size=\"+1\" color=\"green\">"+
                        "*****INVALID METHOD PARAMETER: "+methodParm.parmName+
                          "</font></b>");
            valid = false;
          }
        } else
        {
          if (debug) System.out.println("NAME NOT FOUND: "+name);
        }
    }
    } else
    {
      // There were no user supplied parameters.
      // Check if this method has any required parameters.
      if (methodParms != null)
      {
        for (int i=0; i<methodParms.length; i++)
        {
          methodParm = methodParms[i];
          if (methodParm.parmRequired)
          {
            // A required method parameter was not found
            if (debug) System.out.println("emptyREQUIRED PARAM NAME NOT FOUND: "
                + methodParm.parmName);
            response.setContentType(CONTENT_TYPE_HTML);
            PrintWriter out = response.getWriter();
            out.println("<br></br><b><font size=\"+1\" color=\"green\">"+
                        "REQUIRED METHOD PARAMETER NOT FOUND: "+
                        methodParm.parmName+"</font></b>");
            valid = false;
          } else
          {
            //if (debug) System.out.println("emptyNON-REQUIRED PARAM FOUND: " +
            //    methodParm.parmName);
          }
        }
      }
    }
    return valid;
  }

  /**
   * <p>Displays a list of the servlet input parameters. This method is
   * generally called when a service request returns no data. Usually
   * this is a result of an incorrect spelling of either a required
   * URL parameter or in one of the user-supplied parameters. The output
   * from this method can be used to help verify the URL parameters
   * sent to the servlet</p>
   *
   * @param action The Fedora service requested.
   * @param PID The persistent identifier of the digital object.
   * @param bDefPID The persistent identifier of the Behavior Definition object.
   * @param methodName the name of the method.
   * @param asOfDate The version datetime stamp of the digital object.
   * @param userParms An array of user-supplied method parameters and values.
   * @param clearCache The dissemination cache flag.
   * @param response The servlet response.
   * @throws IOException If an error occurrs with an input or output operation.
   */
  private void showURLParms(String action, String PID, String bDefPID,
                           String methodName, Calendar asOfDate,
                           Property[] userParms, String clearCache,
                           HttpServletResponse response)
      throws IOException
  {

    String versDate = DateUtility.convertCalendarToString(asOfDate);
    if (debug) System.out.println("versdate: "+versDate);
    PrintWriter out = response.getWriter();
    response.setContentType(CONTENT_TYPE_HTML);

    // Display servlet input parameters
    out.println("<html>");
    out.println("<head>");
    out.println("<title>FedoraServlet</title>");
    out.println("</head>");
    out.println("<body>");
    out.println("<br></br><font size='+3' REQUEST Returned NO Data</font>");
    out.println("<br></br><font color='red'>Request Parameters</font>");
    out.println("<br></br>");
    out.println("<table cellpadding='5'>");
    out.println("<tr>");
    out.println("<td><font color='red'>action_</td>");
    out.println("<td> = </td>");
    out.println("<td>"+action+"</td>");
    out.println("</tr>");
    out.println("<tr>");
    out.println("<td><font color='red'>PID_</td>");
    out.println("<td> = <td>"+PID+"</td>");
    out.println("</tr>");
    out.println("<tr>");
    out.println("<td><font color='red'>bDefPID_</td>");
    out.println("<td> = </td>");
    out.println("<td>"+bDefPID+"</td>");
    out.println("</tr>");
    out.println("<tr>");
    out.println("<td><font color='red'>methodName_</td>");
    out.println("<td> = </td>");
    out.println("<td>"+methodName+"</td>");
    out.println("</tr>");
    out.println("<tr>");
    out.println("<td><font color='red'>AsOfDate_</td>");
    out.println("<td> = </td>");
    out.println("<td>"+versDate+"</td>");
    out.println("</tr>");
    out.println("<tr>");
    out.println("<td><font color='red'>clearCache_</td>");
    out.println("<td> = </td>");
    out.println("<td>"+clearCache+"</td>");
    out.println("</tr>");
    out.println("<tr>");
    out.println("</tr>");
    out.println("<tr>");
    out.println("<td colspan='5'><font size='+1' color='blue'>"+
                "Other Parameters Found:</font></td>");
    out.println("</tr>");
    out.println("<tr>");
    out.println("</tr>");

    // List user-supplied parameters if any
    if (userParms != null)
    {
    for (int i=0; i<userParms.length; i++)
    {
      out.println("<tr>");
      out.println("<td><font color='red'>"+userParms[i].name+"</font></td>");
      out.println("<td> = </td>");
      out.println("<td>"+userParms[i].value+"</td>");
        out.println("</tr>");
    }
    }
    out.println("</table></center></font>");
    out.println("</body></html>");

    if (debug)
    {
      System.out.println("PID: "+PID+"bDEF: "+bDefPID+"methodName: " +
                         methodName);
      if (userParms != null)
      {
        for (int i=0; i<userParms.length; i++)
        {
          System.out.println("<p>userParm: "+userParms[i].name+
          " userValue: "+userParms[i].value);
        }
      }
    }
    System.out.println("REQUEST Returned NO Data");
  }

  /**
   * <p>Performs simple string replacement using regular expressions.
   * All matching occurrences of the pattern string will be replaced in the
   * input string by the replacement string.
   *
   * @param inputString The source string.
   * @param patternString The regular expression pattern.
   * @param replaceString The replacement string.
   * @return The source string with substitutions.
   */
  private String substituteString(String inputString, String patternString,
                                 String replaceString)
  {
    Pattern pattern = Pattern.compile(patternString);
    Matcher m = pattern.matcher(inputString);
    return m.replaceAll(replaceString);
  }
}