/*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Educational Community License (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.opensource.org/licenses/ecl1.txt">
 * http://www.opensource.org/licenses/ecl1.txt.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2002-2006 by 
 * The Rector and Visitors of the University of Virginia and Cornell University.
 * All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

package fedora.server.journal.xmlhelpers;

import java.util.Iterator;

import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;

import fedora.server.Context;
import fedora.server.MultiValueMap;
import fedora.server.journal.entry.JournalEntryContext;
import fedora.server.journal.helpers.JournalHelper;
import fedora.server.journal.helpers.PasswordCipher;

/**
 * 
 * <p>
 * <b>Title:</b> ContextXmlWriter.java
 * </p>
 * <p>
 * <b>Description:</b> Write an entire Context object to the Journal file.
 * </p>
 * 
 * @author jblake@cs.cornell.edu
 * @version $Id$
 */

public class ContextXmlWriter extends AbstractXmlWriter {

    public void writeContext(JournalEntryContext context, XMLEventWriter writer)
            throws XMLStreamException {
        putStartTag(writer, QNAME_TAG_CONTEXT);

        writeContextPassword(context, writer);
        writeContextNoOp(context, writer);
        writeContextNow(context, writer);

        writeMultiMap(writer, CONTEXT_MAPNAME_ENVIRONMENT, context
                .getEnvironmentAttributes());
        writeMultiMap(writer, CONTEXT_MAPNAME_SUBJECT, context
                .getSubjectAttributes());
        writeMultiMap(writer, CONTEXT_MAPNAME_ACTION, context
                .getActionAttributes());
        writeMultiMap(writer, CONTEXT_MAPNAME_RESOURCE, context
                .getResourceAttributes());
        writeMultiMap(writer, CONTEXT_MAPNAME_RECOVERY, context
                .getRecoveryAttributes());

        putEndTag(writer, QNAME_TAG_CONTEXT);
    }

    private void writeContextPassword(Context context, XMLEventWriter writer)
            throws XMLStreamException {
        String password = context.getPassword();
        if (password == null) {
            password = "";
        }

        putStartTag(writer, QNAME_TAG_PASSWORD);
        putAttribute(writer, QNAME_ATTR_PASSWORD_TYPE, PASSWORD_CIPHER_TYPE);
        putCharacters(writer, encipherPassword(context, password));
        putEndTag(writer, QNAME_TAG_PASSWORD);
    }

    private String encipherPassword(Context context, String password) {
        String key = JournalHelper.formatDate(context.now());
        return PasswordCipher.encipher(key, password);
    }

    private void writeContextNoOp(Context context, XMLEventWriter writer)
            throws XMLStreamException {
        putStartTag(writer, QNAME_TAG_NOOP);
        putCharacters(writer, String.valueOf(context.getNoOp()));
        putEndTag(writer, QNAME_TAG_NOOP);
    }

    private void writeContextNow(Context context, XMLEventWriter writer)
            throws XMLStreamException {
        putStartTag(writer, QNAME_TAG_NOW);
        putCharacters(writer, JournalHelper.formatDate(context.now()));
        putEndTag(writer, QNAME_TAG_NOW);
    }

    private void writeMultiMap(XMLEventWriter writer, String mapName,
            MultiValueMap map) throws XMLStreamException {
        putStartTag(writer, QNAME_TAG_MULTI_VALUE_MAP);
        putAttribute(writer, QNAME_ATTR_NAME, mapName);
        for (Iterator attributes = map.names(); attributes.hasNext();) {
            String attribute = (String) attributes.next();
            putStartTag(writer, QNAME_TAG_MULTI_VALUE_MAP_KEY);
            putAttribute(writer, QNAME_ATTR_NAME, attribute);
            String[] values = map.getStringArray(attribute);
            for (int i = 0; i < values.length; i++) {
                putStartTag(writer, QNAME_TAG_MULTI_VALUE_MAP_VALUE);
                putCharacters(writer, values[i]);
                putEndTag(writer, QNAME_TAG_MULTI_VALUE_MAP_VALUE);
            }
            putEndTag(writer, QNAME_TAG_MULTI_VALUE_MAP_KEY);
        }
        putEndTag(writer, QNAME_TAG_MULTI_VALUE_MAP);
    }

}
