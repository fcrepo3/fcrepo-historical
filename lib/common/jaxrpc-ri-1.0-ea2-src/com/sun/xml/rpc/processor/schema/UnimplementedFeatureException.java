// NMI's Java Code Viewer 5.1 � 1997-2001 B. Lemaire
// http://njcv.htmlplanet.com - info@njcv.htmlplanet.com

// Copy registered to Evaluation Copy                                   

// Source File Name:   UnimplementedFeatureException.java

package com.sun.xml.rpc.processor.schema;

import com.sun.xml.rpc.processor.model.ModelException;

public class UnimplementedFeatureException extends ModelException {

    public UnimplementedFeatureException(String arg) {
        super("model.schema.notImplemented", arg);
    }
}
