package fedora.server;

import fedora.server.errors.ServerInitializationException;
import fedora.server.errors.ServerShutdownException;
import fedora.server.errors.ModuleInitializationException;
import fedora.server.utilities.ServerUtility;

import fedora.logging.DatingFileHandler;
import fedora.logging.SimpleXMLFormatter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

/**
 *
 * <p><b>Title:</b> BasicServer.java</p>
 * <p><b>Description:</b> </p>
 *
 * @author cwilper@cs.cornell.edu
 * @version $Id$
 */
public class BasicServer
        extends Server {

    private File logDir;

    public BasicServer(Element rootElement, File fedoraHomeDir)
            throws ServerInitializationException,
                   ModuleInitializationException {
        super(rootElement, fedoraHomeDir);
    }

    public void initServer()
            throws ServerInitializationException {

        String fedoraServerHost = null;
        String fedoraServerPort = null;

        initLogger();

        // fedoraServerHost (required)
        fedoraServerHost=getParameter("fedoraServerHost");
        if (fedoraServerHost==null) {
            throw new ServerInitializationException("Parameter fedoraServerHost "
                + "not given, but it's required.");
        }
        // fedoraServerPort (required)
        fedoraServerPort=getParameter("fedoraServerPort");
        if (fedoraServerPort==null) {
            throw new ServerInitializationException("Parameter fedoraServerPort "
                + "not given, but it's required.");
        }
        System.out.println("Fedora Version: " + VERSION_MAJOR + "." + VERSION_MINOR);
        System.out.println("Fedora Build: " + BUILD_NUMBER);
        System.out.println("Server Host Name: " + fedoraServerHost);
        System.out.println("Server Port: " + fedoraServerPort);
        
        logInfo("Fedora Version: " + VERSION_MAJOR + "." + VERSION_MINOR);
        logInfo("Fedora Build: " + BUILD_NUMBER);

        // debug (optional, default = false)
        String debugString = getParameter("debug");
        String offOrOn = "OFF";
        if ( debugString != null) {
            debugString = debugString.toLowerCase();
            if (debugString.equals("true")
                    || debugString.equals("yes")
                    || debugString.equals("on")) {
                offOrOn = "ON";
            }
        }
        System.out.println("Debugging: " + debugString);
        if (offOrOn.equals("ON")) {
            fedora.server.Debug.DEBUG = true;
        }
    }

    private int getLoggerIntParam(String paramName)
            throws ServerInitializationException {
        String s=getParameter(paramName);
        int ret;
        if (s==null) {
            ret=0;
            logConfig(paramName + " not specified, defaulting to 0 (infinite)");
        } else {
            try {
                ret=Integer.parseInt(s);
                if (ret<0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException nfe) {
                throw new ServerInitializationException(paramName
                        + " must be an integer from 0 to " + Integer.MAX_VALUE);
            }
            String retString;
            if (ret==0) {
                retString="0 (infinite)";
            } else {
                retString="" + ret;
            }
            logConfig(paramName + " specified = " + retString + ", ok.");
        }
        return ret;
    }

    private void initLogger()
            throws ServerInitializationException {
        Logger logger=Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        int maxSize=getLoggerIntParam("log_max_size");
        int maxFiles=getLoggerIntParam("log_max_files");
        int maxDays=getLoggerIntParam("log_max_days");
        int flushThreshold=getLoggerIntParam("log_flush_threshold");
        String levelParam=getParameter("log_level");
        Level logLevel;
        if (levelParam==null) {
            logConfig("log_level not specified, defaulting to config.");
            logLevel=Level.CONFIG;
        } else {
            if (levelParam.equalsIgnoreCase("severe")) {
                logLevel=Level.SEVERE;
            } else if (levelParam.equalsIgnoreCase("warning")) {
                logLevel=Level.WARNING;
            } else if (levelParam.equalsIgnoreCase("info")) {
                logLevel=Level.INFO;
            } else if (levelParam.equalsIgnoreCase("config")) {
                logLevel=Level.CONFIG;
            } else if (levelParam.equalsIgnoreCase("fine")) {
                logLevel=Level.FINE;
            } else if (levelParam.equalsIgnoreCase("finer")) {
                logLevel=Level.FINER;
            } else if (levelParam.equalsIgnoreCase("finest")) {
                logLevel=Level.FINEST;
            } else {
                throw new ServerInitializationException("log_level was "
                        + "specified as " + levelParam + ".  If specified, "
                        + "log_level must be severe, warning, info, config, "
                        + "fine, finer, or finest.");
            }
            logConfig("log_level specified = " + levelParam + ", ok.");
        }
        logger.setLevel(logLevel);

        String dirParam=getParameter("log_dir");
        File logDir;
        if (dirParam==null) {
            logDir=new File(getHomeDir(), Server.LOG_DIR);
            logConfig("log_dir not specified, defaulting to " + logDir);
        } else {
            if (dirParam.startsWith(File.separator)) {
                logDir=new File(dirParam);
            } else {
                logDir=new File(getHomeDir(), dirParam);
            }
            logConfig("log_dir specified = " + dirParam + ", "
                    + "using " + logDir);
        }
        if (!(logDir.exists() && logDir.isDirectory())) {
            throw new ServerInitializationException("log_dir is not an "
                    + "existing directory.");
        }
        String ext=".log";
        DatingFileHandler fh=null;
        try {
            fh=new DatingFileHandler(logDir, maxSize, maxDays, maxFiles, ext,
                    new SimpleXMLFormatter(true, "UTF-8"), flushThreshold);
            fh.setLevel(Level.FINEST);
        } catch (IOException ioe) {
            throw new ServerInitializationException("Could not initialize "
                    + "logger due to I/O error: " + ioe.getMessage());
        }
        logger.addHandler(fh);
        logFinest("Logger initialized. Switching to file-based log...");
        setLogger(logger);
    }

    public void shutdownServer()
            throws ServerShutdownException {
        closeLogger();
    }

    /**
     * Gets the names of the roles that are required to be fulfilled by
     * modules specified in this server's configuration file.
     *
     * @return String[] The roles.
     */
    public String[] getRequiredModuleRoles() {
        return new String[] {"fedora.server.storage.DOManager"};
    }

    public static void main(String[] args) throws Exception {
        // prepare for an in-tomcat-instance run of the server by
        // reading the adminPassword and fedoraServerPort from fedora.fcfg,
        // and sending it to <FEDORA_HOME>/<tomcatConfDir>
        String fedoraHome=System.getProperty("fedora.home");
        String tomcatDir=System.getProperty("tomcat.dir"); // the directory name, not full path
        String tomcatConfDir = "server/" + tomcatDir + "/conf/";
        if (fedoraHome==null || fedoraHome.equals("")) {
            System.out.println("ERROR: fedora.home property not set.");
        } else if (tomcatDir == null || tomcatDir.equals("")) {
            System.out.println("ERROR: tomcat.dir property not set.");
        } else {
            File fedoraServerHomeDir=new File(new File(fedoraHome), "server");
            File fcfgFile=new File(fedoraServerHomeDir, "config/fedora.fcfg");
            if (!fcfgFile.exists()) {
                System.out.println("ERROR: fedora.fcfg not found in FEDORA_HOME/server/config/ -- is fedora.home set properly?");
            } else {
           		Properties serverProperties = ServerUtility.getServerProperties(true, true);            	
                File serverTemplate=new File(fedoraHome, tomcatConfDir + "server_fedoraTemplate.xml");
                BufferedReader in=new BufferedReader(new FileReader(serverTemplate));
                FileWriter out=new FileWriter(new File(fedoraHome, tomcatConfDir + "server.xml"));
                String nextLine="";
                while (nextLine!=null) {
                    nextLine=in.readLine();
                    if (nextLine!=null) {
                        if (nextLine.indexOf("#1")>0) {
                            nextLine = nextLine.replaceAll("#1", serverProperties.getProperty(ServerUtility.FEDORA_SERVER_PORT));
                        }
                        if (nextLine.indexOf("#2")>0) {
                            nextLine = nextLine.replaceAll("#2", serverProperties.getProperty(ServerUtility.FEDORA_SHUTDOWN_PORT));
                        }
                        if (nextLine.indexOf("#3")>0) {
                            nextLine = nextLine.replaceAll("#3", serverProperties.getProperty(ServerUtility.FEDORA_REDIRECT_PORT));
                        }
                        out.write(nextLine+"\n");
                    }
                }
                in.close();
                out.close();

                File usersTemplate=new File(fedoraHome, tomcatConfDir + "tomcat-users_fedoraTemplate.xml");
                in=new BufferedReader(new FileReader(usersTemplate));
                out=new FileWriter(new File(fedoraHome, tomcatConfDir + "tomcat-users.xml"));
                nextLine="";
                while (nextLine!=null) {
                    nextLine=in.readLine();
                    if (nextLine!=null) {
                        if (nextLine.indexOf("#")==-1) {
                            out.write(nextLine);
                        } else {
                            for (int i=0; i<nextLine.length(); i++) {
                                char c=nextLine.charAt(i);
                                if (c=='#') {
                                    out.write(serverProperties.getProperty(ServerUtility.ADMIN_PASSWORD));
                                } else if (c=='%') {
                                    out.write(serverProperties.getProperty(ServerUtility.ADMIN_USER));                                	
                                } else {
                                    out.write(c);
                                }
                            }
                        }
                        out.write("\n");
                    }
                }
                in.close();
                out.close();
            }
        }
    }

}
