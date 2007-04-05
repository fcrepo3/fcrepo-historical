/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */

package fedora.server.config.webxml;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.betwixt.io.BeanReader;
import org.apache.commons.betwixt.io.BeanWriter;
import org.apache.commons.betwixt.strategy.NamespacePrefixMapper;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class WebXML implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final String BETWIXT_MAPPING = "/fedora/server/config/webxml/webxml-mapping.xml";
	private String id;
	private String version;
	private String displayName;
	private List<Servlet> servlets;
	private List<ServletMapping> servletMappings;
	private List<Filter> filters;
	private List<FilterMapping> filterMappings;
	private List<SecurityConstraint> securityConstraints;
	private WelcomeFileList welcomeFileList;
	private List<ErrorPage> errorPages;
	private LoginConfig loginConfig;
	private List<SecurityRole> securityRoles;

	public WebXML() {
		servlets = new ArrayList<Servlet>();
		servletMappings = new ArrayList<ServletMapping>();
		filters = new ArrayList<Filter>();
		filterMappings = new ArrayList<FilterMapping>();
		securityConstraints = new ArrayList<SecurityConstraint>();
		errorPages = new ArrayList<ErrorPage>();
		securityRoles = new ArrayList<SecurityRole>();
	}
	
	public static WebXML getInstance() {
		return new WebXML();
	}
	
	public static WebXML getInstance(String webxml) {
		WebXML wx = null;
		BeanReader reader = new BeanReader();
		reader.getXMLIntrospector().getConfiguration().setAttributesForPrimitives(false);
		reader.getBindingConfiguration().setMapIDs(false);

		try {
			reader.registerMultiMapping(getBetwixtMapping());
			wx = (WebXML)reader.parse(webxml);			
		} catch (IntrospectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return wx;
	}
	
	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}
	
	public List<Servlet> getServlets() {
		return servlets;
	}
	
	public void addServlet(Servlet servlet) {
		servlets.add(servlet);
	}
	
	public List<ServletMapping> getServletMappings() {
		return servletMappings;
	}
	
	public void addServletMapping(ServletMapping servletMapping) {
		servletMappings.add(servletMapping);
	}
	
	public List<Filter> getFilters() {
		return filters;
	}
	
	public void addFilter(Filter filter) {
		filters.add(filter);
	}
	
	public List<FilterMapping> getFilterMappings() {
		return filterMappings;
	}
	
	public void addFilterMapping(FilterMapping filterMapping) {
		filterMappings.add(filterMapping);
	}
	
	public void removeFilterMapping(FilterMapping filterMapping) {
		filterMappings.remove(filterMapping);
	}
	
	public List<SecurityConstraint> getSecurityConstraints() {
		return securityConstraints;
	}
	
	public void addSecurityConstraint(SecurityConstraint securityConstraint) {
		securityConstraints.add(securityConstraint);
	}
	
	public void removeSecurityConstraint(SecurityConstraint securityConstraint) {
		securityConstraints.remove(securityConstraint);
	}
	
	public WelcomeFileList getWelcomeFileList() {
		return welcomeFileList;
	}
	
	public void setWelcomeFileList(WelcomeFileList welcomeFileList) {
		this.welcomeFileList = welcomeFileList;
	}
	
	public List<ErrorPage> getErrorPages() {
		return errorPages;
	}
	
	public void addErrorPage(ErrorPage errorPage) {
		errorPages.add(errorPage);
	}
	
	public LoginConfig getLoginConfig() {
		return loginConfig;
	}
	
	public void setLoginConfig(LoginConfig loginConfig) {
		this.loginConfig = loginConfig;
	}
	
	public List<SecurityRole> getSecurityRoles() {
		return securityRoles;
	}
	
	public void addSecurityRole(SecurityRole securityRole) {
		securityRoles.add(securityRole);
	}
	
	public void write(Writer outputWriter) throws IOException {
		//
		NamespacePrefixMapper nspm = new NamespacePrefixMapper();
		nspm.setPrefix("http://www.w3.org/2001/XMLSchema-instance", "xsi");
		nspm.setPrefix("http://java.sun.com/xml/ns/j2ee", "xmlns");
		//
		
		
		outputWriter.write("<?xml version=\"1.0\" ?>\n");
		
		BeanWriter beanWriter = new BeanWriter(outputWriter);
		beanWriter.getBindingConfiguration().setMapIDs(false);
		beanWriter.setWriteEmptyElements(false);
		beanWriter.enablePrettyPrint();
		try {
			beanWriter.getXMLIntrospector().register(getBetwixtMapping());
			beanWriter.getXMLIntrospector().getConfiguration().setPrefixMapper(nspm);
			beanWriter.write("web-app", this);
		} catch (IntrospectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		beanWriter.flush();
		beanWriter.close();
	}
	
	private static InputSource getBetwixtMapping() {
		return new InputSource(WebXML.class.getResourceAsStream(BETWIXT_MAPPING));
	}
}
