/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */

package fedora.client.objecteditor;

import java.util.*;

/**
 * Compares binding sequences for sorting.
 */
import fedora.server.types.gen.DatastreamBinding;

    public class DatastreamBindingComparator
            implements Comparator {

        public int compare(Object o1, Object o2) {
            int seqNo1=getInt(((DatastreamBinding) o1).getSeqNo());
            int seqNo2=getInt(((DatastreamBinding) o2).getSeqNo());
            return seqNo1-seqNo2; // negative if lt, 0 if equal, positive if gt
        }

        private int getInt(String number) {
            try {
                return Integer.parseInt(number);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }