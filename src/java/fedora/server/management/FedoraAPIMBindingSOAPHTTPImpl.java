package fedora.server.management;

import fedora.server.ReadOnlyContext;
import fedora.server.Server;
import fedora.server.errors.InitializationException;
import fedora.server.errors.ObjectIntegrityException;
import fedora.server.errors.ServerException;
import fedora.server.errors.ServerInitializationException;
import fedora.server.errors.StorageDeviceException;
import fedora.server.management.Management;
import fedora.server.storage.DefinitiveDOWriter;
import fedora.server.storage.METSDOSerializer;
import fedora.server.storage.METSDODeserializer;
import fedora.server.storage.TestFileStreamStorage;
import fedora.server.storage.lowlevel.ILowlevelStorage;
import fedora.server.storage.lowlevel.FileSystemLowlevelStorage;
import fedora.server.types.gen.ObjectInfo;
import fedora.server.utilities.AxisUtility;

import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Iterator;
import org.apache.axis.AxisEngine;
import org.apache.axis.MessageContext;

public class FedoraAPIMBindingSOAPHTTPImpl 
        implements FedoraAPIM {

    /** The Fedora Server instance */
    private static Server s_server;

    /** Whether the service has initialized... true if we got a good Server instance. */
    private static boolean s_initialized;

    /** The exception indicating that initialization failed. */
    private static InitializationException s_initException;

    private static Management s_management;

    /** This is a temporary hack -- normally DOManager provides these */
    private static DefinitiveDOWriter w;

    private static ILowlevelStorage s_st;

    private static ReadOnlyContext s_context;
    
    /** Before fulfilling any requests, make sure we have a server instance. */
    static {
        try {
            String fedoraHome=System.getProperty("fedora.home");
            if (fedoraHome==null) {
                s_initialized=false;
                s_initException=new ServerInitializationException(
                    "Server failed to initialize: The 'fedora.home' "
                    + "system property was not set.");
            } else {
                s_server=Server.getInstance(new File(fedoraHome));
                s_initialized=true;
                s_management=(Management) s_server.getModule("fedora.server.management.Management");
                HashMap h=new HashMap();
                h.put("application", "apim");
                h.put("useCachedObject", "false");
                h.put("userId", "fedoraAdmin");
                s_context=new ReadOnlyContext(h);
            }
            s_st=FileSystemLowlevelStorage.getPermanentStore();  // FIXME: Move this
        } catch (InitializationException ie) {
            System.err.println(ie.getMessage());
            s_initialized=false;
            s_initException=ie;
        }
    }
    
    public String createObject() 
            throws RemoteException {
        assertInitialized();
        try {
            return s_management.createObject(s_context);
        } catch (ServerException se) {
            logStackTrace(se);
            throw AxisUtility.getFault(se);
        }
    }
    
    private void logStackTrace(Exception e) {
        StackTraceElement[] els=e.getStackTrace();
        StringBuffer lines=new StringBuffer();
        boolean skip=false;
        for (int i=0; i<els.length; i++) {
            if (els[i].toString().indexOf("FedoraAPIMBindingSOAPHTTPSkeleton")!=-1) {
                skip=true;
            }
            if (!skip) {
                lines.append(els[i].toString());
                lines.append("\n");
            }
        }
        s_server.logFiner("Error carried up to API-M level: " + e.getClass().getName() + "\n" + lines.toString());
    }

    public String ingestObject(byte[] METSXML) throws java.rmi.RemoteException {
        assertInitialized();
        try {
            return s_management.ingestObject(s_context, 
                    new ByteArrayInputStream(METSXML), "mets11fedora1", "UTF-8", true); // always gens pid, unless pid in stream starts with "test:"
        } catch (ServerException se) {
            logStackTrace(se);
            throw AxisUtility.getFault(se);
        }
/*        try {
            String pid="1234";
            TestFileStreamStorage st=new TestFileStreamStorage(new File(s_server.getHomeDir(), "data"), 4096);
            METSDOSerializer ser=new METSDOSerializer("UTF-8");
            METSDODeserializer deser=new METSDODeserializer("UTF-8", false, METSDODeserializer.QUERY_NEVER); // don't check if it's well-formed xml
            w=new DefinitiveDOWriter(pid, st, st, null, 
                    deser, ser, deser, ser, new ByteArrayInputStream(METSXML),
                    true);
            return w.GetObjectPID();
        } catch (ServerException se) {
            AxisUtility.throwFault(se);
        } catch (Exception e) {
            AxisUtility.throwFault(new ServerInitializationException(e.getClass().getName() + ": " + e.getMessage()));
        } */
       /* 
    public DefinitiveDOWriter(String pid, TestStreamStorage storage, 
            TestStreamStorage tempStorage, StreamValidator validator,
            DODeserializer importDeserializer, DOSerializer storageSerializer,
            DODeserializer storageDeserializer, DOSerializer exportSerializer,
            InputStream initialContent, boolean useContentPid) 
            throws ObjectIntegrityException, 
            StreamIOException, StreamReadException {        
        */    
            
    }

    public byte[] getObjectXML(String PID) 
            throws RemoteException {
        assertInitialized();
        try {
            InputStream in=s_management.getObjectXML(s_context, PID, "mets11fedora1", "UTF-8");
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            pipeStream(in, out);
            return out.toByteArray();
        } catch (ServerException se) {
            throw AxisUtility.getFault(se);
        } catch (Exception e) {
            throw AxisUtility.getFault(new ServerInitializationException(e.getClass().getName() + ": " + e.getMessage()));
        }
    }

    // temporarily here
    private void pipeStream(InputStream in, OutputStream out) 
            throws StorageDeviceException {
        try {
            byte[] buf = new byte[4096];
            int len;
            while ( ( len = in.read( buf ) ) != -1 ) {
                out.write( buf, 0, len );
            }
        } catch (IOException ioe) {
            throw new StorageDeviceException("Error writing to stream");
        } finally {
            try {
                out.close();
                in.close();
            } catch (IOException closeProb) {
              // ignore problems while closing
            }
        }
    }

    public byte[] exportObject(String PID) throws java.rmi.RemoteException {
        assertInitialized();
        try {
            InputStream in=w.ExportObject();
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            pipeStream(in, out);
            return out.toByteArray();
        } catch (ServerException se) {
            logStackTrace(se);
            AxisUtility.throwFault(se);
        } catch (Exception e) {
            logStackTrace(e);
            AxisUtility.throwFault(new ServerInitializationException(e.getClass().getName() + ": " + e.getMessage()));
        }
        return null;
    }

    public void withdrawObject(String PID, String logMessage) throws java.rmi.RemoteException {
        assertInitialized();
    }

    public void deleteObject(String PID, String logMessage) throws java.rmi.RemoteException {
        assertInitialized();
    }

    public void purgeObject(String PID, String logMessage) throws java.rmi.RemoteException {
        assertInitialized();
        try {
            s_management.purgeObject(s_context, PID, logMessage);
        } catch (ServerException se) {
            logStackTrace(se);
            AxisUtility.throwFault(se);
        }
    }

    public void obtainLock(String PID) throws java.rmi.RemoteException {
        assertInitialized();
        try {
            ByteArrayInputStream testInputStream=new ByteArrayInputStream(PID.getBytes());
            s_st.add(PID, testInputStream);
        } catch (ServerException se) {
            logStackTrace(se);
            AxisUtility.throwFault(se);
        }
    }

    public void releaseLock(String PID, String logMessage, boolean commit) throws java.rmi.RemoteException {
        assertInitialized();
/* needs rewrite to work with new "Management" instance
        try {
            if (commit=false) {
                w.rollBack();
            } else {
                w.commit(logMessage);
            }
        } catch (ServerException se) {
            logStackTrace(se);
            AxisUtility.throwFault(se);
        } catch (Exception e) {
            logStackTrace(e);
            AxisUtility.throwFault(new ServerInitializationException(e.getClass().getName() + ": " + e.getMessage()));
        }
*/
    }
    
    public ObjectInfo getObjectInfo(String PID) 
            throws RemoteException {
        return null;
    }

    public fedora.server.types.gen.AuditRecord[] getObjectAuditTrail(String PID) throws java.rmi.RemoteException {
        assertInitialized();
        return null;
    }

    public String[] listObjectPIDs(String foType) throws java.rmi.RemoteException {
        assertInitialized();
        try {
            return s_management.listObjectPIDs(s_context, foType);
        } catch (ServerException se) {
            logStackTrace(se);
            throw AxisUtility.getFault(se);
        }
    }

    public String addDatastreamExternal(String PID, String dsLabel, String dsLocation) throws java.rmi.RemoteException {
        assertInitialized();
        return null;
    }

    public String addDatastreamManagedContent(String PID, String dsLabel, String MIMEType, byte[] dsContent) throws java.rmi.RemoteException {
        assertInitialized();
        return null;
    }

    public String addDatastreamXMLMetadata(String PID, String dsLabel, String MDType, byte[] dsInlineMetadata) throws java.rmi.RemoteException {
        assertInitialized();
        return null;
    }

    public void modifyDatastreamExternal(String PID, String datastreamID, String dsLabel, String dsLocation) throws java.rmi.RemoteException {
        assertInitialized();
    }

    public void modifyDatastreamManagedContent(String PID, String datastreamID, String dsLabel, String MIMEType, byte[] dsContent) throws java.rmi.RemoteException {
        assertInitialized();
    }

    public void modifyDatastreamXMLMetadata(String PID, String datastreamID, String dsLabel, String MDType, byte[] dsInlineMetadata) throws java.rmi.RemoteException {
        assertInitialized();
    }

    public void withdrawDatastream(String PID, String datastreamID) throws java.rmi.RemoteException {
        assertInitialized();
    }

    public void withdrawDisseminator(String PID, String disseminatorID) throws java.rmi.RemoteException {
        assertInitialized();
    }

    public void deleteDatastream(String PID, String datastreamID) throws java.rmi.RemoteException {
        assertInitialized();
    }

    public java.util.Calendar[] purgeDatastream(String PID, String datastreamID, java.util.Calendar startDT, java.util.Calendar endDT) throws java.rmi.RemoteException {
        assertInitialized();
        return null;
    }

    public fedora.server.types.gen.Datastream getDatastream(String PID, String datastreamID, java.util.Calendar asOfDateTime) throws java.rmi.RemoteException {
        assertInitialized();
        return null;
    }

    public fedora.server.types.gen.Datastream[] getDatastreams(String PID, java.util.Calendar asOfDateTime) throws java.rmi.RemoteException {
        assertInitialized();
        return null;
    }

    public String[] listDatastreamIDs(String PID, String state) throws java.rmi.RemoteException {
        assertInitialized();
        try {
            return w.ListDatastreamIDs(state);
        } catch (ServerException se) {
            logStackTrace(se);
            AxisUtility.throwFault(se);
        } catch (Exception e) {
            logStackTrace(e);
            AxisUtility.throwFault(new ServerInitializationException(e.getClass().getName() + ": " + e.getMessage()));
        }
        return null;
    }

    public fedora.server.types.gen.ComponentInfo[] getDatastreamHistory(String PID, String datastreamID) throws java.rmi.RemoteException {
        assertInitialized();
        return null;
    }

    public String addDisseminator(String PID, String bMechPID, String dissLabel, fedora.server.types.gen.DatastreamBindingMap bindingMap) throws java.rmi.RemoteException {
        assertInitialized();
        return null;
    }

    public void modifyDisseminator(String PID, String disseminatorID, String bMechPID, String dissLabel, fedora.server.types.gen.DatastreamBindingMap bindingMap) throws java.rmi.RemoteException {
        assertInitialized();
    }

    public void deleteDisseminator(String PID, String disseminatorID) throws java.rmi.RemoteException {
        assertInitialized();
    }

    public java.util.Calendar[] purgeDisseminator(String PID, String disseminatorID, java.util.Calendar startDT, java.util.Calendar endDT) throws java.rmi.RemoteException {
        assertInitialized();
        return null;
    }

    public fedora.server.types.gen.Disseminator getDisseminator(String PID, String disseminatorID, java.util.Calendar asOfDateTime) throws java.rmi.RemoteException {
        assertInitialized();
        return null;
    }

    public fedora.server.types.gen.Disseminator[] getDisseminators(String PID, java.util.Calendar asOfDateTime) throws java.rmi.RemoteException {
        assertInitialized();
        return null;
    }

    public String[] listDisseminatorIDs(String PID, String state) throws java.rmi.RemoteException {
        assertInitialized();
        return null;
    }

    public fedora.server.types.gen.ComponentInfo[] getDisseminatorHistory(String PID, String disseminatorID) throws java.rmi.RemoteException {
        assertInitialized();
        return null;
    }
    
    private void assertInitialized()
            throws java.rmi.RemoteException {
        if (!s_initialized) {
            AxisUtility.throwFault(s_initException);
        }
    }

}
