package fedora.server.search;

import java.util.List;

import fedora.server.errors.ServerException;
import fedora.server.storage.DOReader;

/**
 * A provider of a simple field-based search service across all objects
 * in the repository.
 * <p></p>
 * Key object metadata and dublin core fields are searchable from via 
 * implementations of this interface.
 * <p></p>
 * Key fields include:<dir>
 * <i>pid, label, cModel, state, locker, cDate, mDate, dcmDate</i></dir>
 * <p></p>
 * Dublin core fields include:<dir>
 * <i>title, creator, subject, description, publisher, contributor, date, type,
 * format, identifier, source, language, relation, coverage, rights</i></dir>
 *
 * @author cwilper@cs.cornell.edu
 */
public interface FieldSearch {

    /**
     * Update the search indexes with information from the provided DOReader.
     *
     * @param reader the DOReader containing all the field information
     *        for the object
     * @throws ServerException if anything went wrong
     */
    public void update(DOReader reader) 
            throws ServerException;

    /**
     * Remove an object from the search indexes.
     *
     * @param pid the unique id of the object whose info should be removed
     * @throws ServerException if anything went wrong
     */
    public boolean delete(String pid) 
            throws ServerException;

    /**
     * Search across specific fields and return the desired fields.
     *
     * @param resultFields the desired fields
     * @param maxResults the maximum number of results the client wants
     * @param query the query
     * @return FieldSearchResult the results
     * @throws ServerException if anything went wrong
     */
    public FieldSearchResult findObjects(String[] resultFields, 
            int maxResults, FieldSearchQuery query) 
            throws ServerException;

    /**
     * Resume an in-progress search across specific fields and return the 
     * desired fields.
     *
     * @param sessiontoken the token of the session in which the remaining
     *        results can be found
     * @return FieldSearchResult the results
     * @throws ServerException if anything went wrong
     */
    public FieldSearchResult resumeFindObjects(String sessionToken) 
            throws ServerException;
            
}