package fedora.server;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fedora.common.Constants;
import fedora.server.errors.authorization.AuthzDeniedException;
import fedora.server.errors.authorization.AuthzOperationalException;
import fedora.server.errors.authorization.AuthzPermittedException;
import fedora.server.errors.servletExceptionExtensions.BadRequest400Exception;
import fedora.server.errors.servletExceptionExtensions.Continue100Exception;
import fedora.server.errors.servletExceptionExtensions.Forbidden403Exception;
import fedora.server.errors.servletExceptionExtensions.InternalError500Exception;
import fedora.server.errors.servletExceptionExtensions.Ok200Exception;
import fedora.server.errors.servletExceptionExtensions.Unavailable503Exception;
import fedora.server.security.Authorization;
import fedora.server.utilities.status.ServerState;
import fedora.server.utilities.status.ServerStatusFile;

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

	private static final long serialVersionUID = 1L;
	
    private static Server s_server;

    private ServerStatusFile _status;

    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
    	log("Z1");
        String actionLabel = "server control";    	
        String action=request.getParameter("action");
        String requestInfo="Got controller '" + action + "' request from " + request.getRemoteAddr();
    	log("Z2 action=" + action);
        if (fedora.server.Debug.DEBUG) System.out.println(requestInfo);
        if (action==null) {
            throw new BadRequest400Exception(request, actionLabel, "no action", new String[0]);	
        } 

/*
        if (action.equals("startup")) {
        	actionLabel = "starting server";
        	boolean serverHasInstance = false;
        	log("Z3");
            try {
                serverHasInstance = Server.hasInstance(new File(System.getProperty("fedora.home")));
            	log("Z4");
            } catch (Throwable t) {
            	log("A " + t.getMessage() + " " + ((t.getCause() == null) ? "" : t.getCause().getMessage()));
                throw new InternalError500Exception(request, actionLabel, "error starting server", new String[0]);	
            }          
        	log("Z5");
            if (serverHasInstance) {
            	log("B");
                throw new InternalError500Exception(request, actionLabel, "server already started", new String[0]);	
            }	
        	log("Z6");
            try {
				s_server=Server.getInstance(new File(System.getProperty("fedora.home")));
			} catch (ServerInitializationException e) {
            	log("C " + e.getMessage() + " " + ((e.getCause() == null) ? "" : e.getCause().getMessage()));
                throw new InternalError500Exception(request, actionLabel, "error starting server", new String[0]);	
			} catch (ModuleInitializationException e) {
            	log("D " + e.getMessage() + " " + ((e.getCause() == null) ? "" : e.getCause().getMessage()));				
                throw new InternalError500Exception(request, actionLabel, "error starting server", new String[0]);	
			}
        	log("Z7");
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
*/
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
        if (action.equals("reloadPolicies")) {
        	actionLabel = "reloading repository policies";
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
			Authorization authModule = null;
			authModule = (Authorization) server.getModule("fedora.server.security.Authorization");
			if (authModule == null) {
                throw new InternalError500Exception(request, actionLabel, "error performing action2", new String[0]);	
			}
			try {				
				authModule.reloadPolicies(context);
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

    public void init() throws ServletException {
    	// make sure fedora.home is defined first
    	String fedoraHome;
    	fedoraHome = getInitParameter("fedora.home");
        if (fedoraHome == null) {
        	// try again
        	fedoraHome = System.getProperty("fedora.home");
        	if (fedoraHome == null) {
	            String msg = "FATAL ERROR: System property fedora.home is undefined";
	            System.out.println(msg);
	            throw new ServletException(msg);
        	}
        } else {
        	System.setProperty("fedora.home", fedoraHome);
        }
        File fedoraHomeDir = new File(fedoraHome);

        // get file for writing startup status
        try {
            _status = new ServerStatusFile(new File(fedoraHomeDir, "server"));
        } catch (Throwable th) {
            th.printStackTrace();
            throw new ServletException(th);
        }

        try {

            // Start the Fedora instance
            _status.append(ServerState.STARTING, "Starting Fedora Server instance");
            s_server = Server.getInstance(fedoraHomeDir);
            _status.append(ServerState.STARTED, null);
        } catch (Throwable th) {
            String msg = "Fedora startup failed";
            try {
                _status.appendError(ServerState.STARTUP_FAILED, th);
            } catch (Exception e) { }
            throw new ServletException(msg, th);
        }
    }

    public void destroy() {

        if (s_server == null) {
            System.out.println("Fedora Server not initialized; skipping shutdown");
        } else {
            try {
                _status.append(ServerState.STOPPING, "Shutting down Fedora Server and modules");
                s_server.shutdown(null);
                _status.append(ServerState.STOPPED, "Shutdown Successful");
            } catch (Throwable th) {
                try {
                    _status.appendError(ServerState.STOPPED_WITH_ERR, th);
                } catch (Exception e) { }
            }
            s_server = null;
        }
    }
    
	public static boolean log = false; 
	
	public final void log(String msg) {
		if (! log) return;
		System.err.println(msg);
	}
	
	public static boolean slog = false; 
	
	protected static final void slog(String msg) {
		if (! slog) return;
		System.err.println(msg);
	}
    
}
