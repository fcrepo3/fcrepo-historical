package fedora.server.access;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import fedora.server.Context;
import fedora.server.Module;
import fedora.server.Server;
import fedora.server.access.dissemination.DisseminationService;
import fedora.server.errors.InvalidUserParmException;
import fedora.server.errors.ModuleInitializationException;
import fedora.server.errors.GeneralException;
import fedora.server.errors.ServerException;
import fedora.server.security.IPRestriction;
import fedora.server.storage.DOReader;
import fedora.server.storage.BMechReader;
import fedora.server.storage.DOManager;
import fedora.server.storage.types.DisseminationBindingInfo;
import fedora.server.storage.types.Disseminator;
import fedora.server.storage.types.MethodDef;
import fedora.server.storage.types.MethodParmDef;
import fedora.server.storage.types.MIMETypedStream;
import fedora.server.storage.types.ObjectMethodsDef;
import fedora.server.storage.types.Property;
import fedora.server.utilities.DateUtility;

/**
 *
 * <p>Title: DefaultAccess.java</p>
 * <p>Description: The Access Module, providing support for the Fedora Access
 * subsystem.</p>
 *
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author Ross Wayland
 * @version 1.0
 */
public class DefaultAccess extends Module implements Access
{
  /** Constant holding value of xml MIME type. */
  private final static String CONTENT_TYPE_XML = "text/xml";

  /** Current DOManager of the Fedora server. */
  private DOManager m_manager;

  /** IP Restriction for the Access subsystem. */
  private IPRestriction m_ipRestriction;

  /** Dynamic Access Module */
  // FIXIT!! is this the right way to associate the dynamic access module???
  private DynamicAccessModule m_dynamicAccess;

  /**
   * <p>Creates and initializes the Access Module. When the server is starting
   * up, this is invoked as part of the initialization process.</p>
   *
   * @param moduleParameters A pre-loaded Map of name-value pairs comprising
   *        the intended configuration of this Module.
   * @param server The <code>Server</code> instance.
   * @param role The role this module fulfills, a java class name.
   * @throws ModuleInitializationException If initilization values are
   *         invalid or initialization fails for some other reason.
   */
  public DefaultAccess(Map moduleParameters, Server server, String role)
          throws ModuleInitializationException
  {
    super(moduleParameters, server, role);
  }

  /**
   * <p>Initializes the module.</p>
   *
   * @throws ModuleInitializationException If the module cannot be initialized.
   */
  public void initModule() throws ModuleInitializationException
  {
    m_manager=(DOManager) getServer().getModule(
        "fedora.server.storage.DOManager");
    if (m_manager == null)
    {
      throw new ModuleInitializationException("Can't get a DOManager "
          + "from Server.getModule", getRole());
    }
    String dsMediation = getParameter("doMediateDatastreams");
    if (dsMediation==null)
    {
        throw new ModuleInitializationException(
            "doMediateDatastreams parameter must be specified.", getRole());
    }
    String allowHosts=getParameter("allowHosts");
    String denyHosts=getParameter("denyHosts");
    try {
        m_ipRestriction=new IPRestriction(allowHosts, denyHosts);
    } catch (ServerException se) {
        throw new ModuleInitializationException("Error setting IP restriction "
                + "for Access subsystem: " + se.getClass().getName() + ": "
                + se.getMessage(), getRole());
    }
  }

  public void postInitModule()
      throws ModuleInitializationException
  {
      // get ref to DynamicAccess module
      m_dynamicAccess = (DynamicAccessModule) getServer().
              getModule("fedora.server.access.DynamicAccess");
  }

  /**
   * <p>Gets the persistent identifiers or PIDs of all Behavior Definition
   * objects associated with the specified digital object.</p>
   *
   * @param context The context of this request.
   * @param PID The persistent identifier of the digitla object.
   * @param asOfDateTime The versioning datetime stamp.
   * @return An Array containing the list of Behavior Definition object PIDs.
   * @throws ServerException If any type of error occurred fulfilling the
   *         request.
   */
  public String[] getBehaviorDefinitions(Context context, String PID,
      Calendar asOfDateTime) throws ServerException
  {
    long startTime = new Date().getTime();
    m_ipRestriction.enforce(context);
    // Grab the behavior definitions that are bound to the object
    Date versDateTime = DateUtility.convertCalendarToDate(asOfDateTime);
    String[] behaviorDefs = null;
    DOReader reader =
        m_manager.getReader(context, PID);
    behaviorDefs = reader.GetBehaviorDefs(versDateTime);

    // DYNAMIC!! Grab any dynamic behavior definitions and merge them with
    // the statically bound behavior definitions
    String[] behaviorDefsDynamic =
        m_dynamicAccess.getBehaviorDefinitions(context, PID, asOfDateTime);
    ArrayList bDefList = new ArrayList();
    for (int i=0; i < behaviorDefs.length; i++)
    {
      bDefList.add(behaviorDefs[i]);
    }
    for (int j=0; j < behaviorDefsDynamic.length; j++)
    {
      bDefList.add(behaviorDefsDynamic[j]);
    }
    return (String[])bDefList.toArray(new String[0]);
  }

  /**
   * <p>Gets the method definitions of the Behavior Mechanism object
   * associated with the specified Behavior Definition object in the form of
   * an array of method definitions.</p>
   *
   * @param context The context of this request.
   * @param PID The persistent identifier of the digital object.
   * @param bDefPID The persistent identifier of the Behavior Definition object.
   * @param asOfDateTime The versioning datetime stamp.
   * @return An Array containing the list of method definitions.
   * @throws ServerException If any type of error occurred fulfilling the
   *         request.
   */
  public MethodDef[] getBehaviorMethods(Context context, String PID,
      String bDefPID, Calendar asOfDateTime) throws ServerException
  {
    long startTime = new Date().getTime();
    m_ipRestriction.enforce(context);

    // DYNAMIC!! If the behavior definition (bDefPID) is defined as dynamic, then
    // grab its dynamic method definitions and return.
    if (m_dynamicAccess.isDynamicBehaviorDefinition(context, PID, bDefPID))
    {
      return
        m_dynamicAccess.getBehaviorMethods(context, PID, bDefPID, asOfDateTime);
    }

    Date versDateTime = DateUtility.convertCalendarToDate(asOfDateTime);
    DOReader reader =
        m_manager.getReader(context, PID);
    MethodDef[] methods =
        reader.getObjectMethods(bDefPID, versDateTime);
    return methods;
  }

  /**
   * <p>Gets the method definitions of the Behavior Mechanism object
   * associated with the specified Behavior Definition object in the form of
   * XML as defined in the WSDL.</p>
   *
   * @param context The context of this request.
   * @param PID The persistent identifier of the digital object.
   * @param bDefPID The persistent identifier of the Behavior Definition object.
   * @param asOfDateTime The versioning datetime stamp.
   * @return A MIME-typed stream containing the method definitions in the form
   *         of an XML fragment obtained from the WSDL in the associated
   *         Behavior Mechanism object.
   * @throws ServerException If any type of error occurred fulfilling the
   *         request.
   */
  public MIMETypedStream getBehaviorMethodsXML(Context context,
      String PID, String bDefPID, Calendar asOfDateTime) throws ServerException
  {
    long startTime = new Date().getTime();
    m_ipRestriction.enforce(context);
    try
    {
      ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
      Date versDateTime = DateUtility.convertCalendarToDate(asOfDateTime);
      InputStream methodResults = null;
      DOReader reader =
          m_manager.getReader(context, PID);
      methodResults = reader.getObjectMethodsXML(bDefPID, versDateTime);
      /*int byteStream = 0;
      while ((byteStream = methodResults.read()) >= 0)
      {
        baos.write(byteStream);
      }
      methodResults.close();*/
      if (methodResults != null)
      {
        // RLW: change required by conversion fom byte[] to InputStream
        //MIMETypedStream methodDefs =
        //    new MIMETypedStream(CONTENT_TYPE_XML, baos.toByteArray());
        MIMETypedStream methodDefs =
            new MIMETypedStream(CONTENT_TYPE_XML, methodResults);
        // RLW: change required by conversion fom byte[] to InputStream
        long stopTime = new Date().getTime();
        long interval = stopTime - startTime;
        System.out.println("[DefaultAccess] Roundtrip GetBehaviorMethodsXML: "
                  + interval + " milliseconds.");
        logFiner("[DefaultAccess] Roundtrip GetBehaviorMethodsXML: "
              + interval + " milliseconds.");
        return methodDefs;
      }
    } catch (Throwable th)
    {
      getServer().logWarning(th.getMessage());
      throw new GeneralException("DefaultAccess returned error. The "
                                 + "underlying error was a "
                                 + th.getClass().getName() + "The message "
                                 + "was \"" + th.getMessage() + "\"");
    }
    return null;
  }

  /**
   * <p>Disseminates the content produced by executing the specified method
   * of the associated Behavior Mechanism object of the specified digital
   * object.</p>
   *
   * @param context The context of this request.
   * @param PID The persistent identifier of the digital object.
   * @param bDefPID The persistent identifier of the Behavior Definition object.
   * @param methodName The name of the method to be executed.
   * @param userParms An array of user-supplied method parameters consisting
   *        of name/value pairs.
   * @param asOfDateTime The versioning datetime stamp.
   * @return A MIME-typed stream containing the result of the dissemination.
   * @throws ServerException If any type of error occurred fulfilling the
   *         request.
   */
  public MIMETypedStream getDissemination(Context context, String PID,
      String bDefPID, String methodName, Property[] userParms,
      Calendar asOfDateTime) throws ServerException
  {
    long initStartTime = new Date().getTime();
    m_ipRestriction.enforce(context);

    long startTime = new Date().getTime();
    // DYNAMIC!! If the behavior definition (bDefPID) is defined as dynamic, then
    // perform the dissemination via the DynamicAccess module.
    if (m_dynamicAccess.isDynamicBehaviorDefinition(context, PID, bDefPID))
    {
      return
        m_dynamicAccess.getDissemination(context, PID, bDefPID, methodName,
          userParms, asOfDateTime);
    }
    long stopTime = new Date().getTime();
    long interval = stopTime - startTime;
    System.out.println("[DefaultAccess] Roundtrip DynamicDisseminator: "
        + interval + " milliseconds.");
    logFiner("[DefaultAccess] Roundtrip DynamicDisseminator: "
        + interval + " milliseconds.");

    Date versDateTime = DateUtility.convertCalendarToDate(asOfDateTime);
    Hashtable h_userParms = new Hashtable();
    MIMETypedStream dissemination = null;
    MethodParmDef[] defaultMethodParms = null;
    DOReader reader =
        m_manager.getReader(context, PID);

    // SDP: get a bmech reader to get information that is specific to
    // a mechanism.
    BMechReader bmechreader = null;
    Disseminator[] dissSet = reader.GetDisseminators(versDateTime);
    startTime = new Date().getTime();
    for (int i=0; i<dissSet.length; i++)
    {
      if (dissSet[i].bDefID.equalsIgnoreCase(bDefPID))
      {
        bmechreader = m_manager.getBMechReader(context, dissSet[i].bMechID);
        break;
      }
    }
    stopTime = new Date().getTime();
    interval = stopTime - startTime;
    System.out.println("[DefaultAccess] Roundtrip Looping Diss: "
              + interval + " milliseconds.");
    logFiner("[DefaultAccess] Roundtrip Looping Diss: "
              + interval + " milliseconds.");

    startTime = new Date().getTime();
    // Put any user-supplied method parameters into hash table
    if (userParms != null)
    {
      for (int i=0; i<userParms.length; i++)
      {
        h_userParms.put(userParms[i].name, userParms[i].value);
      }
    }

    // Validate user-supplied parameters
    validateUserParms(context, PID, bDefPID, methodName,
                      h_userParms, versDateTime);

    stopTime = new Date().getTime();
    interval = stopTime - startTime;
    System.out.println("[DefaultAccess] Roundtrip Get/Validate User Parms: "
        + interval + " milliseconds.");
    logFiner("[DefaultAccess] Roundtrip Get/Validate User Parms: "
        + interval + " milliseconds.");

    startTime = new Date().getTime();
    // SDP: GET INFO FROM BMECH READER:
    // Add any default method parameters to validated user parm list
    //defaultMethodParms = reader.GetBMechDefaultMethodParms(bDefPID,
    defaultMethodParms = bmechreader.getServiceMethodParms(methodName, versDateTime);
    for (int i=0; i<defaultMethodParms.length; i++)
    {
      this.getServer().logFinest("addedDefaultName: "+defaultMethodParms[i].parmName);
      this.getServer().logFinest("addedDefaultValue: "+defaultMethodParms[i].parmDefaultValue);
      h_userParms.put(defaultMethodParms[i].parmName,
                      defaultMethodParms[i].parmDefaultValue);
    }

    stopTime = new Date().getTime();
    interval = stopTime - startTime;
    System.out.println("[DefaultAccess] Roundtrip Get BMech Parms: "
        + interval + " milliseconds.");
    logFiner("[DefaultAccess] Roundtrip Get BMech Parms: "
        + interval + " milliseconds.");

    startTime = new Date().getTime();
    // Get dissemination binding info.
    DisseminationBindingInfo[] dissBindInfo =
        reader.getDisseminationBindingInfo(bDefPID, methodName, versDateTime);

    // Assemble and execute the dissemination request from the binding info.
    DisseminationService dissService = new DisseminationService();
    dissemination =
        dissService.assembleDissemination(PID, h_userParms, dissBindInfo);

    stopTime = new Date().getTime();
    interval = stopTime - startTime;
    System.out.println("[DefaultAccess] Roundtrip Assemble Dissemination: "
        + interval + " milliseconds.");
    logFiner("[DefaultAccess] Roundtrip Assemble Dissemination: "
        + interval + " milliseconds.");

    stopTime = new Date().getTime();
    interval = stopTime - initStartTime;
    System.out.println("[DefaultAccess] Roundtrip GetDissemination: "
              + interval + " milliseconds.");
    logFiner("[DefaultAccess] Roundtrip GetDissemination: "
              + interval + " milliseconds.");
    return dissemination;
  }

  /**
   * <p>Gets a list of all Behavior Definition object PIDs and method names
   * associated with the specified digital object.</p>
   *
   * @param context The context of this request.
   * @param PID The persistent identifier of the digital object
   * @param asOfDateTime The versioning datetime stamp
   * @return An array of all methods associated with the specified
   *         digital object.
   * @throws ServerException If any type of error occurred fulfilling the
   *         request.
   */
  public ObjectMethodsDef[] getObjectMethods(Context context, String PID,
      Calendar asOfDateTime) throws ServerException
  {
    long startTime = new Date().getTime();
    m_ipRestriction.enforce(context);
    Date versDateTime = DateUtility.convertCalendarToDate(asOfDateTime);
    DOReader reader =
        m_manager.getReader(context, PID);
    ObjectMethodsDef[] methodDefs =
        reader.getObjectMethods(versDateTime);
    long stopTime = new Date().getTime();
    long interval = stopTime - startTime;
    System.out.println("[DefaultAccess] Roundtrip GetObjectMethods: "
              + interval + " milliseconds.");

    // DYNAMIC!! Grab any dynamic method definitions and merge them with
    // the statically bound method definitions
    ObjectMethodsDef[] dynamicMethodDefs =
        m_dynamicAccess.getObjectMethods(context, PID, asOfDateTime);
    ArrayList methodList = new ArrayList();
    for (int i=0; i < methodDefs.length; i++)
    {
      methodList.add(methodDefs[i]);
    }
    for (int j=0; j < dynamicMethodDefs.length; j++)
    {
      methodList.add(dynamicMethodDefs[j]);
    }
System.out.println("EXAMINE OBJECT METHODS DEF: (before return from DefaultAccess)");
ObjectMethodsDef[] methods = (ObjectMethodsDef[])methodList.toArray(new ObjectMethodsDef[0]);
System.out.println(">>>>> CNT METHOD ARRAY: " + methods.length);
for (int k=0; k<methods.length; k++)
{
  System.out.println(">> PID= " + methods[k].PID);
  System.out.println(">> bdefpid= " + methods[k].bDefPID);
  System.out.println(">> asOfDate= " + methods[k].asOfDate);
  System.out.println(">> methodname= " + methods[k].methodName);
  System.out.println(">>>>> CNT PARM ARRAY: " + methods[k].methodParmDefs.length);
  for (int m=0; m<methods[k].methodParmDefs.length; m++)
  {
    System.out.println(">> parmName = " + methods[k].methodParmDefs[m].parmName);
    System.out.println(">> parmLabel = " + methods[k].methodParmDefs[m].parmLabel);
    System.out.println(">> parmDefVal = " + methods[k].methodParmDefs[m].parmDefaultValue);
    System.out.println(">>>>> CNT DOM VAL ARRAY: " + methods[k].methodParmDefs[m].parmDomainValues.length);
    System.out.println(">> parmDomVal[] = " + methods[k].methodParmDefs[m].parmDomainValues);
    System.out.println(">> parmPassby = " + methods[k].methodParmDefs[m].parmPassBy);
    System.out.println(">> parmReq = " + methods[k].methodParmDefs[m].parmRequired);
    System.out.println(">> parmType = " + methods[k].methodParmDefs[m].parmType);
  }
}
return methods;
    //return (ObjectMethodsDef[])methodList.toArray(new ObjectMethodsDef[0]);
  }

  public List search(Context context, String[] resultFields,
          String terms)
          throws ServerException {
    m_ipRestriction.enforce(context);
    return m_manager.search(context, resultFields, terms);
  }

  public List search(Context context, String[] resultFields,
          List conditions)
          throws ServerException {
    m_ipRestriction.enforce(context);
    return m_manager.search(context, resultFields, conditions);
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
   * @param context The context of this request.
   * @param PID The persistent identifier of the digital object.
   * @param bDefPID The persistent identifier of the Behavior Definition object.
   * @param methodName The name of the method.
   * @param h_userParms A hashtable of user-supplied method parameter
   *        name/value pairs.
   * @param versDateTime The version datetime stamp of the digital object.
   * @throws ServerException If any type of error occurred fulfilling the
   *         request.
   *
   */
  private void validateUserParms(Context context, String PID, String bDefPID,
      String methodName, Hashtable h_userParms, Date versDateTime)
      throws ServerException
  {
    m_ipRestriction.enforce(context);
    DOReader fdor = null;
    MethodParmDef[] methodParms = null;
    MethodParmDef methodParm = null;
    StringBuffer sb = new StringBuffer();
    Hashtable h_validParms = new Hashtable();
    boolean isValid = true;

    DOReader reader =
      m_manager.getReader(context, PID);
    methodParms = reader.getObjectMethodParms(bDefPID,
        methodName, versDateTime);

    // Put valid method parameters and their attributes into hashtable
    if (methodParms != null)
    {
      for (int i=0; i<methodParms.length; i++)
      {
        methodParm = methodParms[i];
        h_validParms.put(methodParm.parmName,methodParm);
        this.getServer().logFinest("methodParms[" + i + "]: "
            + methodParms[i].parmName
            + "\nlabel: " + methodParms[i].parmLabel
            + "\ndefault: " + methodParms[i].parmDefaultValue
            + "\nrequired: " + methodParms[i].parmRequired
            + "\ntype: " + methodParms[i].parmType);
        for (int j=0; j<methodParms[i].parmDomainValues.length; j++)
        {
          this.getServer().logFinest("domainValues: "
              + methodParms[i].parmDomainValues[j]);
        }
      }
    }

    if (!h_validParms.isEmpty())
    {
      // Iterate over valid parmameters to check for any missing required parms.
      Enumeration e = h_validParms.keys();
      while (e.hasMoreElements())
      {
        String validName = (String)e.nextElement();
        MethodParmDef mp = (MethodParmDef)h_validParms.get(validName);
        if(mp.parmRequired && h_userParms.get(validName) == null)
        {
          // This is a fatal error. A required method parameter does not
          // appear in the list of user supplied parameters.
          sb.append("The required parameter \""
              + validName + "\" was not found in the "
              + "user-supplied parameter list.");
          throw new InvalidUserParmException("[Invalid User Parameters] "
              + sb.toString());
        }
      }

      // Iterate over each user supplied parameter name
      Enumeration parmNames = h_userParms.keys();
      while (parmNames.hasMoreElements())
      {
        String parmName = (String)parmNames.nextElement();
        methodParm = (MethodParmDef)h_validParms.get(parmName);
        if (methodParm != null && methodParm.parmName != null)
        {
          // Method has one or more parameters defined
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
            } else
            {
              // This is a non-fatal error. There is no default specified
              // for this parameter and the user has supplied no value for
              // the parameter. The value of the empty string will be used
              // as the value of the parameter.
              this.getServer().logWarning("The method parameter \""
                  + methodParm.parmName
                  + "\" has no default value and no "
                  + "value was specified by the user.  "
                  + "The value of the empty string has "
                  + "been assigned to this parameter.");
            }
          } else
          {
            // Value of user-supplied parameter contains a value.
            // Validate the supplied value against the parmDomainValues list.
            String[] parmDomainValues = methodParm.parmDomainValues;
            if (!parmDomainValues[0].equalsIgnoreCase("null"))
            {
              boolean isValidValue = false;
              String userValue = (String)h_userParms.get(methodParm.parmName);
              for (int i=0; i<parmDomainValues.length; i++)
              {
                if (userValue.equalsIgnoreCase(parmDomainValues[i]) ||
                    parmDomainValues[i].equalsIgnoreCase("null"))
                {
                  isValidValue = true;
                }
              }
              if (!isValidValue)
              {
                // This is a fatal error. The value supplied for this method
                // parameter does not match any of the values specified by
                // this method.
                StringBuffer values = new StringBuffer();
                for (int i=0; i<parmDomainValues.length; i++)
                {
                  if (i == parmDomainValues.length-1)
                  {
                    sb.append(parmDomainValues[i]);
                  } else
                  {
                    sb.append(parmDomainValues[i]+", ");
                  }
                }
                sb.append("The method parameter \""
                          + methodParm.parmName
                          + "\" with a value of \""
                          + (String)h_userParms.get(methodParm.parmName)
                          + "\" is not allowed for the method \""
                          + methodName + "\". Allowed values for this "
                          + "method include \"" + sb.toString() + "\".");
                isValid = false;
              }
            }
          }
        } else
        {
          // This is a fatal error. A user-supplied parameter name does
          // not match any valid parameter names for this method.
          sb.append("The method parameter \"" + parmName
                    + "\" is not valid for the method \""
                    + methodName + "\".");
          isValid = false;
        }
      }
    } else
    {
      // There are no method parameters define for this method.
      if (!h_userParms.isEmpty())
      {
        // This is an error. There are no method parameters defined for
        // this method and user parameters are specified in the
        // dissemination request.
        Enumeration e = h_userParms.keys();
        while (e.hasMoreElements())
        {
          sb.append("The method parameter \"" + (String)e.nextElement()
                    + "\" is not valid for the method \""
                    + methodName + "\"."
                    + "The method \"" + methodName
                    + "\" defines no method parameters.");
        }
        throw new InvalidUserParmException("[Invalid User Parameters] "
            + sb.toString());
      }
    }
    if (!isValid)
    {
      throw new InvalidUserParmException("[Invalid User Parameter] "
          + sb.toString());
    }
    return;
  }
}
