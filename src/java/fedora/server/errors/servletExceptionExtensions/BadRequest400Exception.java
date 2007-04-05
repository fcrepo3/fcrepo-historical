/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */

package fedora.server.errors.servletExceptionExtensions;

import javax.servlet.http.HttpServletRequest;

/**
 *
 * <p><b>Title:</b> Exception400.java</p>
 * <p><b>Description:</b> Thrown to reach 400-Bad Request error page.  
 * Can be used when forwarding can't, e.g., after some http output has already been written.</p>
 *
 * @author wdn5e@virginia.edu
 * @version $Id$
 */
public class BadRequest400Exception
        extends RootException {
	
	private static final long serialVersionUID = 1L;
    public BadRequest400Exception(HttpServletRequest request, String action, String detail, String[] details) {
        super(request, action, detail, details);
    }
    
    public BadRequest400Exception(HttpServletRequest request, String message, String action, String detail, String[] details) {
        super(message, request, action, detail, details);
    }

    public BadRequest400Exception(String message, Throwable cause, HttpServletRequest request, String action, String detail, String[] details) {
        super(message, cause, request, action, detail, details);
    }

}