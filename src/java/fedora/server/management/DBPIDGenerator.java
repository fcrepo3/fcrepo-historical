package fedora.server.management;

import java.io.*;
import java.sql.*;
import java.util.*;

import fedora.server.storage.ConnectionPool;
import fedora.server.utilities.SQLUtility;

/**
 * A PIDGenerator that uses a database to keep track of the highest
 * pid it knows about for each namespace.
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
 * <p>The entire file consists of original code.  Copyright &copy; 2002-2005 by The
 * Rector and Visitors of the University of Virginia and Cornell University.
 * All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 *
 * @author cwilper@cs.cornell.edu
 * @version $Id$
 */
public class DBPIDGenerator
        implements PIDGenerator {

    private HashMap m_highestID;
    private String m_lastPID;
    private ConnectionPool m_connectionPool;

    /**
     * Initialize the DBPIDGenerator.
     *
     * This initializes the memory hash with values in the database, if any.
     *
     * If oldPidGenDir is not null, the constructor will then call
     * neverGeneratePID on the most recently generated PID as reported by
     * the log files in that directory.  This is to support automatic
     * upgrade of this functionality from versions of Fedora prior to 1.2.
     */
    public DBPIDGenerator(ConnectionPool cPool, File oldPidGenDir)
            throws IOException {
        m_connectionPool=cPool;
        m_highestID=new HashMap();
        // load the values from the database into the m_highestID hash
        // pidGen:  namespace  highestID
        Statement s=null;
        ResultSet results=null;
        Connection conn=null;
        try {
            conn=m_connectionPool.getConnection();
            String query="SELECT namespace, highestID FROM pidGen";
            s=conn.createStatement();
            results=s.executeQuery(query);
            while (results.next()) {
                m_highestID.put(results.getString("namespace"),
                                 new Integer(results.getInt("highestID")));
            }
        } catch (SQLException sqle) {
            throw new IOException("Error reading pidGen table from db: " + sqle.getMessage());
        } finally {
            try {
                if (results!=null) results.close();
                if (s!= null) s.close();
                if (conn!=null) m_connectionPool.free(conn);
            } catch (SQLException sqle2) {
                System.err.println("WARNING: Error trying to free db "
                        + "resources in DBPIDGenerator: " + sqle2.getMessage());
            } finally {
                results=null;
                s=null;
            }
        }
        upgradeIfNeeded(oldPidGenDir);
    }

    /**
     * Read the highest value from the old pidGen directory if it exists,
     * and ensure it is never used.
     */
    private void upgradeIfNeeded(File oldPidGenDir)
            throws IOException {
        if (oldPidGenDir!=null && oldPidGenDir.isDirectory()) {
            String[] names=oldPidGenDir.list();
            Arrays.sort(names);
                if (names.length>0) {
                    BufferedReader in=new BufferedReader(
                     new InputStreamReader(
                                 new FileInputStream(
                                         new File(oldPidGenDir,
                                                  names[names.length-1]))));
                String lastLine=null;
                String line;
                while ((line=in.readLine())!=null) {
                    lastLine=line;
                }
                in.close();
                if (lastLine!=null) {
                    String[] parts=lastLine.split("|");
                    if (parts.length==2) {
                        neverGeneratePID(parts[0]);
                    }
                }
            }
        }
    }

    /**
     * Generate a new pid that is guaranteed to be unique, within the
     * given namespace.
     */
    public synchronized String generatePID(String namespace)
            throws IOException {
        int i=getHighestID(namespace);
        i++;
        setHighestID(namespace, i);
        m_lastPID=namespace + ":" + i;
        return m_lastPID;
    }

    /**
     * Get the last pid that was generated.
     */
    public synchronized String getLastPID() {
        return m_lastPID;
    }

    /**
     * Cause the given PID to never be generated by the PID generator.
     */
    public synchronized void neverGeneratePID(String pid)
            throws IOException {
        String[] parts=pid.split(":");
        if (parts.length!=2) throw new IOException("Invalid pid syntax; must contain one ':' character.");
        String ns=parts[0];
        try {
            int id=Integer.parseInt(parts[1]);
            if (id>getHighestID(ns)) {
                setHighestID(ns, id);
            }
        } catch (NumberFormatException e) {
            // if the id part is not numeric, we already know we'll
            // never generate that id because all generated ids are numeric.
        }
    }

    /**
     * Gets the highest id ever used for the given namespace.
     */
    private int getHighestID(String namespace) {
        Integer i=(Integer) m_highestID.get(namespace);
        if (i==null) return 0;
        return i.intValue();
    }

    /**
     * Sets the highest id ever used for the given namespace.
     */
    private void setHighestID(String namespace, int id)
            throws IOException {
        m_highestID.put(namespace, new Integer(id));
        // write the new highest id in the database, too
        Connection conn=null;
        try {
            conn=m_connectionPool.getConnection();
            SQLUtility.replaceInto(conn,
                                   "pidGen",
                                   new String[] {"namespace", "highestID"},
                                   new String[] {namespace, "" + id},
                                   "namespace",
                                   new boolean[] {false, true},
                                   null);
        } catch (SQLException sqle) {
            throw new IOException("Error setting highest id for "
                    + "namespace in db: " + sqle.getMessage());
        } finally {
            if (conn!=null) {
                m_connectionPool.free(conn);
            }
        }
    }

}
