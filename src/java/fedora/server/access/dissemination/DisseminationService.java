package fedora.server.access.dissemination;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fedora.server.Server;
import fedora.server.errors.DisseminationException;
import fedora.server.errors.DisseminationBindingInfoNotFoundException;
import fedora.server.errors.InitializationException;
import fedora.server.errors.ServerException;
import fedora.server.errors.ServerInitializationException;
import fedora.server.storage.types.DisseminationBindingInfo;
import fedora.server.storage.ExternalContentManager;
import fedora.server.storage.ConnectionPool;
import fedora.server.storage.ConnectionPoolManager;
import fedora.server.storage.types.MIMETypedStream;
import fedora.server.storage.types.DatastreamMediation;

/**
 * <p>Title: DisseminationService.java</p>
 * <p>Description: A service for executing a dissemination given its
 * binding information.</p>
 *
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author Ross Wayland
 * @version 1.0
 */
public class DisseminationService
{

  /** The Fedora Server instance */
  private static Server s_server;

  /** Signifies the special type of address location known as LOCAL.
   *  An address location of LOCAL implies that no remote host name is
   *  required for the address location and that the contents of the
   *  operation location are sufficient to execute the associated mechanism.
   */
  private static final String LOCAL_ADDRESS_LOCATION = "LOCAL";

  /** Port number on which the Fedora server is running. */
  private static String fedoraServerPort = null;

  /** IP address of the Fedora server; determined dynamically from this class.*/
  private static String fedoraServerAddress = null;

  /** The database ConnectionPool */
  private static ConnectionPool connectionPool = null;

  /** The expiration limit in minutes for removing entries from the database. */
  private static int datastreamExpirationLimit = 0;

  /** An incremental counter used to insure uniqueness of tempIDs used for
   * datastream mediation.
   */
  private static int counter = 0;

  /** Make sure we have a server instance for error logging purposes. */
  static
  {
    try
    {
      String fedoraHome = System.getProperty("fedora.home");
      if (fedoraHome == null)
      {
          throw new ServerInitializationException(
              "Server failed to initialize: The 'fedora.home' "
              + "system property was not set.");
      } else
      {
          s_server = Server.getInstance(new File(fedoraHome));
      }
    } catch (InitializationException ie)
    {
        System.err.println(ie.getMessage());
    }
  }

  /** The hashtable containing information required for datastream mediation */
  protected static Hashtable dsRegistry = new Hashtable(100);

  /**
   * <p>Constructs an instance of DisseminationService. Initializes two class
   * variables that contain the IP address and port number of the Fedora server.
   * The port number is obtained from the Fedora server config file and the IP
   * address of the server is obtained dynamically. These variables are needed
   * to perform the datastream proxy service for datastream requests.</p>
   *
   * @throws ServerException If the port number or the IP address of the Fedora
   *         server cannot be obtained.
   */
  public DisseminationService() throws ServerException
  {
    InetAddress host = null;
    try
    {
      this.fedoraServerPort = s_server.getParameter("fedoraServerPort");
      s_server.logFinest("fedoraServerPort: " + fedoraServerPort);
      if (fedoraServerPort == null || fedoraServerPort.equalsIgnoreCase(""))
      {
        throw new DisseminationException("DisseminationService was unable to "
            + "resolve the port number of the Fedora Server: "
            + fedoraServerAddress + "from the configuration file. This "
            + "information is required by the Dissemination Service.");
      }
      host = InetAddress.getLocalHost();
      this.fedoraServerAddress = "http://" + host.getHostAddress()
                                  + ":" + fedoraServerPort
                                  + "/fedora/getDS?id=";
      s_server.logFinest("fedoraServerAddress: " + fedoraServerAddress);
      ConnectionPoolManager poolManager = (ConnectionPoolManager)
          s_server.getModule("fedora.server.storage.ConnectionPoolManager");
      this.connectionPool = poolManager.getPool();
      String expireLimit = s_server.getParameter("datastreamExpirationLimit");
      if (expireLimit == null || expireLimit.equalsIgnoreCase(""))
      {
        s_server.logWarning("DisseminationService was unable to "
            + "resolve the datastream expiration limit from the configuration "
            + "file. The expiration limit has been set to 5 minutes.");
        datastreamExpirationLimit = 5;
      } else
      {
        Integer I1 =
            new Integer(expireLimit);
        datastreamExpirationLimit = I1.intValue();
        s_server.logFinest("datastreamExpirationLimit: "
                           + datastreamExpirationLimit);
      }
    } catch (UnknownHostException uhe)
    {
      throw new DisseminationException("DisseminationService was unable to "
          + "resolve the IP address of the Fedora Server: "
          + fedoraServerAddress + "."
          + " The underlying error was a "
          + uhe.getClass().getName() + "The message "
          + "was \"" + uhe.getMessage() + "\"");
    }
  }

  /**
   * <p>Assembles a dissemination given an instance of <code>
   * DisseminationBindingInfo</code> which has the dissemination-related
   * information from the digital object and its associated Behavior
   * Mechanism object.</p>
   *
   * @param PID The persistent identifier of the digital object.
   * @param h_userParms A hashtable of user-supplied method parameters.
   * @param dissBindInfoArray The associated dissemination binding information.
   * @return A MIME-typed stream containing the result of the dissemination.
   * @throws ServerException If unable to assemble the dissemination for any
   *         reason.
   */
  public MIMETypedStream assembleDissemination(String PID,
      Hashtable h_userParms, DisseminationBindingInfo[] dissBindInfoArray)
      throws ServerException
  {
    String protocolType = null;
    DisseminationBindingInfo dissBindInfo = null;
    String dissURL = null;
    String operationLocation = null;
    MIMETypedStream dissemination = null;

    if (dissBindInfoArray != null && dissBindInfoArray.length > 0)
    {
      String replaceString = null;
      int numElements = dissBindInfoArray.length;

      // Get row(s) of binding info and perform string substitution
      // on DSBindingKey and method parameter values in WSDL
      // Note: In case where more than one datastream matches the
      // DSBindingKey or there are multiple DSBindingKeys for the
      // method, multiple rows will be present; otherwise there us only
      // a single row.
      for (int i=0; i<dissBindInfoArray.length; i++)
      {
        dissBindInfo = dissBindInfoArray[i];

        // If AddressLocation has a value of "LOCAL", this indicates
        // the associated OperationLocation requires no AddressLocation.
        // i.e., the OperationLocation contains all information necessary
        // to perform the dissemination request. This is a special case
        // used when the web services are generally mechanisms like cgi-scripts,
        // java servlets, and simple HTTP GETs. Using the value of LOCAL
        // in the address location also enables one to have different methods
        // running on different hosts. In true web services like SOAP, the
        // AddressLocation specifies the host name of the service and all
        // methods are served from that single host location.
        if (dissBindInfo.AddressLocation.
            equalsIgnoreCase(LOCAL_ADDRESS_LOCATION))
        {
          dissBindInfo.AddressLocation = "";
        }

        // Match DSBindingKey pattern in WSDL which is a string of the form:
        // (DSBindingKey). Rows in DisseminationBindingInfo are sorted
        // alphabeticaly on binding key.
        String bindingKeyPattern = "\\(" + dissBindInfo.DSBindKey + "\\)";
        if (i == 0)
        {
          operationLocation = dissBindInfo.OperationLocation;
          dissURL =
              dissBindInfo.AddressLocation+dissBindInfo.OperationLocation;
          protocolType = dissBindInfo.ProtocolType;
        }
        String currentKey = dissBindInfo.DSBindKey;
        String nextKey = "";
        if (i != numElements-1)
        {
          // Except for last row, get the value of the next binding key
          // to compare with the value of the current binding key.
          s_server.logFinest("currentKey: '" + currentKey + "'");
          nextKey = dissBindInfoArray[i+1].DSBindKey;
          s_server.logFinest("nextKey: '" + nextKey + "'");
        }
s_server.logFinest("currentKey: '" + currentKey + "'");
s_server.logFinest("nextKey: '" + nextKey + "'");
        // In most cases, there is only a single datastream that matches a
        // given DSBindingKey so the substitution process is to just replace
        // the occurence of (BINDING_KEY) with the value of the datastream
        // location. However, when multiple datastreams match the same
        // DSBindingKey, the occurrence of (BINDING_KEY) is replaced with the
        // value of the datastream location and the value +(BINDING_KEY) is
        // appended so that subsequent datastreams matching the binding key
        // will be substituted. The end result is that the binding key will
        // be replaced by a series of datastream locations separated by a
        // plus(+) sign. For example, in the case where 3 datastreams match
        // the binding key for PHOTO:
        //
        // file=(PHOTO) becomes
        // file=dslocation1+dslocation2+dslocation3
        //
        // It is the responsibility of the Behavior Mechanism to know how to
        // handle an input parameter with multiple datastream locations.
        //
        // In the case of a method containing multiple binding keys,
        // substitutions are performed on each binding key. For example, in
        // the case where there are 2 binding keys named PHOTO and WATERMARK
        // where each matches a single datastream:
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
          // Case where binding keys are equal which means that multiple
          // datastreams matched the same binding key.
          replaceString = fedoraServerAddress
              + registerDatastreamLocation(dissBindInfo.dsLocation,
                                           dissBindInfo.dsControlGroupType)
              + "+(" + dissBindInfo.DSBindKey + ")";
        } else
        {
          // Case where there are multiple binding keys.
          replaceString = fedoraServerAddress
              + registerDatastreamLocation(dissBindInfo.dsLocation,
                                           dissBindInfo.dsControlGroupType);
        }
        dissURL = substituteString(dissURL, bindingKeyPattern, replaceString);
        s_server.logFinest("replaced dissURL: "
                           + dissURL.toString()
                           + " DissBindingInfo index: " + i);
      }

      // Substitute user-supplied parameter values in dissemination URL
      Enumeration e = h_userParms.keys();
      while (e.hasMoreElements())
      {
        String name = (String)e.nextElement();
        String value = (String)h_userParms.get(name);
        String pattern = "\\(" + name + "\\)";
        dissURL = substituteString(dissURL, pattern, value);
        s_server.logFinest("User parm substituted in URL: " + dissURL);
      }

      // Resolve content referenced by dissemination result.
      s_server.logFinest("ProtocolType: " + protocolType);
      if (protocolType.equalsIgnoreCase("http"))
      {
        ExternalContentManager externalContentManager = (ExternalContentManager)
            s_server.getModule("fedora.server.storage.ExternalContentManager");
        dissemination = externalContentManager.getExternalContent(dissURL);

      } else if (protocolType.equalsIgnoreCase("soap"))
      {
        // FIXME!! future handling of soap bindings.
        String message = "[DisseminationService] Protocol type: "
            + protocolType + "NOT yet implemented";
        s_server.logWarning(message);
        throw new DisseminationException(message);

      } else
      {
        String message = "[DisseminationService] Protocol type: "
            + protocolType + "NOT supported.";
        s_server.logWarning(message);
        throw new DisseminationException(message);
      }

    } else
    {
      // DisseminationBindingInfo was empty so there was no information
      // provided to construct a dissemination.
      String message = "[DisseminationService] Dissemination Binding "+
                         "Info contained no data";
      s_server.logWarning(message);
      throw new DisseminationBindingInfoNotFoundException(message);
    }
    return dissemination;
  }


  /**
   * <p>Datastream locations are considered privileged information by the
   * Fedora repository. To prevent disclosing physical datastream locations
   * to external mechanism services, a proxy is used to disguise the datastream
   * locations. This method generates a temporary ID that maps to the
   * physical datastream location and registers this information in a
   * memory resident hashtable for subsequent resolution of the physical
   * datastream location. The servlet <code>DatastreamResolverServlet</code>
   * provides the proxy resolution service for datastreams.</p>
   * <p></p>
   * <p>The format of the tempID is derived from <code>java.sql.Timestamp</code>
   * with an arbitrary counter appended to the end to insure uniqueness. The
   * syntax is of the form:
   * <ul>
   * <p>YYYY-MM-DDThh:mm:ss.mmm:dddddd where</p>
   * <ul>
   * <li>YYYY - year (1970-2037)</li>
   * <li>MM - month (01-12)</li>
   * <li>DD - day (01-31)</li>
   * <li>hh - hours (0-23)</li>
   * <li>mm - minutes (0-59)</li>
   * <li>ss - seconds (0-59)</li>
   * <li>mmm - milliseconds (0-999)</li>
   * <li>dddddd - incremental counter (0-999999)</li>
   * </ul>
   * </ul>
   * <p>The "T" character between the date and time is added to avoid having a
   * blank in the URL
   *
   * @param dsLocation The physical location of the datastream.
   * @param dsControlGroupType The type of the datastream.
   * @return A temporary ID used to reference the physical location of the
   *         specified datastream
   * @throws ServerException If an error occurs in connecting or updating the
   *         database or an illegal Control Group Type is encountered.
   */
  public String registerDatastreamLocation(String dsLocation,
      String dsControlGroupType) throws ServerException
  {

    String tempID = null;
    Timestamp timeStamp = null;
    if (counter > 999999) counter = 0;
    long currentTime = new Timestamp(new Date().getTime()).getTime();
    long expireLimit = currentTime -
                       (long)datastreamExpirationLimit*1000;

    try
    {

      // Remove any datastream registrations that have expired.
      // The expiration limit can be adjusted using the Fedora config parameter
      // named "datastreamExpirationLimit" which is in seconds.
      for ( Enumeration e = dsRegistry.keys(); e.hasMoreElements(); )
      {
        String key = (String)e.nextElement();
        timeStamp = timeStamp.valueOf(extractTimestamp(key));
        if (expireLimit > timeStamp.getTime())
        {
          dsRegistry.remove(key);
          s_server.logFinest("DatastreamMediationKey removed from Hash: "+key);
        }
      }

      // Register datastream.
      if (tempID == null)
      {
        timeStamp = new Timestamp(new Date().getTime());
        tempID = timeStamp.toString()+":"+counter++;
        DatastreamMediation dm = new DatastreamMediation();
        dm.mediatedDatastreamID = tempID;
        dm.dsLocation = dsLocation;
        dm.dsControlGroupType = dsControlGroupType;
        dsRegistry.put(tempID, dm);
        s_server.logFinest("DatastreammediationKey added to Hash: "+tempID);
      }

    } catch(Throwable th)
    {
      throw new DisseminationException("[DisseminationService]register"
          + "DatastreamLocation: "
          + "returned an error.\nThe underlying error was a "
          + th.getClass().getName() + "\nThe message "
          + "was \"" + th.getMessage() + "\" .");
    }

    // Since the tempID will eventually appear in a URL, escape any blanks with
    // a plus sign.
    return tempID.replaceAll(" ","T");
  }

  /**
   * <p>The tempID that is used for datastream mediation consists of a <code>
   * Timestamp</code> plus a counter appended to the end to insure uniqueness.
   * This method is a utility method used to extract the Timestamp portion
   * from the tempID by stripping off the arbitrary counter at the end of
   * the string.</p>
   *
   * @param tempID The tempID to be extracted.
   * @return The extracted Timestamp value as a string.
   */
  public String extractTimestamp(String tempID)
  {
    StringBuffer sb = new StringBuffer();
    sb.append(tempID);
    sb.replace(tempID.lastIndexOf(":"),tempID.length(),"");
    return sb.toString();
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