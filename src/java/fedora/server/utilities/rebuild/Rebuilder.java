package fedora.server.utilities.rebuild;

import java.io.*;
import java.util.*;

import fedora.server.config.ServerConfiguration;
import fedora.server.storage.types.DigitalObject;

/**
 * Interface for a class that rebuilds some aspect of the repository.
 *
 * It is expected that clients of this interface will first call init,
 * then start, then addObject (possibly a series of times), then finish.
 * 
 * @@version $Id$
 */
public interface Rebuilder {

    /**
     * Get a short phrase describing what the user can do with this rebuilder.
     */
    public String getAction();

    /**
     * Initialize the rebuilder, given the server configuration.
     *
     * @@returns a map of option names to plaintext descriptions.
     */
    public Map init(File serverBaseDir,
                    ServerConfiguration serverConfig) throws Exception;

    /**
     * Validate the provided options and perform any necessary startup tasks.
     */
    public void start(Map options) throws Exception;

    /**
     * Add the data of interest for the given object.
     */
    public void addObject(DigitalObject object) throws Exception;

    /**
     * Free up any system resources associated with rebuilding.
     */
    public void finish() throws Exception;

}