package fedora.client;

import java.net.URL;
import java.net.MalformedURLException;
import javax.xml.rpc.ServiceException;

import fedora.server.management.FedoraAPIM;
import fedora.server.management.FedoraAPIMServiceLocator;

/**
 *
 * <p><b>Title:</b> APIMStubFactory.java</p>
 * <p><b>Description:</b> </p>
 *
 *
 * @author cwilper@cs.cornell.edu
 * @version $Id$
 */
public abstract class APIMStubFactory {

	public static FedoraAPIM getStub(String protocol, String host, int port, String username, String password)
			throws MalformedURLException, ServiceException {
        	
		FedoraAPIMServiceLocator locator=new FedoraAPIMServiceLocator(username, password);

		//SDP - HTTPS support added
		URL ourl = null;
		URL nurl = null;  
		if (protocol.equalsIgnoreCase("http")) { 
			ourl=new URL(locator.getFedoraAPIMPortSOAPHTTPAddress());
			nurl=rewriteServiceURL(ourl, protocol, host, port);
			if (Administrator.INSTANCE==null) {
				// if running without Administrator, don't wrap it with the statusbar stuff
				return locator.getFedoraAPIMPortSOAPHTTP(nurl);
			} else {
				return new APIMStubWrapper(locator.getFedoraAPIMPortSOAPHTTP(nurl));
			}
		} else if (protocol.equalsIgnoreCase("https")){
			ourl=new URL(locator.getFedoraAPIMPortSOAPHTTPSAddress());
			nurl=rewriteServiceURL(ourl, protocol, host, port);
			if (Administrator.INSTANCE==null) {
				// if running without Administrator, don't wrap it with the statusbar stuff
				return locator.getFedoraAPIMPortSOAPHTTPS(nurl);
			} else {
				return new APIMStubWrapper(locator.getFedoraAPIMPortSOAPHTTPS(nurl));
			}
		} else {
			throw new javax.xml.rpc.ServiceException("The protocol" + " " + protocol 
				+ " is not supported by this service.");
		}
	}
	private static URL rewriteServiceURL(URL ourl, String protocol, String host, int port) 
		throws MalformedURLException, ServiceException {

		StringBuffer nurl=new StringBuffer();    
		if (protocol.equalsIgnoreCase("http")) { 
			nurl.append("http://"); 
		} else if (protocol.equalsIgnoreCase("https")){
			nurl.append("https://"); 
		} else {
			throw new javax.xml.rpc.ServiceException("The protocol" + " " + protocol 
				+ " is not supported by this service.");
		}

		nurl.append(host);
		nurl.append(':');
		nurl.append(port);
		nurl.append(ourl.getPath());
		if ((ourl.getQuery()!=null) && (!ourl.getQuery().equals("")) ) {
			nurl.append('?');
			nurl.append(ourl.getQuery());
		}
		if ((ourl.getRef()!=null) && (!ourl.getRef().equals("")) ) {
			nurl.append('#');
			nurl.append(ourl.getRef());
		}
		return new URL(nurl.toString());
	}


}