package fedora.server.utilities;

import java.io.File;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.httpclient.methods.GetMethod;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import fedora.common.HttpClient;
import fedora.server.errors.GeneralException;

public class ServerUtility {
    public static final String FINE = "fine";
    public static final String FINER = "finer";
    public static final String FINEST = "finest";
    public static final String INFO = "info";
    public static final String WARNING = "warning";
    public static final String SEVERE = "severe";
    
    public static final String FEDORA_SERVER_HOST = "fedoraServerHost";
    public static final String FEDORA_SERVER_PORT = "fedoraServerPort";
    public static final String FEDORA_REDIRECT_PORT = "fedoraRedirectPort";
    public static final String FEDORA_SHUTDOWN_PORT = "fedoraShutdownPort";
    public static final String ADMIN_USER = "adminUser";
    public static final String ADMIN_PASSWORD = "adminPassword";
    private static final Properties readServerProperties(boolean httpRequired, boolean httpsRequired) 
    	throws Exception {
       	Properties properties = new Properties();    	
        String fedoraHome=System.getProperty("fedora.home");
        if (fedoraHome==null) {
        	throw new Exception("ERROR: fedora.home system property not set.");            	
        }        	
        File fedoraHomeDir=new File(fedoraHome);
        File fcfgFile=new File(fedoraHomeDir, "server/config/fedora.fcfg");
        DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder=factory.newDocumentBuilder();
        Element rootElement=builder.parse(fcfgFile).getDocumentElement();
        NodeList params=rootElement.getElementsByTagName("param");
        for (int i=0; i<params.getLength(); i++) {
            Node nameNode=params.item(i).getAttributes().getNamedItem("name");
            Node valueNode=params.item(i).getAttributes().getNamedItem("value");
    		if (FEDORA_SERVER_PORT.equals(nameNode.getNodeValue())
            ||  FEDORA_REDIRECT_PORT.equals(nameNode.getNodeValue())
            ||  FEDORA_SHUTDOWN_PORT.equals(nameNode.getNodeValue())			
            ||  FEDORA_SERVER_HOST.equals(nameNode.getNodeValue())
            ||  ADMIN_USER.equals(nameNode.getNodeValue())
            ||  ADMIN_PASSWORD.equals(nameNode.getNodeValue())) {
        		properties.put(nameNode.getNodeValue(),valueNode.getNodeValue());            			
    		}
        }
        if ((! properties.containsKey(FEDORA_SERVER_HOST))) {
        	throw new Exception("fedora.fcfg missing " + "http host");            	
        }                
        if (httpRequired && ! properties.containsKey(FEDORA_SERVER_PORT)) {
        	throw new Exception("fedora.fcfg missing " + "http port");        	
        }
        if (httpsRequired && ! properties.containsKey(FEDORA_REDIRECT_PORT)) {
        	throw new Exception("fedora.fcfg missing " + "http port");            	
        }
        if (! properties.containsKey(FEDORA_SHUTDOWN_PORT)) {
        	throw new Exception("fedora.fcfg missing " + "shutdown port");            	
        }        
        if ((! properties.containsKey(ADMIN_USER))) {
        	throw new Exception("fedora.fcfg missing " + "admin user");            	
        }
        if ((! properties.containsKey(ADMIN_PASSWORD))) {
        	throw new Exception("fedora.fcfg missing " + "admin passwd");            	
        }            
    	return properties;
    }

    public static final Properties getServerProperties() {
    	return serverProperties;
    }

    
    private static Properties serverProperties = null;
    static {
    	try {
			serverProperties = readServerProperties(false, false);
		} catch (Exception e) {
	        slog(FINEST, "problem during static read of server properties, "+e.getMessage());
		}
    }

    public static final String HTTP = "http";
    public static final String HTTPS = "https";
    
    public static final String getPort(String protocol) throws GeneralException {
    	String port = null;
    	if (! HTTP.equals(protocol) && ! HTTPS.equals(protocol)) {
        	slog(FINEST, "bad protocol parm = "+protocol);
    		throw new GeneralException("bad protocol parm");    	
    	}
    	slog(FINEST, "serverProperties = "+serverProperties);
    	slog(FINEST, "HTTP.equals(protocol) = "+HTTP.equals(protocol));    	
    	slog(FINEST, "HTTPS.equals(protocol) = "+HTTPS.equals(protocol));    	
    	slog(FINEST, "FEDORA_SERVER_PORT = "+FEDORA_SERVER_PORT);    	
    	slog(FINEST, "FEDORA_REDIRECT_PORT = "+FEDORA_REDIRECT_PORT); 
    	slog(FINEST, "serverProperties.containsKey(FEDORA_SERVER_PORT) = "+serverProperties.containsKey(FEDORA_SERVER_PORT));    	
    	slog(FINEST, "serverProperties.containsKey(FEDORA_REDIRECT_PORT) = "+serverProperties.containsKey(FEDORA_REDIRECT_PORT));    	
    	if (HTTP.equals(protocol) && serverProperties.containsKey(FEDORA_SERVER_PORT)) {	
    		port = (String) serverProperties.get(FEDORA_SERVER_PORT);
    	} else if (HTTPS.equals(protocol) && serverProperties.containsKey(FEDORA_REDIRECT_PORT)) {	
    		port = (String) serverProperties.get(FEDORA_REDIRECT_PORT);
    	} else {
        	slog(FINEST, "specified port not configured");
    		throw new GeneralException("specified port not configured");    		
    	}
    	slog(FINEST, protocol+"=>"+port);
    	return port;
    }
    
    public static final ProtocolPort getProtocolPort(String preferredProtocol, String fallbackProtocol) throws GeneralException {
    	String protocol = null;
    	String port = getPort(preferredProtocol);
    	if (port != null) {
    		protocol = preferredProtocol;
    	} else if (fallbackProtocol != null) {
    		port = getPort(fallbackProtocol);
        	if (port != null) {
        		protocol = fallbackProtocol;
        	}
    	}    	
    	slog(FINEST, "protocol="+protocol+"port="+port);
    	return new ProtocolPort(protocol, port);
    }
    
    public static boolean pingServletContainer(String path, int secondsTimeout, int maxConnectionAttemptsPerUrl) throws GeneralException {
        boolean pingsOk = false;
        try {
        	slog(FINEST, "getServerProperties()="+getServerProperties());
        	ProtocolPort protocolPort = getProtocolPort(HTTP, HTTPS);
    		slog(FINEST, "protocolPort="+protocolPort);
    		slog(FINEST, "protocolPort.getProtocol()="+protocolPort.getProtocol());
    		slog(FINEST, "protocolPort.getPort()="+protocolPort.getPort());
    		slog(FINEST, "serverProperties.get(FEDORA_SERVER_HOST)="+serverProperties.get(FEDORA_SERVER_HOST));
    		slog(FINEST, "path="+path);    		
        	HttpClient client = new HttpClient(protocolPort.getProtocol(), (String) getServerProperties().get(FEDORA_SERVER_HOST), protocolPort.getPort(), path);
    		slog(FINEST, "client="+client);
        	GetMethod getMethod = client.doNoAuthnGet(1000 * secondsTimeout, 25, maxConnectionAttemptsPerUrl);
    		slog(FINEST, "getMethod="+getMethod);
    		slog(FINEST, "getMethod.getStatusCode()="+getMethod.getStatusCode());    		
        	pingsOk = (getMethod.getStatusCode() == java.net.HttpURLConnection.HTTP_OK);
    		slog(FINEST, "pingsOk="+pingsOk);
        } catch (Exception e) {			
        	throw new GeneralException(slog(FINEST, "op failure pinging fedora server"), e);
		}
        return pingsOk;    	
    }
    
	public static final int MAX_CONNECTION_ATTEMPTS_PER_URL = 15;   
    public static boolean pingServletContainerStartup(String path, int secondsTimeout) throws GeneralException {
    	return pingServletContainer(path, secondsTimeout, MAX_CONNECTION_ATTEMPTS_PER_URL);
    }
    
    public static boolean pingServletContainerRunning(String path, int secondsTimeout) throws GeneralException {
    	return pingServletContainer(path, secondsTimeout, 1);
    }

    
    private static final void serverAction (String action, String protocol, String optionalUsername, String optionalPassword) throws Exception {
   		slog(FINEST, "SC:call HttpClient()...");
  		HttpClient client = new HttpClient(protocol, 
  				ServerUtility.getServerProperties().getProperty(ServerUtility.FEDORA_SERVER_HOST), 
  				ServerUtility.getServerProperties().getProperty( "http".equals(protocol) ? ServerUtility.FEDORA_SERVER_PORT : ServerUtility.FEDORA_REDIRECT_PORT),
  				"/fedora/management/control?action=" + action
  				);
   		slog(FINEST, "...SC:call HttpClient()"); 
   		slog(FINEST, "SC:call HttpClient.doAuthnGet()...");        		
  		client.doAuthnGet(20000, 25,
  			(optionalUsername == null) ? ServerUtility.getServerProperties().getProperty(ServerUtility.ADMIN_USER) : optionalUsername,
  			(optionalPassword == null) ? ServerUtility.getServerProperties().getProperty(ServerUtility.ADMIN_PASSWORD) : optionalPassword, 
  			ServerUtility.MAX_CONNECTION_ATTEMPTS_PER_URL
  		);
   		slog(FINEST, "...SC:call HttpClient.doAuthnGet()");		      		
   		slog(FINEST, "SC:call HttpClient.getLineResponse()...");
  		String response = client.getLineResponseUrl();
        System.out.println(response);    	
    }
    
    private static final String STARTUP = "startup";
    private static final String SHUTDOWN = "shutdown";
    private static final String STATUS = "status";
    
    public static final void startup(String protocol) throws Exception {
    	startup(protocol, null, null);
    }

    public static final void shutdown(String protocol) throws Exception {
    	shutdown(protocol, null, null);
    }
    
    public static final void status(String protocol) throws Exception {
    	status(protocol, null, null);
    }
    
    public static final void startup(String protocol, String optionalUsername, String optionalPassword) throws Exception {
    	serverAction (STARTUP, protocol, optionalUsername, optionalPassword);    	
    }

    public static final void shutdown(String protocol, String optionalUsername, String optionalPassword) throws Exception {
    	serverAction (SHUTDOWN, protocol, optionalUsername, optionalPassword);    	
    }

    public static final void status(String protocol, String optionalUsername, String optionalPassword) throws Exception {
    	serverAction (STATUS, protocol, optionalUsername, optionalPassword);    	
    }    
    
    private static final String USAGE = "USAGE for ServerController.main(): startup|shutdown|status [http|https] [username] [passwd]";

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
        	throw new Exception(USAGE);
        }
        String action = args[0];
        String protocol = args.length > 1 ? args[1] : "http";        
        if (! "http".equals(protocol) && ! "https".equals(protocol)) {
        	throw new Exception(USAGE);
        }
        String optionalUsername = null;
        String optionalPassword = null;            
        if (args.length > 2) {
        	if (args.length == 3) {
            	throw new Exception(USAGE);
        	}
        	optionalUsername = args[2];
        	optionalPassword = args[3];
        }
        if (STARTUP.equals(action)) {
        	startup(protocol, optionalUsername, optionalPassword);
        } else if (SHUTDOWN.equals(action)) {
        	shutdown(protocol, optionalUsername, optionalPassword);
        } else if (STATUS.equals(action)) {
        	status(protocol, optionalUsername, optionalPassword);
        } else {
        	throw new Exception(USAGE);            	
        }
    }    
    
    private static boolean slog = false;

    public static final String slog(String level, String msg) {
    	if (slog) {
    		System.err.println(level + ": " + msg);
    	}
		return msg;
    }
    


}
