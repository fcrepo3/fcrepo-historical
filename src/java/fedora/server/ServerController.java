package fedora.server;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import fedora.common.Constants;
import fedora.server.errors.ModuleInitializationException;
import fedora.server.errors.ServerInitializationException;
import fedora.server.errors.authorization.AuthzDeniedException;
import fedora.server.errors.authorization.AuthzException;
import fedora.server.errors.authorization.AuthzOperationalException;
import fedora.server.errors.authorization.AuthzPermittedException;
import fedora.server.errors.servletExceptionExtensions.BadRequest400Exception;
import fedora.server.errors.servletExceptionExtensions.Continue100Exception;
import fedora.server.errors.servletExceptionExtensions.Forbidden403Exception;
import fedora.server.errors.servletExceptionExtensions.InternalError500Exception;
import fedora.server.errors.servletExceptionExtensions.Ok200Exception;
import fedora.server.errors.servletExceptionExtensions.Unavailable503Exception;
import fedora.server.utilities.ServerUtility;
import fedora.server.Server;

/**
 *
 * <p><b>Title:</b> ServerController.java</p>
 * <p><b>Description:</b> </p>
 *
 * @author cwilper@cs.cornell.edu
 * @version $Id$
 */
public class ServerController
        extends HttpServlet {

    private static Server s_server;

    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
    	System.err.println("Z1");
        String actionLabel = "server control";    	
        String action=request.getParameter("action");
        String requestInfo="Got controller '" + action + "' request from " + request.getRemoteAddr();
    	System.err.println("Z2 action=" + action);
        if (fedora.server.Debug.DEBUG) System.out.println(requestInfo);
        if (action==null) {
            throw new BadRequest400Exception(request, actionLabel, "no action", new String[0]);	
        } 
        if (action.equals("startup")) {
        	actionLabel = "starting server";
        	boolean serverHasInstance = false;
        	System.err.println("Z3");
            try {
                serverHasInstance = Server.hasInstance(new File(System.getProperty("fedora.home")));
            	System.err.println("Z4");
            } catch (Throwable t) {
            	System.err.println("A " + t.getMessage() + " " + ((t.getCause() == null) ? "" : t.getCause().getMessage()));
                throw new InternalError500Exception(request, actionLabel, "error starting server", new String[0]);	
            }          
        	System.err.println("Z5");
            if (serverHasInstance) {
            	System.err.println("B");
                throw new InternalError500Exception(request, actionLabel, "server already started", new String[0]);	
            }	
        	System.err.println("Z6");
            try {
				s_server=Server.getInstance(new File(System.getProperty("fedora.home")));
			} catch (ServerInitializationException e) {
            	System.err.println("C " + e.getMessage() + " " + ((e.getCause() == null) ? "" : e.getCause().getMessage()));
                throw new InternalError500Exception(request, actionLabel, "error starting server", new String[0]);	
			} catch (ModuleInitializationException e) {
            	System.err.println("D " + e.getMessage() + " " + ((e.getCause() == null) ? "" : e.getCause().getMessage()));				
                throw new InternalError500Exception(request, actionLabel, "error starting server", new String[0]);	
			}
        	System.err.println("Z7");
            throw new Ok200Exception(request, actionLabel,"server started successfully", new String[0]);    
        }
        if (action.equals("shutdown")) {
        	actionLabel = "shutting server down";
            if (! Server.hasInstance(new File(System.getProperty("fedora.home")))) {
                throw new InternalError500Exception(request, actionLabel, "server already shut down", new String[0]);	
            }
            try {
                s_server=Server.getInstance(new File(System.getProperty("fedora.home")));
                s_server.logInfo(requestInfo);
            	Context context = ReadOnlyContext.getContext(Constants.HTTP_REQUEST.REST.uri, request);
                s_server.shutdown(context);
    		} catch (AuthzOperationalException aoe) {
                throw new Forbidden403Exception(request, actionLabel, "authorization failed", new String[0]);                
            } catch (AuthzDeniedException ade) {
                throw new Forbidden403Exception(request, actionLabel, "authorization denied", new String[0]);
			} catch (AuthzPermittedException ape) {
                throw new Continue100Exception(request, actionLabel, "authorization permitted", new String[0]);	    			
            } catch (Throwable t) {
                throw new InternalError500Exception(request, actionLabel, "error shutting down server", new String[0]);	
            }
            throw new Ok200Exception(request, actionLabel, "server shut down successfully", new String[0]);	            
        }
        if (action.equals("status")) {
        	actionLabel = "getting server status";
        	Context context = ReadOnlyContext.getContext(Constants.HTTP_REQUEST.REST.uri, request);
        	File fedoraHome = new File(System.getProperty("fedora.home"));
            if (! Server.hasInstance(fedoraHome)) {
                throw new Unavailable503Exception(request, actionLabel, "server not available", new String[0]);	
            }
			Server server = null;
			try {
				server = Server.getInstance(fedoraHome, false);
            } catch (Throwable t) {
                throw new InternalError500Exception(request, actionLabel, "error performing action0", new String[0]);	
            }
			if (server == null) {
                throw new InternalError500Exception(request, actionLabel, "error performing action1", new String[0]);	
			}
			try {				
				server.status(context);
    		} catch (AuthzOperationalException aoe) {
                throw new Forbidden403Exception(request, actionLabel, "authorization failed", new String[0]);                					
    		} catch (AuthzDeniedException ade) {
                throw new Forbidden403Exception(request, actionLabel, "authorization denied", new String[0]);
			} catch (AuthzPermittedException ape) {
                throw new Continue100Exception(request, actionLabel, "authorization permitted", new String[0]);	    			
            } catch (Throwable t) {
                throw new InternalError500Exception(request, actionLabel, "error performing action2", new String[0]);	
            }
			throw new Ok200Exception(request, actionLabel, "server running", new String[0]);
        } 
        throw new BadRequest400Exception(request, actionLabel, "bad action:  " + action, new String[0]);
    }

    public void init() {
    }

    public void destroy() {
    }
    
}
