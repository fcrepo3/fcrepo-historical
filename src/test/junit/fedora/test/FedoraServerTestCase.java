package fedora.test;

import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;

import fedora.client.*;
import fedora.server.config.Configuration;
import fedora.server.config.ServerConfiguration;

/**
 * Base class for JUnit tests that need a running Fedora server.
 * 
 * 
 * @author Edwin Shin
 */
public abstract class FedoraServerTestCase extends FedoraTestCase {

    private FedoraServerTestSetup testSetup;
    private File m_configDir;
    
    public FedoraServerTestCase() {
        super();
        initConfigDir();
    }
    
    public FedoraServerTestCase(String name) {
        super(name);
        initConfigDir();
    }

    private void initConfigDir() {
        String testHome = System.getProperty(PROP_TEST_HOME);
        if (testHome == null) {
            throw new RuntimeException("Required system property not set: " 
                    + PROP_TEST_HOME);
        }

        //m_configDir = new File(new File(testHome), 
        //                       this.getClass().getName().replaceAll("\\.", "/"));

		m_configDir = new File(new File(testHome), 
							   (this.getClass().getName().replaceAll("\\.", "/")) + "CFG");
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(FedoraServerTestCase.class);
    }

    public void setUp() throws Exception {
        // FedoraServerTestSetup starts a Fedora server if needed
        testSetup = new FedoraServerTestSetup(this, this.getClass().getName());
        testSetup.setUp();
    }

    public void tearDown() throws Exception {
        // FedoraServerTestSetup stops a Fedora server if we started it
        if (testSetup != null) {
            testSetup.tearDown();
        }
    }
   
    public static ServerConfiguration getServerConfiguration() {
        try {
            return FedoraServerTestSetup.getServerConfiguration();
        } catch(Exception e) {
            fail(e.getMessage());
            return null;
        }
    }
    
    public static String getBaseURL() {
        return getProtocol() + "://" + getHost() + ":" + getPort() + "/fedora";  
    }
    
    public static String getHost() {
        return getServerConfiguration().getParameter("fedoraServerHost").getValue();
    }
    
    public static String getPort() {
        String port=null;
        if(getProtocol().equals("http")) {
            port = getServerConfiguration().getParameter("fedoraServerPort").getValue();
        } else {
            port = getServerConfiguration().getParameter("fedoraRedirectPort").getValue();
        }
        System.out.println("***** Port based on SSL setting in beSecurity: "+port);
        return port;
    }
    
    public static String ssl = "http";

    // hack to dynamically set protocol based on settings in beSecurity
    // Settings for fedoraInternalCall-1 should have callSSL=true when server is secure    
    public static String getProtocol() {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(BESECURITY_PATH)));
            String line = null;
            while ((line = br.readLine()) != null) {
                if(line.indexOf("role=\"fedoraInternalCall-1\"") > 0    &&
                        line.indexOf("callSSL=\"true\"") > 0) {
                        ssl = "https";
                        break;
                }
            }
        } catch (Exception e) {
            System.out.println("beSecurity file Not found: "+BESECURITY_PATH);
        } finally {
            try {
                if( br!=null ) {
                    br.close();
                    br=null;
                }
            } catch (Exception e) {
                System.out.println("Unable to close BufferdReader");
            }
        }
        System.out.println("**** SSL setting from beSecurity: "+ssl); 
        return ssl;
    }
    
    public static String getUsername() {
        return getServerConfiguration().getParameter("adminUsername").getValue();
    }
    
    public static String getPassword() {
        return getServerConfiguration().getParameter("adminPassword").getValue();
    }

    public void usePolicies(String dirName) throws Exception {
        File policyBaseDir = new File(m_configDir, dirName);
        System.out.println("Using policies from " + policyBaseDir.getPath());

        // currently just blows away existing policies and replaces them with
        // whatever's in dirName

        System.out.println("Replacing policies...");
        replacePolicies(new File(policyBaseDir, "repository-policies"), 
                        "REPOSITORY-POLICIES-DIRECTORY");
        replacePolicies(new File(policyBaseDir, "object-policies"), 
                        "OBJECT-POLICIES-DIRECTORY");
        replacePolicies(new File(policyBaseDir, "surrogate-policies"), 
                        "SURROGATE-POLICIES-DIRECTORY");
        replacePolicies(new File(policyBaseDir, "repository-policies-generated-by-policyguitool"), 
                        "REPOSITORY-POLICY-GUITOOL-POLICIES-DIRECTORY");

        File backendSecurityReplacement = new File(dirName, "beSecurity.xml");
        if (backendSecurityReplacement.exists()) {
            File currentBackendSecurity = new File(BESECURITY_PATH);
            currentBackendSecurity.delete();
            FedoraServerTestSetup.copy(backendSecurityReplacement, currentBackendSecurity);
        }
       
        System.out.println("Telling server to reload policies...");
        FedoraClient client = new FedoraClient(getBaseURL(), getUsername(), getPassword());
        client.reloadPolicies();
    }

    private final void showFiles (File dir, String label) {
    	System.out.println(dir.getPath() + " " + label);
    	String[] files = dir.list();
    	for (int i = 0; i < files.length; i++) {
    		System.out.println("\t" + files[i]);
    	}
    }
    
    private void replacePolicies(File fromDir, String toDirProp) throws Exception {
        Configuration config = getServerConfiguration().getModuleConfiguration("fedora.server.security.Authorization");
        File toDir = new File(config.getParameter(toDirProp).getValue());
        if (toDir.exists()) {
        	showFiles(toDir, "before clearing");
            clearDir(toDir);
        	showFiles(toDir, "after clearing");
            if (fromDir.exists()) {
            	System.out.println(fromDir.getPath() + " exists");
                copyFiles(fromDir, toDir);
            }
        	showFiles(toDir, "after copying");
        }
    }

    private static void clearDir(File dir) throws Exception {
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            files[i].delete();
        }
    }

    private static void copyFiles(File fromDir, File toDir) throws Exception {
        File[] files = fromDir.listFiles();
        for (int i = 0; i < files.length; i++) {
            FedoraServerTestSetup.copy(files[i], new File(toDir, files[i].getName()));
        }
    }

}
