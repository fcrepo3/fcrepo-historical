package fedora.server.utilities;

import java.util.Map;

import fedora.server.Module;
import fedora.server.Server;
import fedora.server.errors.ModuleInitializationException;

public class ThreadMonitorModule
        extends Module
        implements ThreadMonitor {
        
    ThreadMonitorImpl m_wrappedMonitor;
    
    public ThreadMonitorModule(Map params, Server server, String role) 
            throws ModuleInitializationException {
        super(params, server, role);
    }
    
    public void initModule() 
            throws ModuleInitializationException {
        String active=getParameter("active");
        String pollInterval=getParameter("pollInterval");
        if (active!=null && (active.toLowerCase().equals("yes") || active.toLowerCase().equals("true"))) {
            if (pollInterval==null) {
                logConfig("pollInterval unspecified, defaulting to 10,000 milliseconds.");
                pollInterval="10000";
            }
            try {
                int pi=Integer.parseInt(pollInterval);
                if (pi<0) {
                    throw new NumberFormatException();
                }
                m_wrappedMonitor=new ThreadMonitorImpl(pi, this);
            } catch (NumberFormatException nfe) {
                throw new ModuleInitializationException("Badly formed parameter: pollInterval: must be a nonnegative integer.", getRole());
            }
        }
    }
    
    public void shutdownModule() {
        m_wrappedMonitor.requestStop();
    }

    public void run() {
        m_wrappedMonitor.run();
    }
    
    public void requestStop() {
        m_wrappedMonitor.requestStop();
    }
    
}