package fedora.server.management;

import java.io.*;
import java.math.*;
import java.util.*;
import java.text.*;

/**
 *
 * <p><b>Title:</b> PIDGeneration.java</p>
 * <p><b>Description:</b> Provides a mechanism to generate Persistent
 * Identifiers (PIDs).  A PID uniquely identifies an object within Fedora.</p>
 *
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2002, 2003 by The
 * Rector and Visitors of the University of Virginia and Cornell University.
 * All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 *
 * @author Paul Charlton
 * @version $Id$
 */
public class PIDGeneration implements PIDGenerator {
        private Class classRef;
   private static String logsDirectory;
        private static BigInteger lastObjectID;

   /**
   * <p>
   * Constructor -  the class reference is retrieved for synchronization.  In order
   * to ensure file and data structure integrity, only one thread per class
   * (class, not object) can run at a time.
   *   When the first object is instantiated (lastObjectID == null), the logs
   * directory pathname is determined and the last generated PID is retrieved from
   * the most recent log file.  These values are stored in static variables for use by
   * this object and later object instantiations.
   *
   * @param pidLogDir the pid log directory pathname.
   * @throws ClassNotFoundException class processing error
   * @throws FileNotFoundException properties or log file processing error
   * @throws IOException properties or log file processing error
   */
        public PIDGeneration(String pidLogDir) throws ClassNotFoundException, FileNotFoundException, IOException {
       String lastPID;
       String tmpObjectID;
       int index;
       Properties props;

                classRef = Class.forName("fedora.server.management.PIDGeneration");

       // Single threaded per class here.
                synchronized(classRef) {
           if (lastObjectID == null) {
               // Determine the logs directory pathname.
               logsDirectory = pidLogDir;

try{               // Retrieve the last PID generated from the
               // log file.
               lastPID = getLastPIDNoLock();

               // Remove namespace ID and ':" from the PID.
               index = lastPID.indexOf(':');
               tmpObjectID = lastPID.substring(++index);

               // Convert to BigInteger.
               lastObjectID = new BigInteger(tmpObjectID);
} catch (Throwable th) {
    lastObjectID = new BigInteger("0");
}
           }
       }
        }

   /**
   * <p>
   * Creates a new PID with the specified namespace as a prefix.
   *
   * @param namespaceID identifies a repository or group of repositories
   *
   * @return the generated PID string
   *
   * @throws IOException properties or log file processing error
   */
        public String generatePID(String namespaceID) throws IOException {
                String newPID;

       // Single threaded per class here.
                synchronized(classRef) {

                        // Increment lastObjectID by one.
                        lastObjectID = lastObjectID.add(BigInteger.ONE);

           // Format the new PID from the namespace ID and the object ID.
                        newPID = namespaceID + ":" + lastObjectID;

           // Write the new PID to the log file.
                        putNewPID(newPID);

                }
                return newPID;
        }

   /**
   * <p>
   * Returns the last generated PID
   *
   * @return the retrieved PID string
   *
   * @throws IOException log file processing error
   */
        public String getLastPID() throws IOException {
       // Single threaded per class here.
                synchronized(classRef) {
        		return getLastPIDNoLock();
       }
   }

   /**
   * <p>
   * Returns the last generated PID.  Internal version (private) that provides
   * an unsynchronized implementation for calling by getLastPID and the
   * PIDGeneration constructor.
   *
   * @return the retrieved PID string
   *
   * @throws IOException log file processing error
   */
        private String getLastPIDNoLock() throws IOException {
        	File dir;
        	String files[];
       String mrfp;
       BufferedReader in;
       String lastLine;
       String line;
       int index;
       String pid;

       // Determine the most recent log file.
        	dir = new File(logsDirectory);
        	files = dir.list();
        	Arrays.sort(files);
        	// After sorting, the most recent file will be the last one in the array.
       mrfp = logsDirectory + File.separator + files[files.length - 1];
//System.out.println("mostRecentFile: ["+mrfp+"]");

       // Open the file.
        in = new BufferedReader(new InputStreamReader(new FileInputStream(mrfp)));

       // Read the last line from this file.
       lastLine = null;
       while ((line = in.readLine()) != null)
           lastLine = line;

       // Close the file.
       in.close();

       // Return the PID from this line
       index = lastLine.indexOf('|');
                pid = lastLine.substring(0, index);
//System.out.println("pid: ["+pid+"]");
       return pid;
        }

   /**
   * <p>
   * Appends the newly created PID to the current log file.
   *
   * @param pid the identifier for the digital object.
   * @throws IOException log file processing error
   */
        private void putNewPID(String pid) throws IOException {
       Date date;
       String dateString;
       String pathName;
       PrintWriter out;

       // Determine file name from the date.
       date = new Date();
      		dateString = new SimpleDateFormat("yyyy_MM_dd").format(date);

       // File name format: PIDGeneration_YYYY_MM_DD.log
       pathName = logsDirectory + File.separator + "PIDGeneration_" + dateString + ".log";

       // Open file for appending.  If it doesn't exist, it is created.
       out = new PrintWriter(new FileWriter(pathName, true));

       // Write the log entry.
       out.println(pid + "|" + date);

       // Close file.
       out.close();
        }

   /**
   * <p>
   * Used for unit testing and demonstration purposes.
   *
   * @param args program arguments
   *
   * @throws Exception exceptions that are thrown from called methods
   */
    	static public void main (String[] args) throws Exception {
       PIDGeneration pidg = new PIDGeneration(".");
       int repetitions = 1;

       if (args.length > 0)
           repetitions = Integer.parseInt(args[0]);

       System.out.println("Calling generatePID " + repetitions + " time(s).");
       for(int i=0; i<repetitions; ++i)
           System.out.println(pidg.generatePID("uva-lib"));
    	}
}
