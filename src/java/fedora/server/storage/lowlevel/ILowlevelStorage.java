/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */

package fedora.server.storage.lowlevel;

import java.io.InputStream;

import fedora.server.errors.LowlevelStorageException;

/**
 * ILowlevelStorage.java
 * 
 * @author Bill Niebel
 */
public interface ILowlevelStorage {

    /**
     * @param pid
     * @param content
     * @throws LowlevelStorageException
     */
    public void addObject(String pid, InputStream content)
            throws LowlevelStorageException;

    /**
     * @param pid
     * @param content
     * @throws LowlevelStorageException
     */
    public void replaceObject(String pid, InputStream content)
            throws LowlevelStorageException;

    /**
     * @param pid
     * @return bytestream containing data object
     * @throws LowlevelStorageException
     */
    public InputStream retrieveObject(String pid)
            throws LowlevelStorageException;

    /**
     * @param pid
     * @throws LowlevelStorageException
     */
    public void removeObject(String pid) throws LowlevelStorageException;

    /**
     * @throws LowlevelStorageException
     */
    public void rebuildObject() throws LowlevelStorageException;

    /**
     * @throws LowlevelStorageException
     */
    public void auditObject() throws LowlevelStorageException;

    /**
     * @param pid
     * @param content
     * @throws LowlevelStorageException
     */
    public void addDatastream(String pid, InputStream content)
            throws LowlevelStorageException;

    /**
     * @param pid
     * @param content
     * @throws LowlevelStorageException
     */
    public void replaceDatastream(String pid, InputStream content)
            throws LowlevelStorageException;

    /**
     * @param pid
     * @return bytestream containing datastream
     * @throws LowlevelStorageException
     */
    public InputStream retrieveDatastream(String pid)
            throws LowlevelStorageException;

    /**
     * @param pid
     * @throws LowlevelStorageException
     */
    public void removeDatastream(String pid) throws LowlevelStorageException;

    /**
     * @throws LowlevelStorageException
     */
    public void rebuildDatastream() throws LowlevelStorageException;

    /**
     * @throws LowlevelStorageException
     */
    public void auditDatastream() throws LowlevelStorageException;
}
