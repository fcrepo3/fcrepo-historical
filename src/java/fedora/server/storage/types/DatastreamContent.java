package fedora.server.storage.types;

import fedora.server.errors.StreamIOException;

import java.io.InputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * <p><b>Title:</b> DatastreamContent.java</p>
 * <p><b>Description:</b> Referenced content or managed content -- can point
 * to metadata datastreams.</p>
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
 * <p>The entire file consists of original code.  Copyright &copy; 2002-2004 by The
 * Rector and Visitors of the University of Virginia and Cornell University.
 * All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 *
 * @author cwilper@cs.cornell.edu
 * @version $Id$
 */
public class DatastreamContent
        extends Datastream {

    private ArrayList m_metadataIdList;

    public DatastreamContent() {
        m_metadataIdList=new ArrayList();
    }

    /**
     * These reference a datastream series, not an individual version id.
     */
    public List metadataIdList() {
        return m_metadataIdList;
    }
}
