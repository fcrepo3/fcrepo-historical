package fedora.server.validation;

/**
 * <p>Title: DOValidatorModule.java </p>
 * <p>Description: Module Wrapper for DOValidatorImpl.java. </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author Sandy Payette, payette@cs.cornell.edu
 * @version 1.0
 */

// Fedora imports
import fedora.server.errors.ServerException;
import fedora.server.errors.GeneralException;
import fedora.server.errors.ObjectValidityException;
import fedora.server.errors.ModuleInitializationException;
import fedora.server.errors.InitializationException;
import fedora.server.Module;
import fedora.server.Server;

// Java imports
import java.util.Map;
import java.io.InputStream;
import java.io.File;

public class DOValidatorModule extends Module implements DOValidator
{

  /**
   * An instance of the core implementation class for DOValidator.
   * The DOValidatorModule acts as a wrapper to this class.
   */
  private DOValidatorImpl dov = new DOValidatorImpl();

  /**
   * <p>Constructs a new DOValidatorModule</p>
   *
   * @param moduleParameters The name/value pair map of module parameters.
   * @param server The server instance.
   * @param role The module role name.
   * @throws ModuleInitializationException If initialization values are
   *         invalid or initialization fails for some other reason.
   */
    public DOValidatorModule(Map moduleParameters, Server server, String role)
          throws ModuleInitializationException, ServerException
    {
        super(moduleParameters, server, role);
    }

  /**
   * Initializes the Module based on configuration parameters. The
   * implementation of this method is dependent on the schema used to define
   * the parameter names for the role of
   * <code>fedora.server.storage.DOValidatorModule</code>.
   *
   * @throws ModuleInitializationException If initialization values are
   *         invalid or initialization fails for some other reason.
   */
  public void initModule() throws ModuleInitializationException
  {
    try
    {
      Server s_server = this.getServer();
      dov.tempDir = System.getProperty("fedora.home") + this.getParameter("tempDir");
      dov.xmlSchemaURL = this.getParameter("xmlSchemaURL");
      dov.xmlSchemaLocalPath = System.getProperty("fedora.home") + this.getParameter("xmlSchemaLocalPath");
      dov.schematronPreprocessorID = System.getProperty("fedora.home") + this.getParameter("schematronPreprocessor");
      dov.schematronSchemaID = System.getProperty("fedora.home") + this.getParameter("schematronSchema");
      dov.schematronValidatingXslID = System.getProperty("fedora.home") + this.getParameter("schematronValidatingXsl");
    }
    catch(Exception e)
    {
      System.out.println("Unable to initialize validation module: " + e.getMessage());
      throw new ModuleInitializationException(
          e.getMessage(),"fedora.server.validation.DOValidatorModule");
    }
  }

  /**
   * <p>Validates a digital object.</p>
   *
   * @param in The digital object provided as a bytestream.
   * @param validationLevel The level of validation to perform on the digital object.
   *        This is an integer from 0-3 with the following meanings:
   *        0 = do all validation levels
   *        1 = perform only XML Schema validation
   *        2 = perform only Schematron Rules validation
   *        3 = perform only referential integrity checks for the object
   * @param workFlowPhase The stage in the work flow for which the validation should be contextualized.
   *        "ingest" = the object is in the submission format for the ingest stage phase
   *        "store" = the object is in the authoritative format for the final storage phase
   * @throws ServerException If validation fails for any reason.
   */
  public void validate(InputStream objectAsStream, int validationLevel, String workFlowPhase)
    //throws ObjectValidityException, GeneralException
    throws ServerException
  {
    dov.validate(objectAsStream, validationLevel, workFlowPhase);
  }

  /**
   * <p>Validates a digital object.</p>
   *
   * @param in The digital object provided as a file.
   * @param validationLevel The level of validation to perform on the digital object.
   *        This is an integer from 0-3 with the following meanings:
   *        0 = do all validation levels
   *        1 = perform only XML Schema validation
   *        2 = perform only Schematron Rules validation
   *        3 = perform only referential integrity checks for the object
   * @param workFlowPhase The stage in the work flow for which the validation should be contextualized.
   *        "ingest" = the object is in the submission format for the ingest stage phase
   *        "store" = the object is in the authoritative format for the final storage phase
   * @throws ServerException If validation fails for any reason.
   */
  public void validate(File objectAsFile, int validationLevel, String workFlowPhase)
    //throws ObjectValidityException, GeneralException
    throws ServerException
  {
      dov.validate(objectAsFile, validationLevel, workFlowPhase);
  }
}