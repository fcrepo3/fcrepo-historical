package fedora.server.storage.types;

import fedora.server.errors.StreamIOException;
import fedora.server.storage.lowlevel.FileSystemLowlevelStorage;

import java.io.InputStream;

/**
 *
 * <p><b>Title:</b> DatastreamManagedContent.java</p>
 * <p><b>Description:</b> Managed Content.</p>
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
 */
public class DatastreamManagedContent
        extends Datastream {

    public DatastreamManagedContent() {
    }

    public InputStream getContentStream()
            throws StreamIOException
    {
      try
      {
        return FileSystemLowlevelStorage.getDatastreamStore().
            retrieve(this.DSLocation);

      } catch (Throwable th)
      {
        throw new StreamIOException("[DatastreamManagedContent] returned "
            + " the error: \"" + th.getClass().getName() + "\". Reason: "
            + th.getMessage());
      }
    }
}