package fedora.oai;

/**
 *
 * <p><b>Title:</b> NoRecordsMatchException.java</p>
 * <p><b>Description:</b> Signals that the combination of the values of the
 * from, until, set and metadataPrefix arguments results in an empty list.</p>
 *
 * <p>This may occur while fulfilling a ListIdentifiers or ListRecords request.</p>
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
 * @author cwilper@cs.cornell.edu
 * @version $Id$
 */
public class NoRecordsMatchException
        extends OAIException {

    public NoRecordsMatchException() {
        super("noRecordsMatch", null);
    }

    public NoRecordsMatchException(String message) {
        super("noRecordsMatch", message);
    }

}