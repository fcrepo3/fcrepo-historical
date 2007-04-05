/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */

package fedora.server.storage.translation;

import fedora.server.errors.ObjectIntegrityException;
import fedora.server.errors.ServerException;
import fedora.server.errors.StreamIOException;
import fedora.server.storage.types.DigitalObject;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

/**
 *
 * <p><b>Title:</b> DOSerializer.java</p>
 * <p><b>Description:</b> </p>
 *
 * @author cwilper@cs.cornell.edu
 * @version $Id$
 */
public interface DOSerializer {

    public DOSerializer getInstance() throws ServerException;

    public void serialize(DigitalObject obj, OutputStream out, 
    	String encoding, int transContext) 
            throws ObjectIntegrityException, StreamIOException,
            UnsupportedEncodingException;

}