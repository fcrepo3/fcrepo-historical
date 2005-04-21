package fedora.oai;

/**
 *
 * <p><b>Title:</b> DeletedRecordSupport.java</p>
 * <p><b>Description:</b> An indicator of the kind of deletion support a
 * repository has.</p>
 *
 * @author cwilper@cs.cornell.edu
 * @version $Id$
 */
public class DeletedRecordSupport {

    /**
     * Indicates that the repository does not maintain information about
     * deletions. A repository that indicates this level of support must not
     * reveal a deleted status in any response.
     */
    public static final DeletedRecordSupport NO=new DeletedRecordSupport("no");

    /**
     * Indicates that the repository does not guarantee that a list of
     * deletions is maintained persistently or consistently. A repository
     * that indicates this level of support may reveal a deleted status for
     * records.
     */
    public static final DeletedRecordSupport TRANSIENT=new DeletedRecordSupport("transient");

    /**
     * Indicates that the repository maintains information about deletions with
     * no time limit. A repository that indicates this level of support must
     * persistently keep track of the full history of deletions and consistently
     * reveal the status of a deleted record over time.
     */
    public static final DeletedRecordSupport PERSISTENT=new DeletedRecordSupport("persistent");

    private String m_stringValue;

    private DeletedRecordSupport(String stringValue) {
        m_stringValue=stringValue;
    }

    public String toString() {
        return m_stringValue;
    }

}