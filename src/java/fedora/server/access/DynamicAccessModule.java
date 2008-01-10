/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */

package fedora.server.access;

import java.io.File;

import java.net.InetAddress;
import java.net.UnknownHostException;

import java.util.Date;
import java.util.Hashtable;
import java.util.Map;

import org.apache.log4j.Logger;

import fedora.server.Context;
import fedora.server.Module;
import fedora.server.Server;
import fedora.server.errors.ModuleInitializationException;
import fedora.server.errors.ServerException;
import fedora.server.search.FieldSearchQuery;
import fedora.server.search.FieldSearchResult;
import fedora.server.storage.DOManager;
import fedora.server.storage.types.DatastreamDef;
import fedora.server.storage.types.MIMETypedStream;
import fedora.server.storage.types.MethodDef;
import fedora.server.storage.types.ObjectMethodsDef;
import fedora.server.storage.types.Property;

/**
 * Module Wrapper for DynamicAccessImpl.
 * 
 * <p>The Dynamic Access module will associate dynamic disseminators with
 * a digital object.  It will look to the Fedora repository configuration file 
 * to obtain a list of dynamic disseminators. Currently, the system supports 
 * two types of dynamic disseminators: - Default (BDefPID=fedora-system:3 and
 * BMechPID=fedora-system:4) - Bootstrap (BDefPID=fedora-system:1 and
 * BMechPID=fedora-system:2). The Default disseminator that is associated with
 * every object in the repository. The Default Disseminator endows the objects
 * with a set of basic generic behaviors that enable a simplistic view of the
 * object contents (the Item Index) and a list of all disseminations available
 * on the object (the Dissemination Index). The Bootstrap disseminator is
 * associated with every behavior definition and behavior mechanism object. It
 * defines methods to get the special metadata datastreams out of them, and some
 * other methods. (NOTE: The Bootstrap Disseminator functionality is NOT YET
 * IMPLEMENTED.
 * 
 * @author Sandy Payette
 */
public class DynamicAccessModule
        extends Module
        implements Access {

    /** Logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(DynamicAccessModule.class.getName());

    /**
     * An instance of the core implementation class for DynamicAccess. The
     * DynamicAccessModule acts as a wrapper to this class.
     */
    private DynamicAccessImpl da = null;;

    /** Current DOManager of the Fedora server. */
    private DOManager m_manager;

    /** Main Access module of the Fedora server. */
    private Access m_access;

    private Hashtable dynamicBDefToMech = null;

    private File reposHomeDir = null;

    /**
     * Creates and initializes the Dynmamic Access Module. When the server is
     * starting up, this is invoked as part of the initialization process.
     * 
     * @param moduleParameters
     *        A pre-loaded Map of name-value pairs comprising the intended
     *        configuration of this Module.
     * @param server
     *        The <code>Server</code> instance.
     * @param role
     *        The role this module fulfills, a java class name.
     * @throws ModuleInitializationException
     *         If initilization values are invalid or initialization fails for
     *         some other reason.
     */
    public DynamicAccessModule(Map moduleParameters, Server server, String role)
            throws ModuleInitializationException {
        super(moduleParameters, server, role);
    }

    @Override
    public void postInitModule() throws ModuleInitializationException {
        m_manager =
                (DOManager) getServer()
                        .getModule("fedora.server.storage.DOManager");
        if (m_manager == null) {
            throw new ModuleInitializationException("[DynamicAccessModule] "
                    + "Can't get a DOManager from Server.getModule", getRole());
        }
        m_access =
                (Access) getServer().getModule("fedora.server.access.Access");
        if (m_access == null) {
            throw new ModuleInitializationException("[DynamicAccessModule] "
                                                            + "Can't get a ref to Access from Server.getModule",
                                                    getRole());
        }
        // Get the repository Base URL
        InetAddress hostIP = null;
        try {
            hostIP = InetAddress.getLocalHost();
        } catch (UnknownHostException uhe) {
            LOG.error("Unable to resolve Fedora host", uhe);
        }
        String fedoraServerHost = getServer().getParameter("fedoraServerHost");
        if (fedoraServerHost == null || fedoraServerHost.equals("")) {
            fedoraServerHost = hostIP.getHostName();
        }
        reposHomeDir = getServer().getHomeDir();

        // FIXIT!! In the future, we want to read the repository configuration
        // file for the list of dynamic behavior definitions and their
        // associated internal service classes.  For now, we are explicitly
        // loading up the Default behavior def/mech since this is the only
        // thing supported in the system right now.
        dynamicBDefToMech = new Hashtable();
        try {
            dynamicBDefToMech.put("fedora-system:3", Class
                    .forName(getParameter("fedora-system:4")));
        } catch (Exception e) {
            throw new ModuleInitializationException(e.getMessage(),
                                                    "fedora.server.validation.DOValidatorModule");
        }

        // get ref to the Dynamic Access implementation class
        da = new DynamicAccessImpl(m_access, reposHomeDir, dynamicBDefToMech);
    }

    /**
     * Get a list of behavior definition identifiers for dynamic disseminators
     * associated with the digital object.
     * 
     * @param context
     * @param PID
     *        identifier of digital object being reflected upon
     * @param asOfDateTime
     * @return an array of behavior definition PIDs
     * @throws ServerException
     */
    public String[] getBehaviorDefinitions(Context context,
                                           String PID,
                                           Date asOfDateTime)
            throws ServerException {
        //m_ipRestriction.enforce(context);
        return da.getBehaviorDefinitions(context, PID, asOfDateTime);
    }

    /**
     * Get the behavior method defintions for a given dynamic disseminator that
     * is associated with the digital object. The dynamic disseminator is
     * identified by the bDefPID.
     * 
     * @param context
     * @param PID
     *        identifier of digital object being reflected upon
     * @param bDefPID
     *        identifier of dynamic behavior definition
     * @param asOfDateTime
     * @return an array of method definitions
     * @throws ServerException
     */
    public MethodDef[] getBehaviorMethods(Context context,
                                          String PID,
                                          String bDefPID,
                                          Date asOfDateTime)
            throws ServerException {
        //m_ipRestriction.enforce(context);
        return da.getBehaviorMethods(context, PID, bDefPID, asOfDateTime);
    }

    /**
     * Get an XML encoding of the behavior defintions for a given dynamic
     * disseminator that is associated with the digital object. The dynamic
     * disseminator is identified by the bDefPID.
     * 
     * @param context
     * @param PID
     *        identifier of digital object being reflected upon
     * @param bDefPID
     *        identifier of dynamic behavior definition
     * @param asOfDateTime
     * @return MIME-typed stream containing XML-encoded method definitions
     * @throws ServerException
     */
    public MIMETypedStream getBehaviorMethodsXML(Context context,
                                                 String PID,
                                                 String bDefPID,
                                                 Date asOfDateTime)
            throws ServerException {
        //m_ipRestriction.enforce(context);
        return da.getBehaviorMethodsXML(context, PID, bDefPID, asOfDateTime);
    }

    public MIMETypedStream getDatastreamDissemination(Context context,
                                                      String PID,
                                                      String dsID,
                                                      Date asOfDateTime)
            throws ServerException {
        return da.getDatastreamDissemination(context, PID, dsID, asOfDateTime);
    }

    /**
     * Perform a dissemination for a behavior method that belongs to a dynamic
     * disseminator that is associate with the digital object. The method
     * belongs to the dynamic behavior definition and is implemented by a
     * dynamic behavior mechanism (which is an internal service in the
     * repository access subsystem).
     * 
     * @param context
     * @param PID
     *        identifier of the digital object being disseminated
     * @param bDefPID
     *        identifier of dynamic behavior definition
     * @param methodName
     * @param userParms
     * @param asOfDateTime
     * @return a MIME-typed stream containing the dissemination result
     * @throws ServerException
     */
    public MIMETypedStream getDissemination(Context context,
                                            String PID,
                                            String bDefPID,
                                            String methodName,
                                            Property[] userParms,
                                            Date asOfDateTime)
            throws ServerException {
        
        setParameter("useCachedObject", "" + false); //<<<STILL REQUIRED?

        return da
                .getDissemination(context,
                                  PID,
                                  bDefPID,
                                  methodName,
                                  userParms,
                                  asOfDateTime,
                                  m_manager
                                          .getReader(Server.USE_DEFINITIVE_STORE,
                                                     context,
                                                     PID));
    }

    /**
     * Get the definitions for all dynamic disseminations on the object.
     * 
     * <p>This will return the method definitions for all methods for all of 
     * the dynamic disseminators associated with the object.
     * 
     * @param context
     * @param PID
     *        identifier of digital object being reflected upon
     * @param asOfDateTime
     * @return an array of object method definitions
     * @throws ServerException
     */
    public ObjectMethodsDef[] listMethods(Context context,
                                          String PID,
                                          Date asOfDateTime)
            throws ServerException {
        return da.listMethods(context, PID, asOfDateTime);
    }

    /**
     * Get the profile information for the digital object. This contain key
     * metadata and URLs for the Dissemination Index and Item Index of the
     * object.
     * 
     * @param context
     * @param PID
     *        identifier of digital object being reflected upon
     * @param asOfDateTime
     * @return an object profile data structure
     * @throws ServerException
     */
    public ObjectProfile getObjectProfile(Context context,
                                          String PID,
                                          Date asOfDateTime)
            throws ServerException {
        return null;
    }

    // FIXIT: What do these mean in this context...anything?
    // Maybe these methods' exposure needs to be re-thought?
    public FieldSearchResult findObjects(Context context,
                                         String[] resultFields,
                                         int maxResults,
                                         FieldSearchQuery query)
            throws ServerException {
        return null;
    }

    // FIXIT: What do these mean in this context...anything?
    // Maybe these methods' exposure needs to be re-thought?
    public FieldSearchResult resumeFindObjects(Context context,
                                               String sessionToken)
            throws ServerException {
        return null;
    }

    // FIXIT: What do these mean in this context...anything?
    // Maybe these methods' exposure needs to be re-thought?
    public RepositoryInfo describeRepository(Context context)
            throws ServerException {
        return null;
    }

    // FIXIT: What do these mean in this context...anything?
    // Maybe these methods' exposure needs to be re-thought?
    public String[] getObjectHistory(Context context, String PID)
            throws ServerException {
        return da.getObjectHistory(context, PID);
    }

    protected boolean isDynamicBehaviorDefinition(Context context,
                                                  String PID,
                                                  String bDefPID)
            throws ServerException {
        return da.isDynamicBehaviorDefinition(context, PID, bDefPID);
    }

    public DatastreamDef[] listDatastreams(Context context,
                                           String PID,
                                           Date asOfDateTime)
            throws ServerException {
        return da.listDatastreams(context, PID, asOfDateTime);
    }
}
