package fedora.utilities.install.container;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.dom4j.DocumentException;

import fedora.utilities.FileUtils;
import fedora.utilities.Zip;
import fedora.utilities.install.Distribution;
import fedora.utilities.install.Installer;
import fedora.utilities.install.InstallOptions;
import fedora.utilities.install.InstallationFailedException;

public class BundledTomcat extends Tomcat {
	public BundledTomcat(Distribution dist, InstallOptions options) {
		super(dist, options);
	}
	
	protected void installTomcat() throws InstallationFailedException {
		try {
			Zip.unzip(getDist().get(Distribution.TOMCAT), 
					System.getProperty("java.io.tmpdir"));
		} catch (IOException e) {
			throw new InstallationFailedException(e.getMessage(), e);
		}
		File f = new File(System.getProperty("java.io.tmpdir"), Distribution.TOMCAT_BASENAME);
		if (!FileUtils.move(f, getTomcatHome())) {
			throw new InstallationFailedException("Move to " + 
					getTomcatHome().getAbsolutePath() + " failed.");
		}
        Installer.setScriptsExecutable(new File(getTomcatHome(), "bin"));
	}
	
	protected void installServerXML() throws InstallationFailedException {
		try {
	        File distServerXML = new File(getConf(), "server.xml");
	        TomcatServerXML serverXML = new TomcatServerXML(distServerXML, getOptions());
	        serverXML.update();
	        serverXML.write(distServerXML.getAbsolutePath());
		} catch (IOException e) {
			throw new InstallationFailedException(e.getMessage(), e);
		} catch (DocumentException e) {
			throw new InstallationFailedException(e.getMessage(), e);
		}
	}
	
	protected void installKeystore() throws InstallationFailedException {
		String keystoreFile = getOptions().getValue(InstallOptions.KEYSTORE_FILE);
		if (keystoreFile == null) {
			// nothing to do
			return;
		}
		try {
			InputStream is;
			File keystore = new File(getConf(), Distribution.KEYSTORE);
	        if (keystoreFile.equals(InstallOptions.INCLUDED)) {
	        	is = getDist().get(Distribution.KEYSTORE);
	        } else {
	        	is = new FileInputStream(keystoreFile);
	        }
	        if (!FileUtils.copy(is, new FileOutputStream(keystore))) {
	        	throw new InstallationFailedException("Copy to " + 
	        			keystore.getAbsolutePath() + " failed.");
	        }
		} catch (IOException e) {
			throw new InstallationFailedException(e.getMessage(), e);
		}
	}
}
