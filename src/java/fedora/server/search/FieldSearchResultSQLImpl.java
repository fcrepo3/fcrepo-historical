package fedora.server.search;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import fedora.server.Logging;
import fedora.server.ReadOnlyContext;
import fedora.server.StdoutLogging;
import fedora.server.errors.UnknownSessionTokenException;
import fedora.server.errors.UnrecognizedFieldException;
import fedora.server.errors.ObjectIntegrityException;
import fedora.server.errors.QueryParseException;
import fedora.server.errors.RepositoryConfigurationException;
import fedora.server.errors.StreamIOException;
import fedora.server.errors.StorageDeviceException;
import fedora.server.errors.ServerException;
import fedora.server.storage.ConnectionPool;
import fedora.server.storage.DOReader;
import fedora.server.storage.RepositoryReader;
import fedora.server.storage.types.DatastreamXMLMetadata;
import fedora.server.utilities.DateUtility;
import fedora.server.utilities.MD5Utility;

/**
 * A FieldSearchResults object returned as the result of a 
 * FieldSearchSQLImpl search.
 * <p />
 * A FieldSearchResultSQLImpl is intended to be re-used in cases where
 * the results of a query require more than one call to the server.
 */
public class FieldSearchResultSQLImpl
        extends StdoutLogging
        implements FieldSearchResult {

    private static ReadOnlyContext s_nonCachedContext;
    static {
        HashMap h=new HashMap();
        h.put("useCachedObject", "false");
        s_nonCachedContext=new ReadOnlyContext(h);
    }
    
    /* fields supporting public accessors */
    private ArrayList m_objectFields;
    private String m_token;
    private long m_cursor=-1;
    private long m_completeListSize=-1;
    private Date m_expirationDate;
  
    /* invariants */
    private Connection m_conn;
    private ConnectionPool m_cPool;
    private RepositoryReader m_repoReader;
    private String[] m_resultFields;
    private int m_maxResults;
    private int m_maxSeconds;
    private long m_startMillis;

    /* internal state */
    private ResultSet m_resultSet;
    private long m_nextCursor=0;
    private boolean m_expired;
    

    /**
     * Construct a FieldSearchResultSQLImpl object.
     * <p />
     * Upon construction, a connection is obtained from the connectionPool,
     * and the query is executed.  (The connection will be returned to the
     * pool only after the last result has been obtained from the ResultSet,
     * the session is expired, or some non-recoverable error has occurred)
     * <p />
     * Once the ResultSet is obtained, one result is requested of it
     * (and remembered for use in step()); then the call returns.
     * 
     * @param cPool the connectionPool
     * @param repoReader the provider of object field information for results
     * @param resultFields which fields should be returned in results
     * @param maxResults how many results should be returned at one time.  This
     *        should be the smaller of a) the FieldSearchImpl's limit [the server
     *        limit] and b) the requested limit [the client limit]
     * @param query the end-user query
     * @param logTarget where to send log messages
     */
    protected FieldSearchResultSQLImpl(ConnectionPool cPool, 
            RepositoryReader repoReader, String[] resultFields, int maxResults,
            int maxSeconds, FieldSearchQuery query, 
            Logging logTarget) 
            throws SQLException, QueryParseException {
        super(logTarget);
        m_cPool=cPool;
        m_repoReader=repoReader;
        m_resultFields=resultFields;
        m_maxResults=maxResults;
        m_maxSeconds=maxSeconds;
        m_conn=m_cPool.getConnection();
        try {
            m_resultSet=getResultSet(query);
        } catch (SQLException sqle) {
            // if there's any kind of problem getting the resultSet,
            // give the connection back to the pool
            m_cPool.free(m_conn);
            throw sqle;
        }
    }
    
    private ResultSet getResultSet(FieldSearchQuery query) 
            throws SQLException, QueryParseException {
        StringBuffer queryText=new StringBuffer();
        queryText.append("SELECT doFields.pid FROM doFields");
        if (query.getType()==FieldSearchQuery.TERMS_TYPE) {
            queryText.append(getWhereClause(query.getTerms()));
        } else {
            queryText.append(getWhereClause(query.getConditions()));
        }
        return m_conn.createStatement().executeQuery(queryText.toString());
        
    }
    
    private String getWhereClause(String terms) 
            throws QueryParseException {
        if (terms.indexOf("'")!=-1) {
            throw new QueryParseException("Query cannot contain the ' character.");
        }
        StringBuffer whereClause=new StringBuffer();
        if (!terms.equals("*") && !terms.equals("")) {
            whereClause.append(" WHERE");
            // formulate the where clause if the terms aren't * or ""
            int usedCount=0;
            boolean needsEscape=false;
            for (int i=0; i<FieldSearchSQLImpl.DB_COLUMN_NAMES.length; i++) {
                String column=FieldSearchSQLImpl.DB_COLUMN_NAMES[i];
                // use only stringish columns in query
                boolean use=column.indexOf("Date")==-1;
                if (!use) {
                    if (column.equals("dcDate")) {
                        use=true;
                    }
                }
                if (use) {
                    if (usedCount>0) {
                        whereClause.append(" OR");
                    }
                    String qPart=toSql(column, terms);
                    if (qPart.charAt(0)==' ') {
                        needsEscape=true;
                    } else {
                        whereClause.append(" ");
                    }
                    whereClause.append(qPart);
                    usedCount++;
                }
            }
            if (needsEscape) {
                whereClause.append(" {escape '/'}");
            }
        }
        return whereClause.toString();
    }
    
    private String getWhereClause(List conditions) 
            throws QueryParseException {
        StringBuffer whereClause=new StringBuffer();
        boolean willJoin=false;
        if (conditions.size()>0) {
            boolean needsEscape=false;
            whereClause.append(" WHERE");
            for (int i=0; i<conditions.size(); i++) {
                Condition cond=(Condition) conditions.get(i);
                if (i>0) {
                    whereClause.append(" AND");
                }
                String op=cond.getOperator().getSymbol();
                String prop=cond.getProperty();
                if (prop.toLowerCase().endsWith("date")) {
                    // deal with dates ... cDate mDate dcmDate date
                    if (op.equals("~")) {
                        if (prop.equals("date")) {
                            // query for dcDate as string
                            String sqlPart=toSql("doFields.dcDate", cond.getValue());
                            if (sqlPart.startsWith(" ")) {
                                needsEscape=true;
                            } else {
                                whereClause.append(' ');
                            }
                            whereClause.append(sqlPart);
                        } else {
                            throw new QueryParseException("The ~ operator "
                                    + "cannot be used with cDate, mDate, "
                                    + "or dcmDate because they are not "
                                    + "string-valued fields.");
                        }
                    } else { // =, <, <=, >, >=
                        // property must be parsable as a date... if ok,
                        // do (cDate, mDate, dcmDate) 
                        // or (date) <- dcDate from dcDates table
                        Date dt=DateUtility.parseDate(cond.getValue());
                        if (dt==null) {
                            throw new QueryParseException("When using "
                                    + "equality or inequality operators "
                                    + "with a date-based value, the date "
                                    + "must be in yyyy-MM-DD[Thh:mm:ss[Z]] "
                                    + "form.");
                        }
                        if (prop.equals("date")) {
                            // do a left join on the dcDates table...dcDate
                            // query will be of form: 
                            // select pid 
                            // from doFields 
                            // left join dcDates on doFields.pid=dcDates.pid 
                            // where...
                            if (!willJoin) {
                                willJoin=true;
                                whereClause.insert(0, " LEFT JOIN dcDates "
                                        + "ON doFields.pid=dcDates.pid");
                            }
                            whereClause.append(" dcDates.dcDate" + op 
                                    + dt.getTime() );
                        } else {
                            whereClause.append(" doFields." + prop + op
                                    + dt.getTime() );
                        }
                    }
                } else {
                    if (op.equals("=")) {
                        if (isDCProp(prop)) {
                            throw new QueryParseException("The = operator "
                                    + "can only be used with dates and "
                                    + "non-repeating fields.");
                        } else {
                            // do a real equals check... do a toSql but
                            // reject it if it uses "LIKE"
                            String sqlPart=toSql("doFields." + prop, cond.getValue());
                            if (sqlPart.indexOf("LIKE ")!=-1) {
                                throw new QueryParseException("The = "
                                    + "operator cannot be used with "
                                    + "wildcards.");
                            }
                            if (sqlPart.startsWith(" ")) {
                                needsEscape=true;
                            } else {
                                whereClause.append(' ');
                            }
                            whereClause.append(sqlPart);
                        }
                    } else if (op.equals("~")) {
                        if (isDCProp(prop)) {
                            // prepend dc and caps the first char first...
                            prop="dc" + prop.substring(0,1).toUpperCase() 
                                    + prop.substring(1);  
                        }
                        // the field name is ok, so toSql it
                        String sqlPart=toSql("doFields." + prop, 
                                cond.getValue());
                        if (sqlPart.startsWith(" ")) {
                            needsEscape=true;
                        } else {
                            whereClause.append(' ');
                        }
                        whereClause.append(sqlPart);
                    } else {
                        throw new QueryParseException("Can't use >, >=, <, "
                                + "or <= operator on a string-based field.");
                    }
                }
            }
            if (needsEscape) {
                whereClause.append(" {escape '/'}");
            }
        }
        return whereClause.toString();
    }
    
    protected boolean isExpired() {
        long passedSeconds=(System.currentTimeMillis() - m_startMillis)/1000;
        m_expired=(passedSeconds > m_maxSeconds);
        if (m_expired) {
            // clean up
            try {
                m_resultSet.close();
            } catch (SQLException sqle2) {
            } finally {
                m_cPool.free(m_conn);
            }
        }
        return m_expired;
    }
   
    /**
     * Update object with the next chunk of results.
     *
     * if getToken() is null after this call, the resultSet was exhausted.
     */
    protected void step() 
            throws UnrecognizedFieldException, ObjectIntegrityException,
            RepositoryConfigurationException, StreamIOException, 
            ServerException {
        m_objectFields=new ArrayList();        
        int resultCount=0;
        // run through resultSet, adding each result to m_objectFields
        // for up to maxResults objects, or until the result set is
        // empty, whichever comes first.
        try {
            while (m_resultSet.next() && resultCount<m_maxResults) {
                resultCount++;
                // add the current object from the resultSet to m_objectFields
                String pid=m_resultSet.getString("pid");
                m_objectFields.add(getObjectFields(pid));
            }
            // done with this block.  now, are there any more results to return?
            if (resultCount>0 && !m_resultSet.isAfterLast()) {
                // yes, so generate a token, make sure the cursor is set, 
                // and make sure the expirationDate is set
                long now=System.currentTimeMillis();
                m_token=MD5Utility.getBase16Hash(this.hashCode() + "" +  now);
                m_cursor=m_nextCursor;
                // keep m_nextCursor updated for next block
                m_nextCursor+=resultCount;
                m_startMillis=now;
                Date dt=new Date();
                dt.setTime(m_startMillis + (1000 * m_maxSeconds));
                m_expirationDate=DateUtility.convertLocalDateToUTCDate(dt);
            } else {
                // no, so make sure the token is null and clean up
                m_token=null;
                try {
                    m_resultSet.close();
                } catch (SQLException sqle2) {
                } finally {
                    m_cPool.free(m_conn);
                }
            }
        } catch (SQLException sqle) {
            try {
                m_resultSet.close();
            } catch (SQLException sqle2) {
            } finally {
                m_cPool.free(m_conn);
            }
        }
    }
    
    /**
     * For the given pid, get a reader on the object from the repository
     * and return an ObjectFields object with resultFields fields populated.
     *
     * @param pid the unique identifier of the object for which the information
     *        is requested.
     * @return ObjectFields populated with the requested fields
     * @throws UnrecognizedFieldException if a resultFields value isn't valid
     * @throws ObjectIntegrityException if the underlying digital object can't
     *         be parsed
     * @throws RepositoryConfigurationException if the sax parser can't
     *         be constructed
     * @throws StreamIOException if an error occurs while reading the serialized
     *         digital object stream
     * @throws ServerException if any other kind of error occurs while reading
     *         the underlying object
     */
    private ObjectFields getObjectFields(String pid) 
            throws UnrecognizedFieldException, ObjectIntegrityException,
            RepositoryConfigurationException, StreamIOException, 
            ServerException {
        DOReader r=m_repoReader.getReader(s_nonCachedContext, pid);
        ObjectFields f;
        // If there's a DC record available, use SAX to parse the most 
        // recent version of it into f.
        DatastreamXMLMetadata dcmd=null;
        try {
            dcmd=(DatastreamXMLMetadata) r.GetDatastream("DC", null);
        } catch (ClassCastException cce) {
            throw new ObjectIntegrityException("Object " + r.GetObjectPID() 
                    + " has a DC datastream, but it's not inline XML.");
        }
        if (dcmd!=null) {
            f=new ObjectFields(m_resultFields, dcmd.getContentStream());
            // add dcmDate if wanted
            for (int i=0; i<m_resultFields.length; i++) {
                if (m_resultFields[i].equals("dcmDate")) {
                    f.setDCMDate(dcmd.DSCreateDT);
                }
            }
        } else {
            f=new ObjectFields();
        }
        // add non-dc values from doReader for the others in m_resultFields[]
        for (int i=0; i<m_resultFields.length; i++) {
            String n=m_resultFields[i];
            if (n.equals("pid")) {
                f.setPid(pid);
            }
            if (n.equals("label")) {
                f.setLabel(r.GetObjectLabel());
            }
            if (n.equals("fType")) {
                f.setFType(r.getFedoraObjectType());
            }
            if (n.equals("cModel")) {
                f.setCModel(r.getContentModelId());
            }
            if (n.equals("state")) {
                f.setState(r.GetObjectState());
            }
            if (n.equals("locker")) {
                f.setLocker(r.getLockingUser());
            }
            if (n.equals("cDate")) {
                f.setCDate(r.getCreateDate());
            }
            if (n.equals("mDate")) {
                f.setMDate(r.getLastModDate());
            }
        }
        return f;
    }

    
    public List objectFieldsList() {
        return m_objectFields;
    }

    public String getToken() {
        return m_token;
    }
    
    public long getCursor() {
        return m_cursor;
    }
    
    public long getCompleteListSize() {
        return m_completeListSize;
    }
    
    public Date getExpirationDate() {
        return m_expirationDate;
    }
    
    /**
     * Return a condition suitable for a SQL WHERE clause, given a column
     * name and a string with a possible pattern (using * and questionmark 
     * wildcards).
     * <p></p>
     * If the string has any characters that need to be escaped, it will
     * begin with a space, indicating to the caller that the entire WHERE
     * clause should end with " {escape '/'}".
     *
     * @param name the name of the field in the database
     * @param in the query string, where * and ? are treated as wildcards
     * @return String a suitable string for use in a SQL WHERE clause,
     *         as described above
     */
    private static String toSql(String name, String in) {
        in=in.toLowerCase();
        if (name.startsWith("dc") || (name.startsWith("doFields.dc"))) {
            StringBuffer newIn=new StringBuffer();
            if (!in.startsWith("*")) {
                newIn.append("* ");
            }
            newIn.append(in);
            if (!in.endsWith("*")) {
                newIn.append(" *");
            }
            in=newIn.toString();
        }
        if (in.indexOf("\\")!=-1) {
            // has one or more escapes, un-escape and translate
            StringBuffer out=new StringBuffer();
            out.append("\'");
            boolean needLike=false;
            boolean needEscape=false;
            boolean lastWasEscape=false;
            for (int i=0; i<in.length(); i++) {
                char c=in.charAt(i);
                if ( (!lastWasEscape) && (c=='\\') ) {
                    lastWasEscape=true;
                } else {
                    char nextChar='!';
                    boolean useNextChar=false;
                    if (!lastWasEscape) {
                        if (c=='?') {
                            out.append('_');
                            needLike=true;
                        } else if (c=='*') {
                            out.append('%');
                            needLike=true;
                        } else {
                            nextChar=c;
                            useNextChar=true;
                        }
                    } else {
                        nextChar=c;
                        useNextChar=true;
                    }
                    if (useNextChar) {
                        if (nextChar=='\"') {
                            out.append("\\\"");
                            needEscape=true;
                        } else if (nextChar=='\'') {
                            out.append("\\\'");
                            needEscape=true;
                        } else if (nextChar=='%') {
                            out.append("\\%");
                            needEscape=true;
                        } else if (nextChar=='_') {
                            out.append("\\_");
                            needEscape=true;
                        } else {
                            out.append(nextChar);
                        }
                    }
                    lastWasEscape=false;
                }
            }
            out.append("\'");
            if (needLike) {
                out.insert(0, " LIKE ");
            } else {
                out.insert(0, " = ");
            }
            out.insert(0, name);
            if (needEscape) {
                out.insert(0, ' ');
            }
            return out.toString();
        } else {
            // no escapes, just translate if needed
            StringBuffer out=new StringBuffer();
            out.append("\'");
            boolean needLike=false;
            boolean needEscape=false;
            for (int i=0; i<in.length(); i++) {
                char c=in.charAt(i);
                if (c=='?') {
                    out.append('_');
                    needLike=true;
                } else if (c=='*') {
                    out.append('%');
                    needLike=true;
                } else if (c=='\"') {
                    out.append("\\\"");
                    needEscape=true;
                } else if (c=='\'') {
                    out.append("\\\'");
                    needEscape=true;
                } else if (c=='%') {
                    out.append("\\%");
                    needEscape=true;
                } else if (c=='_') {
                    out.append("\\_");
                    needEscape=true;
                } else {
                    out.append(c);
                }
            }
            out.append("\'");
            if (needLike) {
                out.insert(0, " LIKE ");
            } else {
                out.insert(0, " = ");
            }
            out.insert(0, name);
            if (needEscape) {
                out.insert(0, ' ');
            }
            return out.toString();
        }
    }

    /**
     * Tell whether a field name, as given in the search request, is a
     * dublin core field.
     *
     * @param the field
     * @return whether it's a dublin core field
     */
    private boolean isDCProp(String in) {
        for (int i=0; i<FieldSearchSQLImpl.DB_COLUMN_NAMES.length; i++) {
            String n=FieldSearchSQLImpl.DB_COLUMN_NAMES[i];
            if ( (n.startsWith("dc"))
                    && (n.toLowerCase().indexOf(in.toLowerCase())!=-1) ) {
                return true;
            }
        }
        return false;
    }
    
}    

