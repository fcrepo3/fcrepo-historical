// NMI's Java Code Viewer 5.1 � 1997-2001 B. Lemaire
// http://njcv.htmlplanet.com - info@njcv.htmlplanet.com

// Copy registered to Evaluation Copy                                   

// Source File Name:   JAXRPCDeserializer.java

package com.sun.xml.rpc.encoding;

import com.sun.xml.rpc.streaming.XMLReader;
import javax.activation.DataHandler;
import javax.xml.rpc.encoding.Deserializer;
import javax.xml.rpc.namespace.QName;

// Referenced classes of package com.sun.xml.rpc.encoding:
//            SOAPDeserializationContext

public interface JAXRPCDeserializer
    extends Deserializer {

    public abstract Object deserialize(QName qname, XMLReader xmlreader, SOAPDeserializationContext soapdeserializationcontext);

    public abstract Object deserialize(DataHandler datahandler, SOAPDeserializationContext soapdeserializationcontext);
}
