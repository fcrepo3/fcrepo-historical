package fedora.server.storage.types;

import fedora.common.Constants;

/**
 * A data structure for holding relationships consisting of predicate and
 * subject.
 * 
 * @author rh9ec@virginia.edu
 * @version $Id$
 */
public class RelationshipTuple implements Constants {

    public String subject;
    public String predicate;
    public String object;
    public boolean isLiteral;
    public String datatype;

    public RelationshipTuple() {
    }

    public RelationshipTuple(String subject, String predicate, String object,
            boolean isLiteral, String datatype) {
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
        this.isLiteral = isLiteral;
        this.datatype = datatype;
    }

    public String getObjectPID() {
        if (object != null && !isLiteral && object.startsWith("info:fedora/")) {
            String PID = object.substring(12);
            return PID;
        }
        return null;
    }

    public String getSubjectPID() {
        if (subject != null && subject.startsWith("info:fedora/")) {
            String PID = subject.substring(12);
            return PID;
        }
        return null;
    }

    public String getRelationship() {
        String prefixRel = RELS_EXT.uri;
        if (predicate != null && predicate.startsWith(prefixRel))
        {
            String rel = "rel:" + predicate.substring(prefixRel.length());
            return(rel);
        }
        String prefixModel = MODEL.uri;
        if (predicate != null && predicate.startsWith(prefixModel))
        {
            String rel = "fedora-model:" + predicate.substring(prefixModel.length ());
            return(rel);
        }
        return(predicate);
    }

    public String toString() {
        String retVal = "Sub: " + subject + "  Pred: " + predicate + "  Obj: ["
                + object + ", " + isLiteral + ", " + datatype + "]";
        return retVal;
    }
}