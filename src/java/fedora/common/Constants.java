package fedora.common;

import fedora.common.policy.*;
import fedora.common.rdf.*;

/**
 * Constants of general utility.
 *
 */ 
public interface Constants {
    public static final FedoraNamespace         FEDORA        = new FedoraNamespace();  

    /**
     * The base directory of the Fedora installation.
     *
     * This is normally determined by the FEDORA_HOME environment variable,
     * but if defined, the <code>fedora.home</code> system property will be
     * used instead.
     */
    public static final String FEDORA_HOME = FedoraHome.getValue();

    /** The PID of the Fedora system definition object. */
    public static final PID    FEDORA_SYSTEM_DEF_PID = PID.getInstance("fedora-system:def");
    public static final String FEDORA_SYSTEM_DEF_URI = FEDORA_SYSTEM_DEF_PID.toURI();

    /* rdf namespaces */
    public static final DublinCoreNamespace     DC            = new DublinCoreNamespace();
    public static final FedoraModelNamespace    MODEL         = new FedoraModelNamespace();
    public static final FedoraRelsExtNamespace  RELS_EXT      = new FedoraRelsExtNamespace();
    public static final FedoraViewNamespace     VIEW          = new FedoraViewNamespace();
    public static final RecoveryNamespace       RECOVERY      = new RecoveryNamespace();
    public static final RDFSyntaxNamespace      RDF           = new RDFSyntaxNamespace();
    public static final TucanaNamespace         TUCANA        = new TucanaNamespace();
    public static final XSDNamespace            XSD           = new XSDNamespace();
    
    /* policy namespaces */
    public static final SubjectNamespace        SUBJECT       = SubjectNamespace.getInstance();
    public static final ActionNamespace         ACTION        = ActionNamespace.getInstance();
    public static final ResourceNamespace       RESOURCE      = ResourceNamespace.getInstance();
    public static final ObjectNamespace         OBJECT        = ObjectNamespace.getInstance();
    public static final DatastreamNamespace     DATASTREAM    = DatastreamNamespace.getInstance();
    public static final DisseminatorNamespace   DISSEMINATOR  = DisseminatorNamespace.getInstance();
    public static final BDefNamespace           BDEF          = BDefNamespace.getInstance();
    public static final BMechNamespace          BMECH         = BMechNamespace.getInstance();    
    public static final EnvironmentNamespace    ENVIRONMENT   = EnvironmentNamespace.getInstance();
    public static final HttpRequestNamespace    HTTP_REQUEST  = HttpRequestNamespace.getInstance();

    static class FedoraHome {
        public static final String getValue() {
            if (System.getProperty("fedora.home") != null) {
                return System.getProperty("fedora.home");
            } else {
                return System.getenv("FEDORA_HOME");
            }
        }
    }

}
