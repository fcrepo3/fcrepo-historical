package fedora.server.storage.types;

/**
 * <p>Title: MethodDef.java</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author Sandy Payette
 * @version 1.0
 */

public class MethodDef
{

    public String methodName;
    public String methodLabel;
    public MethodParmDef[] methodParms;

    public String httpBindingURL;
    public String httpBindingAddress;
    public String httpBindingOperationLocation;

    // TODO: deal with soap bindings

    public MethodDef()
    {
    }

}