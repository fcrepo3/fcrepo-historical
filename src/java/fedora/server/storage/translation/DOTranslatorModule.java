package fedora.server.storage.translation;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import fedora.server.Module;
import fedora.server.Server;
import fedora.server.errors.ModuleInitializationException;
import fedora.server.errors.ObjectIntegrityException;
import fedora.server.errors.ServerException;
import fedora.server.errors.StreamIOException;
import fedora.server.errors.UnsupportedTranslationException;
import fedora.server.storage.types.DigitalObject;

/**
 * DOTranslatorImpl as a Module.
 *
 * @author cwilper@cs.cornell.edu
 * @version $Id$
 */
public class DOTranslatorModule
        extends Module
        implements DOTranslator {

    private DOTranslatorImpl m_wrappedTranslator;

    public DOTranslatorModule(Map params, Server server, String role)
            throws ModuleInitializationException {
        super(params, server, role);
    }

    public void initModule()
            throws ModuleInitializationException {
        HashMap serMap=new HashMap();
        HashMap deserMap=new HashMap();
        Iterator nameIter=parameterNames();
        while (nameIter.hasNext()) {
            String paramName=(String) nameIter.next();
            if (paramName.startsWith("serializer_")) {
                String serName=paramName.substring(11);
                try {
                    DOSerializer ser=(DOSerializer) Class.forName(
                            getParameter(paramName)).newInstance();
                    serMap.put(serName, ser);
                } catch (Exception e) {
                    throw new ModuleInitializationException(
                            "Can't instantiate serializer class for format="
                            + serName + " : " +
                            e.getClass().getName() + ": " + e.getMessage(),
                            getRole());
                }
            } else if (paramName.startsWith("deserializer_")) {
                String deserName=paramName.substring(13);
                try {
                    DODeserializer deser=(DODeserializer) Class.forName(
                            getParameter(paramName)).newInstance();
                    deserMap.put(deserName, deser);
                } catch (Exception e) {
                    throw new ModuleInitializationException(
                            "Can't instantiate deserializer class for format="
                            + deserName + " : " +
                            e.getClass().getName() + ": " + e.getMessage(),
                            getRole());
                }
            }
        }
        m_wrappedTranslator=new DOTranslatorImpl(serMap, deserMap);
    }

    public void deserialize(InputStream in, DigitalObject out,
            String format, String encoding, int transContext)
            throws ObjectIntegrityException, StreamIOException,
            UnsupportedTranslationException, ServerException {
        m_wrappedTranslator.deserialize(in, out, format, encoding, transContext); 
    }

    public void serialize(DigitalObject in, OutputStream out,
			String format, String encoding, int transContext)
            throws ObjectIntegrityException, StreamIOException,
            UnsupportedTranslationException, ServerException {
		m_wrappedTranslator.serialize(in, out, format, encoding, transContext);
    }

}