package fedora.oai;

import java.util.Set;

/**
 *
 * <p><b>Title:</b> SetInfo.java</p>
 * <p><b>Description:</b> Describes a set in the repository.</p>
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
 * @see <a href="http://www.openarchives.org/OAI/openarchivesprotocol.html#ListSets">
 *      http://www.openarchives.org/OAI/openarchivesprotocol.html#ListSets</a>
 */
public interface SetInfo {

    /**
     * Get the name of the set.
     */
    public abstract String getName();

    /**
     * Get the setSpec of the set.
     */
    public abstract String getSpec();

    /**
     * Get the descriptions of the set.
     */
    public abstract Set getDescriptions();

}