package fedora.server.storage;

/**
 * <p>Title: BMechReader.java</p>
 * <p>Description: Interface for reading Behavior Mechanism Objects</p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author Sandy Payette
 * @version 1.0
 */

import fedora.server.storage.abstraction.*;
import java.io.InputStream;
import java.util.Date;

public interface BMechReader extends DOReader
{

  public MethodDef[] GetBehaviorMethods(Date versDateTime);

  // Overloaded method: returns InputStream of WSDL as alternative
  public InputStream GetBehaviorMethodsWSDL(Date versDateTime);

  public BMechDSBindSpec GetDSBindingSpec(Date versDateTime);
}