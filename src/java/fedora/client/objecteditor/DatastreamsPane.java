package fedora.client.objecteditor;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import fedora.client.Administrator;

import fedora.server.types.gen.Datastream;

/**
 * Shows a tabbed pane, one for each datastream in the object.
 * Also show add and purge buttons at the bottom.
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
 * <p>The entire file consists of original code.  Copyright &copy; 2002, 2003 by The
 * Rector and Visitors of the University of Virginia and Cornell University.
 * All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 *
 * @author cwilper@cs.cornell.edu
 * @version $Id$
 */
public class DatastreamsPane
        extends JPanel
        implements PotentiallyDirty {

    private String m_pid;
    private JTabbedPane m_tabbedPane;
    private DatastreamPane[] m_datastreamPanes;

    /**
     * Build the pane.
     */
    public DatastreamsPane(String pid)
            throws Exception {
        m_pid=pid;

        // this(m_tabbedPane)

            // m_tabbedPane(DatastreamPane[])

            m_tabbedPane=new JTabbedPane(SwingConstants.LEFT);
            Datastream[] currentVersions=Administrator.APIM.
                    getDatastreams(pid, null, null);
            m_datastreamPanes=new DatastreamPane[currentVersions.length];
            for (int i=0; i<currentVersions.length; i++) {
                m_datastreamPanes[i]=new DatastreamPane(pid, currentVersions[i]);
                m_tabbedPane.add(currentVersions[i].getID(), m_datastreamPanes[i]);
            }

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(6,6,0,6));
        add(m_tabbedPane, BorderLayout.CENTER);

    }

    public boolean isDirty() {
        for (int i=0; i<m_datastreamPanes.length; i++) {
            if (m_datastreamPanes[i].isDirty()) return true;
        }
        return false;
    }

}