package fedora.server.storage.types;

import fedora.common.HttpClient;
import fedora.server.errors.StreamIOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import org.apache.commons.httpclient.methods.GetMethod;

/**
 *
 * <p><b>Title:</b> DatastreamReferencedContent.java</p>
 * <p><b>Description:</b> Referenced Content.</p>
 *
 * @author cwilper@cs.cornell.edu
 * @version $Id$
 */
public class DatastreamReferencedContent
        extends Datastream {

    public DatastreamReferencedContent() {
    }

    /**
     * Gets an InputStream to the content of this externally-referenced
     * datastream.
     * <p></p>
     * The DSLocation of this datastream must be non-null before invoking
     * this method.
     * <p></p>
     * If successful, the DSMIME type is automatically set based on the
     * web server's response header.  If the web server doesn't send a
     * valid Content-type: header, as a last resort, the content-type
     * is guessed by using a map of common extensions to mime-types.
     * <p></p>
     * If the content-length header is present in the response, DSSize
     * will be set accordingly.
     */
    public InputStream getContentStream()
            throws StreamIOException {
      	InputStream contentStream = null;
      	try {
      		HttpClient client = new HttpClient(DSLocation);
      		GetMethod getMethod = client.doNoAuthnGet(20000, 25, 1);
      		DSSize = client.getContentLength();
      		contentStream = getMethod.getResponseBodyAsStream();
      		//get.releaseConnection() before stream is read would give java.io.IOException: Attempted read on closed stream.
      	} catch (Throwable th) {
      		th.printStackTrace();
      		throw new StreamIOException("[DatastreamReferencedContent] "
      			+ "returned an error.  The underlying error was a "
    			+ th.getClass().getName() + "  The message "
    			+ "was  \"" + th.getMessage() + "\"  .  ");
      	} finally {
      		log("in getContentStream(), in finally");     	
      	}    	
    	return(contentStream);
    }
    
    
    private boolean log = false;
    
    private final void log(String msg) {
    	if (log) {
  	  	System.err.println(msg);	  		
    	}
    }    
    
}
