/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */

package fedora.server.access;

import java.io.File;

import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;

import fedora.common.Constants;

import fedora.server.Context;
import fedora.server.Module;
import fedora.server.access.defaultdisseminator.DefaultDisseminatorImpl;
import fedora.server.access.defaultdisseminator.ServiceMethodDispatcher;
import fedora.server.errors.GeneralException;
import fedora.server.errors.MethodNotFoundException;
import fedora.server.errors.ServerException;
import fedora.server.search.FieldSearchQuery;
import fedora.server.search.FieldSearchResult;
import fedora.server.storage.DOReader;
import fedora.server.storage.types.DatastreamDef;
import fedora.server.storage.types.MIMETypedStream;
import fedora.server.storage.types.MethodDef;
import fedora.server.storage.types.ObjectMethodsDef;
import fedora.server.storage.types.Property;

/**
 * The implementation of the Dynamic Access module.
 *
 * <p>The Dynamic Access module will associate dynamic disseminators with a 
 * digital object. It will look to the Fedora repository configuration file 
 * to obtain a list of dynamic disseminators.  Currently, the system supports 
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
public class DynamicAccessImpl {

    private final Access m_access;

    private final ServiceMethodDispatcher dispatcher;

    private File reposHomeDir = null;

    private Hashtable dynamicBDefToMech = null;

    public DynamicAccessImpl(Access m_access,
                             File reposHomeDir,
                             Hashtable dynamicBDefToMech) {
        dispatcher = new ServiceMethodDispatcher();
        this.m_access = m_access;
        this.reposHomeDir = reposHomeDir;
        this.dynamicBDefToMech = dynamicBDefToMech;
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
        // FIXIT! In FUTURE this method might consult some source that tells
        // what behavior definitions are appropriate to dynamically associate
        // with the object.  The rules for association might be based on the
        // context or based on something about the particular object (PID).
        // There is one rule that is always true - associate the Default
        // behavior definition with EVERY object. For now we will just take the
        // dynamic behavior definitions that were loaded by DynamicAccessModule.
        // NOTE: AT THIS TIME THERE THERE IS JUST ONE LOADED, NAMELY,
        // THE DEFAULT DISSEMINATOR BDEF (bDefPID = fedora-system:3)

        ArrayList bdefs = new ArrayList();
        Iterator iter = dynamicBDefToMech.keySet().iterator();
        while (iter.hasNext()) {
            bdefs.add(iter.next());
        }
        return (String[]) bdefs.toArray(new String[0]);
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
        Class mechClass = (Class) dynamicBDefToMech.get(bDefPID);
        if (mechClass != null) {
            try {
                Method method =
                        mechClass.getMethod("reflectMethods", (Class[]) null);
                return (MethodDef[]) method.invoke(null, (Object[]) null);
            } catch (Exception e) {
                throw new GeneralException("[DynamicAccessImpl] returned error when "
                        + "attempting to get dynamic behavior method definitions. "
                        + "The underlying error class was: "
                        + e.getClass().getName()
                        + ". The message "
                        + "was \""
                        + e.getMessage() + "\"");
            }
        }
        throw new MethodNotFoundException("[DynamicAccessImpl] The object, "
                + PID + " does not have the dynamic behavior definition "
                + bDefPID);
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
        return null;
    }

    private String getReposBaseURL(String protocol, String port) {
        String reposBaseURL = null;
        String fedoraServerHost =
                ((Module) m_access).getServer()
                        .getParameter("fedoraServerHost");
        reposBaseURL = protocol + "://" + fedoraServerHost + ":" + port;
        return reposBaseURL;
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
                                            Date asOfDateTime,
                                            DOReader reader)
            throws ServerException {
        if (bDefPID.equalsIgnoreCase("fedora-system:3")) {
            // FIXIT!! Use lookup to dynamicBDefToMech table to get class for
            // DefaultDisseminatorImpl and construct via Java reflection.

            String reposBaseURL =
                    getReposBaseURL(context
                                            .getEnvironmentValue(Constants.HTTP_REQUEST.SECURITY.uri)
                                            .equals(Constants.HTTP_REQUEST.SECURE.uri) ? "https"
                                            : "http",
                                    context
                                            .getEnvironmentValue(Constants.HTTP_REQUEST.SERVER_PORT.uri));

            Object result =
                    dispatcher
                            .invokeMethod(new DefaultDisseminatorImpl(context,
                                                                      asOfDateTime,
                                                                      reader,
                                                                      m_access,
                                                                      reposBaseURL,
                                                                      reposHomeDir),
                                          methodName,
                                          userParms);
            if (result
                    .getClass()
                    .getName()
                    .equalsIgnoreCase("fedora.server.storage.types.MIMETypedStream")) {
                return (MIMETypedStream) result;
            } else {
                throw new GeneralException("[DynamicAccessImpl] returned error. "
                        + "Internal service must return a MIME typed stream. "
                        + "(see fedora.server.storage.types.MIMETypedStream)");
            }
        } else {
            // FIXIT! (FUTURE) Open up the possibility of there being other
            // kinds of dynamic behaviors.  Use the bDefPID to locate the
            // appropriate mechanism for the dynamic behavior.  In future
            // we want the mechanism for a dynamic behavior defintion to
            // be able to be either an internal services, a local services,
            // or a distributed service.  We'll have to rework some things to
            // be able to see what kind of mechanism we have, and to do the
            // request dispatching appropriately.
        }
        return null;
    }

    /**
     * Get the definitions for all dynamic disseminations on the object. This
     * will return the method definitions for all methods for all of the dynamic
     * disseminators associated with the object.
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
        String[] bDefPIDs = getBehaviorDefinitions(context, PID, asOfDateTime);
        Date versDateTime = asOfDateTime;
        ArrayList objectMethods = new ArrayList();
        for (String element : bDefPIDs) {
            MethodDef[] methodDefs =
                    getBehaviorMethods(context, PID, element, asOfDateTime);
            for (MethodDef element2 : methodDefs) {
                ObjectMethodsDef method = new ObjectMethodsDef();
                method.PID = PID;
                method.asOfDate = versDateTime;
                method.bDefPID = element;
                method.methodName = element2.methodName;
                method.methodParmDefs = element2.methodParms;
                objectMethods.add(method);
            }
        }
        return (ObjectMethodsDef[]) objectMethods
                .toArray(new ObjectMethodsDef[0]);
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
        // FIXIT! Return something here.
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
        return null;
    }

    public boolean isDynamicBehaviorDefinition(Context context,
                                               String PID,
                                               String bDefPID)
            throws ServerException {
        if (dynamicBDefToMech.containsKey(bDefPID)) {
            return true;
        }
        return false;
    }

    // FIXIT: What do these mean in this context...anything?
    // Maybe these methods' exposure needs to be re-thought?
    public MIMETypedStream getDatastreamDissemination(Context context,
                                                      String PID,
                                                      String dsID,
                                                      Date asOfDateTime)
            throws ServerException {
        return null;
    }

    public DatastreamDef[] listDatastreams(Context context,
                                           String PID,
                                           Date asOfDateTime)
            throws ServerException {
        return null;
    }
}
