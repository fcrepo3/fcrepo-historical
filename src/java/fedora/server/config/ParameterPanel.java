package fedora.server.config;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

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
public class ParameterPanel extends JPanel implements ListSelectionListener, ActionListener {

    private JList m_paramList;
    private ItemListModel m_model;
    private JPanel m_paramValuePanel;
    private boolean m_ignoreValueChanged;

    public ParameterPanel(java.util.List parameterList) {
        super(new BorderLayout());
        m_model = new ItemListModel(parameterList);

        //
        // WEST: Parameter chooser with add/delete buttons
        //
        JPanel paramChoice = new JPanel(new BorderLayout());
        paramChoice.add(new JLabel("Parameter:"), BorderLayout.NORTH);
        m_paramList = new JList(m_model);
        m_paramList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_paramList.setVisibleRowCount(0);
        m_paramList.addListSelectionListener(this);
        paramChoice.add(new JScrollPane(m_paramList), BorderLayout.CENTER);
        JPanel paramButtonPanel = new JPanel(new BorderLayout());
        paramButtonPanel.setBorder(BorderFactory.createEmptyBorder(6,0,0,0));
        JButton addButton = new JButton("Add Parameter...");
        addButton.addActionListener(this);
        JButton deleteButton = new JButton("Delete Parameter...");
        deleteButton.addActionListener(this);
        paramButtonPanel.add(addButton, BorderLayout.NORTH);
        JPanel deleteButtonPanel = new JPanel(new BorderLayout());
        deleteButtonPanel.add(deleteButton, BorderLayout.CENTER);
        deleteButtonPanel.setBorder(BorderFactory.createEmptyBorder(6,0,0,0));
        paramButtonPanel.add(deleteButtonPanel, BorderLayout.SOUTH);
        paramChoice.add(paramButtonPanel, BorderLayout.SOUTH);
        paramChoice.setBorder(BorderFactory.createEmptyBorder(6,12,12,6));

        //
        // CENTER: CardLayout, one panel per parameter
        //
        m_paramValuePanel = new JPanel(new CardLayout());
        Iterator iter = parameterList.iterator();
        while (iter.hasNext()) {
            addParamValueCard((Parameter) iter.next());
        }
        m_paramValuePanel.setBorder(BorderFactory.createEmptyBorder(6,6,12,12));

        add(paramChoice, BorderLayout.WEST);
        add(m_paramValuePanel, BorderLayout.CENTER);
        m_paramList.setSelectedIndex(0);
    }

    private void addParamValueCard(Parameter param) {
        m_paramValuePanel.add(new ParamValueCard(param), param.getName());
    }

    private void deleteParamValueCard(Parameter param) {
        Component[] components = m_paramValuePanel.getComponents();
        for (int i = 0; i < components.length; i++) {
            if (components[i] instanceof ParamValueCard) {
                ParamValueCard card = (ParamValueCard) components[i];
                if (card.getName().equals(param.getName())) {
                    m_paramValuePanel.remove(card);
                }
            }
        }
    }

    public void valueChanged(ListSelectionEvent e) {
        if (!m_ignoreValueChanged) {
            CardLayout cl = (CardLayout) (m_paramValuePanel.getLayout());
            Parameter param = (Parameter) m_model.getElementAt(m_paramList.getSelectedIndex());
            cl.show(m_paramValuePanel, param.getName());
            validate();
        }
    }

    /**
     * Get the values from the UI into a List of Parameter objects.
     */
    public java.util.List getParameters() {
        ArrayList out = new ArrayList();
        Component[] components = m_paramValuePanel.getComponents();
        for (int i = 0; i < components.length; i++) {
            if (components[i] instanceof ParamValueCard) {
                ParamValueCard card = (ParamValueCard) components[i];
                out.add(card.getParameter());
            }
        }
        return out;
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().startsWith("Add")) {
            String paramName = JOptionPane.showInputDialog("What is the new parameter name?");
            if (paramName != null) {
                // first, check if one of that name is in m_model (if so we'll just switch to it)
                Iterator iter = m_model.toList().iterator();
                Parameter param = null;
                while (iter.hasNext()) {
                    Parameter p = (Parameter) iter.next();
                    if (p.getName().equals(paramName)) param = p;
                }
                if (param == null) {
                    param = new Parameter(paramName, "Enter value here.", "Enter description here.", new HashMap());
                    m_model.addElement(param);
                    addParamValueCard(param);
                }
                // switch to the new (or already existing) parameter
                m_paramList.setSelectedValue(param, true);
                valueChanged(null);
            }
        } else if (e.getActionCommand().startsWith("Delete")) {
            // delete the currently selected item from m_model
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    int i = m_paramList.getSelectedIndex();
                    if ( i >= 0 ) {
                        m_ignoreValueChanged = true;
                        Parameter param = (Parameter) m_paramList.getSelectedValue();
                        m_model.remove(i);
                        m_ignoreValueChanged = false;
                        // ...and set the selection to something sane
                        if (m_model.size() > 0) {
                            if (m_model.size() > i) {
                                m_paramList.setSelectedIndex(i);
                            } else {
                                i = m_model.size() - 1;
                                m_paramList.setSelectedIndex(m_model.size() - 1);
                            }
                        }
                        // finally, remove the panel from the cardlayout
                        deleteParamValueCard(param);
                    }
                }
            });
        }
    }

    /** 
     * A JPanel for modifying the description, value, and server profile-specific values
     * of a particular parameter.
     * The layout is accomplished through nested Panels using BorderLayouts.
     */
    public class ParamValueCard extends JPanel {

        private Parameter m_param;

        private JTextArea m_descArea;
        private JComboBox m_profileList;
        private JTextField m_valueText;
        private String m_name;

        private JPanel m_valuePanel;

        public String getName() {
            return m_name;
        }

        public ParamValueCard(Parameter param) {
            super(new BorderLayout());
            m_name = param.getName();
            m_param = param;

            //
            // First, create all the interesting (non-layout) components
            //
            // a1
            if (param.getComment() == null) {
                m_descArea = new JTextArea();
            } else {
                m_descArea = new JTextArea(param.getComment());
            }
            // l1
            JLabel descriptionLabel = new JLabel("Description:");
            // d1
            m_profileList = new JComboBox(new String[] { "Primary value", "'mckoi' value", "'oracle' value", "Add Profile...", "Delete Profile..." });
            //
            // TODO: each value gets it's own card
            //
            m_valuePanel = new JPanel(new CardLayout());
            m_valueText = new JTextField(param.getValue());

            //
            // Then, lay them out with a bunch of crazy JPanels
            //
            JPanel c1 = new JPanel(new BorderLayout());
            c1.add(new JScrollPane(m_descArea), BorderLayout.CENTER);
            c1.add(descriptionLabel, BorderLayout.NORTH);
            add(c1, BorderLayout.CENTER);

            JPanel s1 = new JPanel(new BorderLayout());
            add(s1, BorderLayout.SOUTH);
            JPanel c2 = new JPanel(new BorderLayout());
            JPanel w3 = new JPanel(new BorderLayout());
            w3.setBorder(BorderFactory.createEmptyBorder(6,0,0,6));
            w3.add(m_profileList, BorderLayout.NORTH);

            JPanel c3 = new JPanel(new BorderLayout());
            c3.add(m_valueText, BorderLayout.NORTH);
            c3.setBorder(BorderFactory.createEmptyBorder(6,0,0,0));

            c2.add(c3, BorderLayout.CENTER);
            c2.add(w3, BorderLayout.WEST);
            s1.add(c2, BorderLayout.CENTER);

        }

        public Parameter getParameter() {
            String comment = m_descArea.getText();
            return null;           
//            return new Parameter(m_name, comment, value, profileValues);
        }
    
    }

}
