// NMI's Java Code Viewer 5.1 � 1997-2001 B. Lemaire
// http://njcv.htmlplanet.com - info@njcv.htmlplanet.com

// Copy registered to Evaluation Copy                                   

// Source File Name:   ExtensionVisitor.java

package com.sun.xml.rpc.wsdl.framework;


// Referenced classes of package com.sun.xml.rpc.wsdl.framework:
//            Extension

public interface ExtensionVisitor {

    public abstract void preVisit(Extension extension) throws Exception;

    public abstract void postVisit(Extension extension) throws Exception;
}
