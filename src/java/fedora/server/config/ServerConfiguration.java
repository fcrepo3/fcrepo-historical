package fedora.server.config;

import java.io.*;
import java.util.*;

/**
 *
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2002-2005 by The
 * Rector and Visitors of the University of Virginia and Cornell University.
 * All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 *
 */
public class ServerConfiguration 
        extends Configuration {

    private String m_className;
    private List m_moduleConfigurations;
    private List m_datastoreConfigurations;

    public ServerConfiguration(String className,
                               List parameters,
                               List moduleConfigurations,
                               List datastoreConfigurations) {
        super(parameters);
        m_className = className;
        m_moduleConfigurations = moduleConfigurations;
        m_datastoreConfigurations = datastoreConfigurations;
    }

    public void serialize(OutputStream xmlStream) throws IOException {
        PrintStream out = new PrintStream(xmlStream);
        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        out.println("<server xmlns=\"http://www.fedora.info/definitions/1/0/config/\" class=\"" + m_className + "\">");
        // do server parameters first
        serializeParameters(getParameters(), 2, out);
        // next, modules
        Iterator mIter = getModuleConfigurations().iterator();
        while (mIter.hasNext()) {
            ModuleConfiguration mc = (ModuleConfiguration) mIter.next();
            out.println("  <module role=\"" + mc.getRole() + "\" class=\"" + mc.getClassName() + "\">");
            String comment = strip(mc.getComment());
            if (comment != null) {
                out.println("    <comment>" + comment + "</comment>");
            }
            serializeParameters(mc.getParameters(), 4, out);
            out.println("  </module>");
        }
        // finally, datastores
        Iterator dIter = getDatastoreConfigurations().iterator();
        while (dIter.hasNext()) {
            DatastoreConfiguration dc = (DatastoreConfiguration) dIter.next();
            out.println("  <datastore id=\"" + dc.getId() + "\">");
            String comment = strip(dc.getComment());
            if (comment != null) {
                out.println("    <comment>" + comment + "</comment>");
            }
            serializeParameters(dc.getParameters(), 4, out);
            out.println("  </datastore>");
        }

        out.println("</server>");
        out.close();
    }

    private void serializeParameters(List params, int indentBy, PrintStream out) {
        Iterator paramIter = params.iterator();
        while (paramIter.hasNext()) {
            out.println(getParamXMLString((Parameter) paramIter.next(), indentBy));
        }
    }

    private String spaces(int num) {
        StringBuffer out = new StringBuffer();
        for (int i = 0; i < num; i++) out.append(' ');
        return out.toString();
    }

    private String getParamXMLString(Parameter p, int indentBy) {
        StringBuffer out = new StringBuffer();
        out.append(spaces(indentBy) + "<param name=\"" + p.getName() + "\" value=\"" + enc(p.getValue()) + "\"");
        if (p.getProfileValues() != null) {
            Iterator iter = p.getProfileValues().keySet().iterator();
            while (iter.hasNext()) {
                String profileName = (String) iter.next();
                String profileVal = (String) p.getProfileValues().get(profileName);
                out.append(" " + profileName + "value=\"" + enc(profileVal) + "\"");
            }
        }
        String comment = strip(p.getComment());
        if (comment != null) {
            out.append(">\n" + spaces(indentBy + 2) + "<comment>" + enc(comment)
                       + "</comment>\n" + spaces(indentBy) + "</param>");
        } else {
            out.append("/>");
        }
        return out.toString();
    }

    private String enc(String in) {
        StringBuffer out = new StringBuffer();
        for (int i = 0; i < in.length(); i++) {
            char c = in.charAt(i);
            if (c == '<') {
                out.append("&lt;");
            } else if (c == '>') {
                out.append("&gt;");
            } else if (c == '\'') {
                out.append("&apos;");
            } else if (c == '\"') {
                out.append("&quot;");
            } else if (c == '&') {
                out.append("&amp;");
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    // strip leading and trailing whitespace and \n, return null if
    // resulting string is empty in incoming string is null.
    private String strip(String in) {
        if (in == null) return null;
        String out = stripTrailing(stripLeading(in));
        if (out.length() == 0) {
            return null;
        } else {
            return out;
        }
    }

    private static String stripLeading(String in) {
        StringBuffer out = new StringBuffer();
        boolean foundNonWhitespace = false;
        for (int i = 0; i< in.length(); i++) {
            char c = in.charAt(i);
            if (foundNonWhitespace) {
                out.append(c);
            } else {
                if (c != ' ' && c != '\t' && c != '\n') {
                    foundNonWhitespace = true;
                    out.append(c);
                }
            }
        }
        return out.toString();
    }

    private static String stripTrailing(String in) {
        StringBuffer out = new StringBuffer();
        boolean foundNonWhitespace = false;
        for (int i = in.length() - 1; i >= 0; i--) {
            char c = in.charAt(i);
            if (foundNonWhitespace) {
                out.insert(0, c);
            } else {
                if (c != ' ' && c != '\t' && c != '\n') {
                    foundNonWhitespace = true;
                    out.insert(0, c);
                }
            }
        }
        return out.toString();
    }

    public String getClassName() {
        return m_className;
    }

    public List getModuleConfigurations() {
        return m_moduleConfigurations;
    }

    public ModuleConfiguration getModuleConfiguration(String role) {
        for (int i = 0; i < m_moduleConfigurations.size(); i++) {
            ModuleConfiguration config = (ModuleConfiguration) m_moduleConfigurations.get(i);
            if (config.getRole().equals(role)) return config;
        }
        return null;
    }

    public List getDatastoreConfigurations() {
        return m_datastoreConfigurations;
    }

    public DatastoreConfiguration getDatastoreConfiguration(String id) {
        for (int i = 0; i < m_datastoreConfigurations.size(); i++) {
            DatastoreConfiguration config = (DatastoreConfiguration) m_datastoreConfigurations.get(i);
            if (config.getId().equals(id)) return config;
        }
        return null;
    }

}
