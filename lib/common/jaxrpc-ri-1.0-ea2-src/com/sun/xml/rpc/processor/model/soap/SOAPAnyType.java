// NMI's Java Code Viewer 5.1 � 1997-2001 B. Lemaire
// http://njcv.htmlplanet.com - info@njcv.htmlplanet.com

// Copy registered to Evaluation Copy                                   

// Source File Name:   SOAPAnyType.java

package com.sun.xml.rpc.processor.model.soap;

import com.sun.xml.rpc.processor.model.java.JavaType;
import javax.xml.rpc.namespace.QName;

// Referenced classes of package com.sun.xml.rpc.processor.model.soap:
//            SOAPType, SOAPTypeVisitor

public class SOAPAnyType extends SOAPType {

    public SOAPAnyType(QName name) {
        super(name);
    }

    public SOAPAnyType(QName name, JavaType javaType) {
        super(name, javaType);
    }

    public void accept(SOAPTypeVisitor visitor) throws Exception {
        visitor.visit(this);
    }
}
