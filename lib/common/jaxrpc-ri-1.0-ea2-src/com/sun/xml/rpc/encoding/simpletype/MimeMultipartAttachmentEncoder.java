// NMI's Java Code Viewer 5.1 � 1997-2001 B. Lemaire
// http://njcv.htmlplanet.com - info@njcv.htmlplanet.com

// Copy registered to Evaluation Copy                                   

// Source File Name:   MimeMultipartAttachmentEncoder.java

package com.sun.xml.rpc.encoding.simpletype;

import javax.activation.DataHandler;
import javax.mail.Multipart;
import javax.mail.internet.MimeMultipart;

// Referenced classes of package com.sun.xml.rpc.encoding.simpletype:
//            AttachmentEncoder

public class MimeMultipartAttachmentEncoder
    implements AttachmentEncoder {

    private static final AttachmentEncoder encoder = new MimeMultipartAttachmentEncoder();

    private MimeMultipartAttachmentEncoder() {
    }

    public static AttachmentEncoder getInstance() {
        return encoder;
    }

    public DataHandler objectToDataHandler(Object obj) throws Exception {
        String contentType = ((MimeMultipart)obj).getContentType();
        DataHandler dataHandler = new DataHandler(obj, contentType);
        return dataHandler;
    }

    public Object dataHandlerToObject(DataHandler dataHandler) throws Exception {
        return dataHandler.getContent();
    }

}
