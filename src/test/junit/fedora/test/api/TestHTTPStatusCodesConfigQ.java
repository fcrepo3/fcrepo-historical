
package fedora.test.api;

import junit.framework.Test;
import junit.framework.TestSuite;

import fedora.test.FedoraServerTestCase;

import static fedora.test.api.TestHTTPStatusCodes.RI_SEARCH_PATH;
import static fedora.test.api.TestHTTPStatusCodes.checkError;

/**
 * HTTP status code tests to be run when API-A authentication is off and the
 * resource index is disabled.
 *
 * @author Chris Wilper
 */
public class TestHTTPStatusCodesConfigQ
        extends FedoraServerTestCase {

    public static Test suite() {
        TestSuite suite = new TestSuite("TestHTTPStatusCodesConfigQ TestSuite");
        suite.addTestSuite(TestHTTPStatusCodesConfigQ.class);
        return suite;
    }

    //---
    // API-A Lite: riSearch
    //---

    public void testRISearch_Disabled() throws Exception {
        checkError(RI_SEARCH_PATH);
    }

}
