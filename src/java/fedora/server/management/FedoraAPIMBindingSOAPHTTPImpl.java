package fedora.server.management;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.rmi.RemoteException;
import java.util.Date;

import org.apache.axis.types.NonNegativeInteger;

import org.apache.log4j.Logger;

import fedora.server.Context;
import fedora.server.ReadOnlyContext;
import fedora.server.Server;
import fedora.server.errors.GeneralException;
import fedora.server.errors.InitializationException;
import fedora.server.errors.ServerException;
import fedora.server.errors.ServerInitializationException;
import fedora.server.errors.StorageDeviceException;
import fedora.server.errors.authorization.AuthzException;
import fedora.server.utilities.AxisUtility;
import fedora.server.utilities.DateUtility;
import fedora.server.utilities.TypeUtility;

/**
 *
 * <p><b>Title:</b> FedoraAPIMBindingSOAPHTTPImpl.java</p>
 * <p><b>Description:</b> </p>
 *
 * @author cwilper@cs.cornell.edu
 * @version $Id$
 */
public class FedoraAPIMBindingSOAPHTTPImpl
        implements FedoraAPIM {

    /** Logger for this class. */
	private static final Logger LOG = Logger.getLogger(
	        FedoraAPIMBindingSOAPHTTPImpl.class);
	
    /** The Fedora Server instance */
    private static Server s_server;

    /** Whether the service has initialized... true if we got a good Server instance. */
    private static boolean s_initialized;

    /** The exception indicating that initialization failed. */
    private static InitializationException s_initException;

    private static Management s_management;

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
            }
        } catch (InitializationException ie) {
            LOG.error("Error getting server", ie);
            s_initialized=false;
            s_initException=ie;
        }
    }

    /**
     * @deprecated This remains in Fedora 2.0 for backward compatibility. 
     *    It assumes METS-Fedora as the ingest format.
     *    It will be removed in a future version.
     *    Replaced by {@link #ingest(byte[], String, String)}
     */
    public String ingestObject(byte[] METSXML, String logMessage) throws java.rmi.RemoteException {
        assertInitialized();
		return ingest(METSXML, "metslikefedora1", logMessage);
    }
    
	public String ingest(byte[] XML, String format, String logMessage) throws java.rmi.RemoteException {
		LOG.debug("start: ingest");
		assertInitialized();
		try {
		  // always gens pid, unless pid in stream starts with "test:" "demo:"
		  // or other prefix that is configured in the retainPIDs parameter of fedora.fcfg
			return s_management.ingestObject(ReadOnlyContext.getSoapContext(),
					new ByteArrayInputStream(XML), logMessage, format, "UTF-8", true);
        } catch (Throwable th) {
            LOG.error("Error ingesting", th);
            throw AxisUtility.getFault(th); 			
		} finally {
			LOG.debug("end: ingest");
		}
	}

    public String modifyObject(String PID, String state, String label,
            String logMessage)
            throws RemoteException {
    	LOG.debug("start: modifyObject, " + PID);
        assertInitialized();
        try {
            return DateUtility.convertDateToString(
                    s_management.modifyObject(
                            ReadOnlyContext.getSoapContext(), PID, state, label, logMessage));
        } catch (Throwable th) {
            LOG.error("Error modifying object", th);
            throw AxisUtility.getFault(th); 			
        } finally {
        	LOG.debug("end: modifyObject, " + PID);
        }
    }

	public fedora.server.types.gen.Property[] getObjectProperties(String PID)
			throws RemoteException {
		assertInitialized();
		try {
			fedora.server.storage.types.Property[] properties=
					s_management.getObjectProperties(
							ReadOnlyContext.getSoapContext(), PID);
			return TypeUtility.convertPropertyArrayToGenPropertyArray(properties);
        } catch (Throwable th) {
            LOG.error("Error getting object properties", th);
            throw AxisUtility.getFault(th); 			
        }
	}

    public fedora.server.types.gen.UserInfo describeUser(String id)
            throws RemoteException {
    	Context context = ReadOnlyContext.getSoapContext();
    	try {
			s_management.adminPing(context);
        } catch (Throwable th) {
            LOG.error("Error getting user info", th);
            throw AxisUtility.getFault(th); 			
        }
        fedora.server.types.gen.UserInfo inf=new fedora.server.types.gen.UserInfo();
        inf.setId(id);
        //so, for the purposes of this method, an administrator is whoever is permitted action "adminPing"
        //and only administrators can be "described"
        inf.setAdministrator(true);
        return inf;
    }


    public byte[] getObjectXML(String PID)
            throws RemoteException {
        assertInitialized();
        try {
            InputStream in=s_management.getObjectXML(ReadOnlyContext.getSoapContext(), PID, "UTF-8");
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            pipeStream(in, out);
            return out.toByteArray();
        } catch (Throwable th) {
            LOG.error("Error getting object XML", th);
            throw AxisUtility.getFault(th); 			
        }
    }

    public byte[] exportObject(String PID)
            throws RemoteException {
        assertInitialized();
        try {
			InputStream in=s_management.exportObject(ReadOnlyContext.getSoapContext(), PID, null, null, "UTF-8");
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            pipeStream(in, out);
            return out.toByteArray();
        } catch (Throwable th) {
            LOG.error("Error exporting object", th);
            throw AxisUtility.getFault(th); 			
        }
    }
    
	public byte[] export(String PID, String format, String exportContext)
			throws RemoteException {
		assertInitialized();
		try {
			InputStream in=s_management.exportObject(ReadOnlyContext.getSoapContext(), PID, format, exportContext, "UTF-8");
			ByteArrayOutputStream out=new ByteArrayOutputStream();
			pipeStream(in, out);
			return out.toByteArray();
        } catch (Throwable th) {
            LOG.error("Error exporting object", th);
            throw AxisUtility.getFault(th); 			
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

    public String purgeObject(String PID, 
                              String logMessage,
                              boolean force) throws java.rmi.RemoteException {
    	LOG.debug("start: purgeObject, " + PID);
        assertInitialized();
        try {
            return DateUtility.convertDateToString(
                    s_management.purgeObject(ReadOnlyContext.getSoapContext(), 
                                             PID, 
                                             logMessage,
                                             force));
        } catch (Throwable th) {
            LOG.error("Error purging object", th);
            throw AxisUtility.getFault(th); 			
        } finally {
        	LOG.debug("end: purgeObject, " + PID);
        }
    }

    public String addDatastream(String pid,
                                String dsID,
                                String[] altIds,
                                String label,
                                boolean versionable,
                                String MIMEType,
                                String formatURI,
                                String location,
                                String controlGroup,
                                String dsState,
                                String checksumType,
                                String checksum,
                                String logMessage) throws RemoteException {
    	LOG.debug("start: addDatastream, " + pid + ", " + dsID);
        assertInitialized();
        try {
            return s_management.addDatastream(ReadOnlyContext.getSoapContext(), 
                                                 pid, 
                                                 dsID,
                                                 altIds,
                                                 label, 
                                                 versionable,
                                                 MIMEType,
                                                 formatURI,
                                                 location,
                                                 controlGroup,
                                                 dsState,
                                                 checksumType,
                                                 checksum,
                                                 logMessage);
        } catch (Throwable th) {
            LOG.error("Error adding datastream", th);
            throw AxisUtility.getFault(th); 			
        } finally {
        	LOG.debug("end: addDatastream, " + pid + ", " + dsID);
        }
    }

    public String modifyDatastreamByReference(String PID, 
                                              String datastreamID,
                                              String[] altIDs,
                                              String dsLabel, 
                                              String mimeType,
                                              String formatURI,
                                              String dsLocation, 
                                              String checksumType,
                                              String checksum,
                                              String logMessage, 
                                              boolean force)
            throws java.rmi.RemoteException {
    	LOG.debug("start: modifyDatastreamByReference, " + PID + ", " + datastreamID);
        assertInitialized();
        try {
            return DateUtility.convertDateToString(
                    s_management.modifyDatastreamByReference(
                            ReadOnlyContext.getSoapContext(), 
                            PID,
                            datastreamID, 
                            altIDs,
                            dsLabel, 
                            mimeType,
                            formatURI,
                            dsLocation, 
                            checksumType,
                            checksum,
                            logMessage, 
                            force));
        } catch (Throwable th) {
            LOG.error("Error modifying datastream by reference", th);
            throw AxisUtility.getFault(th); 			
        } finally {
        	LOG.debug("end: modifyDatastreamByReference, " + PID + ", " + datastreamID);
        }
    }

    public String modifyDatastreamByValue(String PID, 
                                          String datastreamID, 
                                          String[] altIDs,
                                          String dsLabel, 
                                          String mimeType,
                                          String formatURI,
                                          byte[] dsContent, 
                                          String checksumType,
                                          String checksum,
                                          String logMessage, 
                                          boolean force) 
                throws java.rmi.RemoteException {
    	LOG.debug("start: modifyDatastreamByValue, " + PID + ", " + datastreamID);
        assertInitialized();
        try {
            ByteArrayInputStream byteStream = null;
            if (dsContent!=null && dsContent.length>0) {
                byteStream = new ByteArrayInputStream(dsContent);
            }
            return DateUtility.convertDateToString(
                    s_management.modifyDatastreamByValue(ReadOnlyContext.getSoapContext(), 
                                                         PID,
                                                         datastreamID, 
                                                         altIDs,
                                                         dsLabel, 
                                                         mimeType,
                                                         formatURI,
                                                         byteStream, 
                                                         checksumType,
                                                         checksum,
                                                         logMessage, 
                                                         force));
        } catch (Throwable th) {
            LOG.error("Error modifying datastream by value", th);
            throw AxisUtility.getFault(th); 			
        } finally {
        	LOG.debug("end: modifyDatastreamByValue, " + PID + ", " + datastreamID);
        }
    }

    public String setDatastreamState(String PID, 
                                     String datastreamID, 
                                     String dsState, 
                                     String logMessage) throws java.rmi.RemoteException {
        assertInitialized();
        try {
            return DateUtility.convertDateToString(
                    s_management.setDatastreamState(ReadOnlyContext.getSoapContext(), 
                                                    PID,
                                                    datastreamID, 
                                                    dsState, 
                                                    logMessage));
        } catch (Throwable th) {
            LOG.error("Error setting datastream state", th);
            throw AxisUtility.getFault(th); 			
        }
    }

    public String setDatastreamVersionable(String PID, 
                                            String datastreamID, 
                                            boolean versionable, 
                                            String logMessage) throws java.rmi.RemoteException 
    {
        assertInitialized();
        try {
        return DateUtility.convertDateToString(
            s_management.setDatastreamVersionable(ReadOnlyContext.getSoapContext(), 
                                                   PID,
                                                   datastreamID, 
                                                   versionable, 
                                                   logMessage));
        } catch (Throwable th) {
            LOG.error("Error setting datastream versionable", th);
            throw AxisUtility.getFault(th); 			
        }
    }
    
    public String setDatastreamChecksum(String PID, 
                                        String datastreamID, 
                                        String algorithm) throws java.rmi.RemoteException 
    {
        assertInitialized();
        try {
            return s_management.setDatastreamChecksum(ReadOnlyContext.getSoapContext(), 
                                                       PID,
                                                       datastreamID, 
                                                       algorithm);
        } catch (Throwable th) {
            LOG.error("Error setting datastream checksum", th);
            throw AxisUtility.getFault(th); 			
        }
    }

    public String compareDatastreamChecksum(String PID, 
                                            String datastreamID, 
                                            String versionDate) throws java.rmi.RemoteException 
    {
        assertInitialized();
        try {
            return s_management.compareDatastreamChecksum(ReadOnlyContext.getSoapContext(), 
                                                           PID,
                                                           datastreamID, 
                                                           versionDate);
        } catch (Throwable th) {
            LOG.error("Error comparing datastream checksum", th);
            throw AxisUtility.getFault(th); 			
        }
    }


    public String setDisseminatorState(String PID, 
                                       String disseminatorID, 
                                       String dissState, 
                                       String logMessage) throws java.rmi.RemoteException {
        assertInitialized();
        try {
            return DateUtility.convertDateToString(
                    s_management.setDisseminatorState(ReadOnlyContext.getSoapContext(), 
                                                      PID,
                                                      disseminatorID, 
                                                      dissState, 
                                                      logMessage));
        } catch (Throwable th) {
            LOG.error("Error setting disseminator state", th);
            throw AxisUtility.getFault(th); 			
        }
    }

    public String[] purgeDatastream(String PID, 
                                    String datastreamID, 
                                    String startDT,
                                    String endDT,
                                    String logMessage,
                                    boolean force) 
            throws java.rmi.RemoteException {
    	LOG.debug("start: purgeDatastream, " + PID + ", " + datastreamID);
        assertInitialized();
        try {
            return toStringArray(
                    s_management.purgeDatastream(
                            ReadOnlyContext.getSoapContext(), 
                            PID, 
                            datastreamID,
                            DateUtility.convertStringToDate(startDT),
                            DateUtility.convertStringToDate(endDT),
                            logMessage,
                            force));
        } catch (Throwable th) {
            LOG.error("Error purging datastream", th);
            throw AxisUtility.getFault(th); 			
        } finally {
        	LOG.debug("end: purgeDatastream, " + PID + ", " + datastreamID);
        }
    }

    private String[] toStringArray(Date[] dates) throws Exception {
        String[] out = new String[dates.length];
        for (int i = 0; i < dates.length; i++) {
            out[i] = DateUtility.convertDateToString(dates[i]);
        }
        return out;
    }

    public fedora.server.types.gen.Datastream getDatastream(String PID, 
                                                            String datastreamID, 
                                                            String asOfDateTime) 
            throws java.rmi.RemoteException {
        assertInitialized();
        try {
            fedora.server.storage.types.Datastream ds=
                    s_management.getDatastream(
                            ReadOnlyContext.getSoapContext(), 
                            PID, 
                            datastreamID, 
                            DateUtility.convertStringToDate(asOfDateTime));
            return TypeUtility.convertDatastreamToGenDatastream(ds);
        } catch (Throwable th) {
            LOG.error("Error getting datastream", th);
            throw AxisUtility.getFault(th); 			
        }
    }

    public fedora.server.types.gen.Datastream[] getDatastreams(String PID, 
                                                               String asOfDateTime, 
                                                               String state) 
            throws java.rmi.RemoteException {
        assertInitialized();
        try {
            fedora.server.storage.types.Datastream[] intDatastreams=
                    s_management.getDatastreams(
                            ReadOnlyContext.getSoapContext(), 
                            PID, 
                            DateUtility.convertStringToDate(asOfDateTime),
                            state);
            return getGenDatastreams(intDatastreams);
        } catch (Throwable th) {
            LOG.error("Error getting datastreams", th);
            throw AxisUtility.getFault(th); 			
        }
    }

    private fedora.server.types.gen.Datastream[] getGenDatastreams(
            fedora.server.storage.types.Datastream[] intDatastreams) {
        fedora.server.types.gen.Datastream[] genDatastreams=
                new fedora.server.types.gen.Datastream[intDatastreams.length];
    	for (int i=0; i<intDatastreams.length; i++) {
    	    genDatastreams[i]=TypeUtility.convertDatastreamToGenDatastream(
   		        intDatastreams[i]);
		}
        return genDatastreams;
    }

    private fedora.server.types.gen.Disseminator[] getGenDisseminators(
            fedora.server.storage.types.Disseminator[] intDisseminators) {
        fedora.server.types.gen.Disseminator[] genDisseminators=
                new fedora.server.types.gen.Disseminator[intDisseminators.length];
            for (int i=0; i<intDisseminators.length; i++) {
                genDisseminators[i]=TypeUtility.convertDisseminatorToGenDisseminator(
                           intDisseminators[i]);
                }
        return genDisseminators;
    }

    public fedora.server.types.gen.Datastream[] getDatastreamHistory(String PID, String datastreamID)
            throws java.rmi.RemoteException {
        assertInitialized();
        try {
            fedora.server.storage.types.Datastream[] intDatastreams=
                    s_management.getDatastreamHistory(
                            ReadOnlyContext.getSoapContext(),
                            PID,
                            datastreamID);
            return getGenDatastreams(intDatastreams);
        } catch (Throwable th) {
            LOG.error("Error getting datastream history", th);
            throw AxisUtility.getFault(th); 			
        }
    }

	public String addDisseminator(String PID, 
	                              String bDefPID, 
	                              String bMechPID, 
	                              String dissLabel,  
	                              fedora.server.types.gen.DatastreamBindingMap bindingMap, 
	                              String dissState,
	                              String logMessage) 
            throws java.rmi.RemoteException {
		assertInitialized();
		try {
			return s_management.addDisseminator(
			        ReadOnlyContext.getSoapContext(), 
		            PID, 
	                bDefPID, 
                    bMechPID, 
                    dissLabel, 
                    TypeUtility.convertGenDatastreamBindingMapToDSBindingMap(bindingMap), 
                    dissState,
                    logMessage);
        } catch (Throwable th) {
            LOG.error("Error adding disseminator", th);
            throw AxisUtility.getFault(th); 			
        }
    }

    public String[] purgeDisseminator(String PID,
                                      String disseminatorID, 
                                      String endDT,
                                      String logMessage)
            throws java.rmi.RemoteException {
        assertInitialized();
        try {
            return toStringArray(
                    s_management.purgeDisseminator(
                            ReadOnlyContext.getSoapContext(), 
                            PID, 
                            disseminatorID, 
                            DateUtility.convertStringToDate(endDT),
                            logMessage));
        } catch (Throwable th) {
            LOG.error("Error purging disseminator", th);
            throw AxisUtility.getFault(th); 			
        }
    }

    public fedora.server.types.gen.Disseminator[] getDisseminatorHistory(String PID, 
                                                                         String disseminatorID) 
              throws java.rmi.RemoteException {
      assertInitialized();
      try {
          fedora.server.storage.types.Disseminator[] intDisseminators=
                  s_management.getDisseminatorHistory(
                          ReadOnlyContext.getSoapContext(),
                          PID,
                          disseminatorID);
          return getGenDisseminators(intDisseminators);
      } catch (Throwable th) {
          LOG.error("Error getting disseminator history", th);
          throw AxisUtility.getFault(th); 			
      }
   }

    public fedora.server.types.gen.Disseminator getDisseminator(String PID, 
                                                                String disseminatorID, 
                                                                String asOfDateTime) 
            throws java.rmi.RemoteException {
        assertInitialized();
        try {
            fedora.server.storage.types.Disseminator diss=
                    s_management.getDisseminator(
                            ReadOnlyContext.getSoapContext(), 
                            PID, 
                            disseminatorID, 
                            DateUtility.convertStringToDate(asOfDateTime));
            return TypeUtility.convertDisseminatorToGenDisseminator(diss);
        } catch (Throwable th) {
            LOG.error("Error getting disseminator", th);
            throw AxisUtility.getFault(th); 			
        }
    }

    public fedora.server.types.gen.Disseminator[] getDisseminators(String PID, 
                                                                   String asOfDateTime, 
                                                                   String dissState) 
              throws java.rmi.RemoteException {
        assertInitialized();
        try {
            fedora.server.storage.types.Disseminator[] intDisseminators=
                    s_management.getDisseminators(
                            ReadOnlyContext.getSoapContext(), 
                            PID, 
                            DateUtility.convertStringToDate(asOfDateTime),
                            dissState);
            return getGenDisseminators(intDisseminators);
        } catch (Throwable th) {
            LOG.error("Error getting disseminators", th);
            throw AxisUtility.getFault(th); 			
        }
    }

    public String modifyDisseminator(String PID, 
                                     String disseminatorID, 
                                     String bMechPID, 
                                     String dissLabel,  
                                     fedora.server.types.gen.DatastreamBindingMap bindingMap, 
                                     String dissState,
                                     String logMessage,
                                     boolean force)
            throws java.rmi.RemoteException {
        assertInitialized();
        try {
            return DateUtility.convertDateToString(
                    s_management.modifyDisseminator(
                            ReadOnlyContext.getSoapContext(), 
                            PID,
                            disseminatorID, 
                            bMechPID, 
                            dissLabel, 
                            TypeUtility.convertGenDatastreamBindingMapToDSBindingMap(bindingMap),
                            dissState,
                            logMessage,
                            force));
        } catch (Throwable th) {
            LOG.error("Error modifying disseminator", th);
            throw AxisUtility.getFault(th); 			
        }
    }

    public java.lang.String[] getNextPID(NonNegativeInteger numPIDs,
            String namespace)
            throws java.rmi.RemoteException {
    	LOG.debug("start: getNextPID");
        assertInitialized();
        try {
            if(numPIDs==null) numPIDs=new NonNegativeInteger("1");
            return s_management.getNextPID(ReadOnlyContext.getSoapContext(), numPIDs.intValue(), namespace);
        } catch (Throwable th) {
            LOG.error("Error getting next PID", th);
            throw AxisUtility.getFault(th); 			
        } finally {
        	LOG.debug("end: getNextPID");
        }
    }


    private void assertInitialized()
            throws java.rmi.RemoteException {
        if (!s_initialized) {
            AxisUtility.throwFault(s_initException);
        }
    }

}
