package fedora.server.utilities;

import java.util.Map;

import fedora.server.Module;
import fedora.server.Server;
import fedora.server.errors.ModuleInitializationException;

/**
 *
 * <p><b>Title:</b> ThreadMonitorModule.java</p>
 * <p><b>Description:</b> </p>
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
 * <p>The entire file consists of original code.  Copyright &copy; 2002, 2003 by The
 * Rector and Visitors of the University of Virginia and Cornell University.
 * All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 *
 * @author cwilper@cs.cornell.edu
 * @version $Id$
 */
public class ThreadMonitorModule
        extends Module
        implements ThreadMonitor {

    private ThreadMonitorImpl m_wrappedMonitor;
    private boolean m_active=false;

    public ThreadMonitorModule(Map params, Server server, String role)
            throws ModuleInitializationException {
        super(params, server, role);
    }

    public void initModule()
            throws ModuleInitializationException {
        String active=getParameter("active");
        String pollInterval=getParameter("pollInterval");
        if (active!=null && (active.toLowerCase().equals("yes") || active.toLowerCase().equals("true"))) {
            m_active=true;
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
        if (m_active) {
            m_wrappedMonitor.requestStop();
        }
    }

    public void run() {
        if (m_active) {
            m_wrappedMonitor.run();
        }
    }

    public void requestStop() {
        if (m_active) {
            m_wrappedMonitor.requestStop();
        }
    }

}