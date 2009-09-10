/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.utilities.install.container;

import java.io.File;

import fedora.utilities.install.InstallOptions;

/**
 * Options for the Fedora web.xml file.
 *
 * @author Edwin Shin
 */
public class WebXMLOptions {

    private boolean apiaAuth;

    private boolean apiaSSL;

    private boolean apimSSL;

    private File fedoraHome;

    public WebXMLOptions() {
    }

    public WebXMLOptions(InstallOptions installOptions) {
        apiaAuth =
                installOptions
                        .getBooleanValue(InstallOptions.APIA_AUTH_REQUIRED,
                                         false);
        apiaSSL =
                installOptions
                        .getBooleanValue(InstallOptions.APIA_SSL_REQUIRED,
                                         false);
        apimSSL =
                installOptions
                        .getBooleanValue(InstallOptions.APIM_SSL_REQUIRED,
                                         false);
        fedoraHome =
                new File(installOptions.getValue(InstallOptions.FEDORA_HOME));
    }

    public boolean requireApiaAuth() {
        return apiaAuth;
    }

    public void setApiaAuth(boolean apiaAuth) {
        this.apiaAuth = apiaAuth;
    }

    public boolean requireApiaSSL() {
        return apiaSSL;
    }

    public void setApiaSSL(boolean apiaSSL) {
        this.apiaSSL = apiaSSL;
    }

    public boolean requireApimSSL() {
        return apimSSL;
    }

    public void setApimSSL(boolean apimSSL) {
        this.apimSSL = apimSSL;
    }

    public File getFedoraHome() {
        return fedoraHome;
    }

    public void setFedoraHome(File fedoraHome) {
        this.fedoraHome = fedoraHome;
    }
}