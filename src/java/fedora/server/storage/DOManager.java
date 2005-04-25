package fedora.server.storage;

import java.io.InputStream;

import fedora.server.Context;
import fedora.server.errors.ServerException;
import fedora.server.search.FieldSearchQuery;
import fedora.server.search.FieldSearchResult;

/**
 *
 * <p><b>Title:</b> DOManager.java</p>
 * <p><b>Description:</b> A RepositoryReader that provides facilities for creating
 * and modifying objects within the repository, as well as
 * a query facility.</p>
 *
 * @author cwilper@cs.cornell.edu
 * @version $Id$
 */
public interface DOManager
        extends RepositoryReader {

    /**
     * Relinquishes control of a DOWriter back to the DOManager.
     * <p></p>
     * When a DOManager provides a DOWriter, it creates a session lock.
     * This is used to guarantee that there will never be concurrent changes
     * to the same object. To release the session lock, a DOWriter user
     * calls this method.
     *
     * @param writer an instance of a digital object writer.
     * @throws ServerException if an error occurs in obtaining a writer.
     */
    public abstract void releaseWriter(DOWriter writer)
            throws ServerException;

    /**
     * Gets a DOWriter for an existing digital object.
     *
     * @param context The context of this request.
     * @param pid The PID of the object.
     * @return A writer, or null if the pid didn't point to an accessible object.
     * @throws ServerException If anything went wrong.
     */
    public abstract DOWriter getWriter(boolean cachedObjectRequired, Context context, String pid)
            throws ServerException;

    /**
     * Creates a digital object with a newly-allocated pid, and returns
     * a DOWriter on it.
     *
     * @param context The context of this request.
     * @return A writer.
     * @throws ServerException If anything went wrong.
     */
    /*
    public abstract DOWriter getIngestWriter(Context context)
            throws ServerException;
    */

    /**
     * Creates a copy of the digital object given by the InputStream,
     * with either a new PID or the PID indicated by the InputStream.
     *
     * @param context The context of this request.
     * @param in A serialization of the digital object.
     * @param format The format of the serialization.
     * @param encoding The character encoding.
     * @param newPid Whether a new PID should be generated or the one indicated
     *        by the InputStream should be used.
     * @return a writer.
     * @throws ServerException If anything went wrong.
     */
    public abstract DOWriter getIngestWriter(boolean cachedObjectRequired, Context context, InputStream in, String format, String encoding, boolean newPid)
            throws ServerException;

    public FieldSearchResult findObjects(Context context,
            String[] resultFields, int maxResults, FieldSearchQuery query)
            throws ServerException;

    public FieldSearchResult resumeFindObjects(Context context,
            String sessionToken)
            throws ServerException;

    public String[] getNextPID(int numPIDs, String namespace) throws ServerException;

}