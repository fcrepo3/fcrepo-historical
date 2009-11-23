-------------------------------------------------------------------
               Fedora Release 3.3 - December 2009
-------------------------------------------------------------------
This is a full source code release of Fedora.  Before using this
software, you must read and agree to the license, found under
resources/doc/license/license.html.  Documentation can be found for
online browsing or download at http://fedora-commons.org/go/fcr30

Building Fedora
===============
To build the executable installer, make sure you have maven2
installed and enter the following:

  mvn install -P fedora-installer

This generates fedora-installer.jar, which can be found in
/installer/target

Running Unit Tests
=====================

  mvn install -Dintegration.test.skip=true

Running System Tests
====================
The system tests consist of functional "black box" tests to be
executed against a running Fedora server.  These tests are divided
into top-level suites, where each suite is intended to be run with
the Fedora server configured in a specific way.

  [fedora.test.AllSystemTestsConfigA]
    When running this suite, the server should be configured
    with API-A authentication turned OFF.

  [fedora.test.AllSystemTestsConfigB]
    When running this suite, the server should be configured
    with API-A authentication turned ON, with the
    Resource Index, REST api, and Messaging modules enabled.
    
  [fedora.test.AllSystemTestsConfigQ]
    When running this suite, the server should be configured
    with the default options provided by 'quick install'.
    It can be used to verify the successful installation of 'quick install'.
    
These tests do not depend on external hosts and can therefore be
run without external network access.

To execute a test suite, make sure the server has been started[*] and 
that $FEDORA_HOME points to the correct directory. Then enter:

  mvn integration-test -P config[A|B|Q]

By default, each test will run using the demo objects in
FOXML format.  To run the same tests using the demo objects
in METS, Atom, or Atom Zip format, add one of the following to 
the line above:

  -Ddemo.format=mets
  -Ddemo.format=atom
  -Ddemo.format=atom-zip

There are some system tests that are not included in the system
test suites due to the time required to execute the test,
the following tests fall into that category:

  [fedora.test.integration.TestLargeDatastreams]
    This test adds a 5GB datastream through API-M, then 
    retrieves it via API-A and API-A-Lite. When running 
    this test, the server should be configured to allow
    non-SSL access to API-M and API-A. This test has no
    dependencies on external hosts and can therefore be 
    run without external internet access.
  
  To run this test, make sure the server has been started[*] 
  Then, from within the integrationtest sub-module enter:
    
    mvn integration-test -P config[A|Q] -Dtest=fedora.test.integration.TestLargeDatastreams

Running system tests with an alternate host or webapp context
=============================================================
By default, integration tests assume Fedora is running at
http://localhost:8080/fedora/.  A different server port may be
chosen with no consequence.

However, if the fedora server uses an alternate app
server context (i.e. not /fedora), you must set the environment
variable WEBAPP_NAME to the alternate context name.  This variable
is used by command-line utilities.  System tests involving these 
utilities may fail if WEBAPP_NAME is not set properly.

Additionally, if your test instance of Fedora is not on the same 
host from which you are running the tests, you must manually 
edit or remove the deny-apim-if-not-localhost.xml policy before
testing.
