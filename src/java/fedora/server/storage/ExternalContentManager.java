package fedora.server.storage;

import fedora.server.Context;
import fedora.server.errors.ServerException;
import fedora.server.storage.types.MIMETypedStream;

/**
 *
 * <p><b>Title:</b> ExternalContentManager.java</p>
 * <p><b>Description:</b> Interface that provides a mechanism for retrieving
 * external content via HTTP.</p>
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
 * @author rlw@virginia.edu
 * @version $Id$
 */
public interface ExternalContentManager
{
  public MIMETypedStream getExternalContent(String URL, Context context)
      throws ServerException;

}