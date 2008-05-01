/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */

package fedora.client.deployment;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.IOException;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

import fedora.client.deployment.data.Method;
import fedora.client.deployment.data.MethodParm;
import fedora.client.deployment.data.MethodProperties;
import fedora.client.objecteditor.Util;
import fedora.client.objecteditor.types.MethodDefinition;
import fedora.client.objecteditor.types.ParameterDefinition;

/**
 * @author Sandy Payette
 */
public class MethodsPane
        extends JPanel {

    private static final long serialVersionUID = 1L;

    protected JInternalFrame parent;

    private JRadioButton rb_baseURL;

    private JRadioButton rb_noBaseURL;

    private JRadioButton rb_noBaseURLMS;

    private final ButtonGroup rb_buttonGroup = new ButtonGroup();

    protected String rb_chosen;

    private JTextField baseURL;

    protected DefaultTableModel methodTableModel;

    protected MethodDialog methodDialog;

    private boolean editMethodMode = false;

    // Method Map: key=methodData.methodName, value=methodData
    private HashMap methodMap = new HashMap();

    private JTable methodTable;

    public MethodsPane(ServiceDefinitionBuilder parent) {
        this.parent = parent;
        JPanel methodsPanel = setMethodsPanel();
        methodsPanel
                .setBorder(new TitledBorder("Abstract Method Definitions:"));
        add(methodsPanel, BorderLayout.CENTER);
        setVisible(true);
    }

    public MethodsPane(ServiceDeploymentBuilder parent) {
        this.parent = parent;
        setLayout(new BorderLayout());
        ActionListener rb_listen = new BaseURLActionListener();

        rb_baseURL = new JRadioButton("Base URL: ", true);
        rb_baseURL.setActionCommand("baseURL");
        rb_baseURL.addActionListener(rb_listen);
        rb_chosen = "baseURL";

        rb_noBaseURLMS =
                new JRadioButton("No Base URL (Multi-Server Service)", false);
        rb_noBaseURLMS.setActionCommand("noBaseURLMS");
        rb_noBaseURLMS.addActionListener(rb_listen);

        rb_noBaseURL =
                new JRadioButton("Fedora Built-in Datastream Resolver", false);
        rb_noBaseURL.setActionCommand("noBaseURL");
        rb_noBaseURL.addActionListener(rb_listen);

        rb_buttonGroup.add(rb_baseURL);
        rb_buttonGroup.add(rb_noBaseURL);
        rb_buttonGroup.add(rb_noBaseURLMS);

        JPanel serviceBasePanel = new JPanel();
        serviceBasePanel.setBorder(new TitledBorder("Service Address"));
        serviceBasePanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.gridy = 0;
        gbc.gridx = 0;
        serviceBasePanel.add(rb_baseURL, gbc);
        gbc.gridx = 1;
        serviceBasePanel.add(baseURL = new JTextField(30), gbc);
        gbc.gridy = 1;
        gbc.gridx = 0;
        serviceBasePanel.add(rb_noBaseURLMS, gbc);
        gbc.gridy = 2;
        gbc.gridx = 0;
        serviceBasePanel.add(rb_noBaseURL, gbc);

        JPanel methodsPanel = setMethodsPanel();
        methodsPanel.setBorder(new TitledBorder("Service Method Definitions:"));
        add(serviceBasePanel, BorderLayout.NORTH);
        add(methodsPanel, BorderLayout.CENTER);
        setVisible(true);
    }

    private JPanel setMethodsPanel() {
        // Table Panel
        methodTableModel = new DefaultTableModel();
        // Create a JTable that disallow edits (edits done via dialog box only)
        methodTable = new JTable(methodTableModel) {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int rowIndex, int vColIndex) {
                return false;
            }
        };

        methodTable.setColumnSelectionAllowed(false);
        methodTable.setRowSelectionAllowed(true);
        // methodTable.setBackground(Color.WHITE);

        methodTableModel.addColumn("Method Name");
        methodTableModel.addColumn("Method Description");

        JScrollPane scrollpane = new JScrollPane(methodTable);
        scrollpane.getViewport().setBackground(Color.white);

        // Table Buttons Panel
        JButton jb1 = new JButton("New");
        jb1.setMinimumSize(new Dimension(100, 30));
        jb1.setMaximumSize(new Dimension(100, 30));
        jb1.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                addMethod();
            }
        });
        JButton jb1b = new JButton("Edit");
        jb1b.setMinimumSize(new Dimension(100, 30));
        jb1b.setMaximumSize(new Dimension(100, 30));
        jb1b.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                editMethod();
            }
        });
        JButton jb2 = new JButton("Delete");
        jb2.setMinimumSize(new Dimension(100, 30));
        jb2.setMaximumSize(new Dimension(100, 30));
        jb2.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                deleteMethod();
            }
        });
        JButton jb3 = new JButton("Properties");
        jb3.setMinimumSize(new Dimension(100, 30));
        jb3.setMaximumSize(new Dimension(100, 30));
        jb3.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                addMethodProperties();
            }
        });
        JButton jb4 = new JButton("Help");
        jb4.setMinimumSize(new Dimension(100, 30));
        jb4.setMaximumSize(new Dimension(100, 30));
        jb4.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                helpTable();
            }
        });
        JPanel t_buttonPanel = new JPanel();
        // t_buttonPanel.setLayout(new GridLayout(3,1));
        t_buttonPanel.setLayout(new BoxLayout(t_buttonPanel, BoxLayout.Y_AXIS));
        t_buttonPanel.add(jb1);
        t_buttonPanel.add(jb1b);
        t_buttonPanel.add(jb2);
        t_buttonPanel.add(jb3);
        t_buttonPanel.add(jb4);

        JPanel methodsPanel = new JPanel(new BorderLayout());
        // methodsPanel.setBorder(new TitledBorder("Service Method
        // Definitions:"));
        methodsPanel.add(scrollpane, BorderLayout.CENTER);
        methodsPanel.add(t_buttonPanel, BorderLayout.EAST);
        return methodsPanel;
    }

    public void clearContractMethods() {
        // reinitialize the method map and the method table display
        methodMap = new HashMap();
        ((DefaultTableModel) methodTable.getModel()).setNumRows(0);
    }

    public JTable renderContractMethods(String sDefPID) {
        // reinitialize the method map and the method table display
        clearContractMethods();

        List methodDefs = getSDefMethods(sDefPID);
        for (int i = 0; i < methodDefs.size(); i++) {
            MethodDefinition def = (MethodDefinition) methodDefs.get(i);
            List parms = def.parameterDefinitions();
            Method newmethod = new Method();
            newmethod.methodName = def.getName();
            newmethod.methodLabel = def.getLabel();
            newmethod.methodProperties = new MethodProperties();
            newmethod.methodProperties.dsBindingKeys = new String[0];
            newmethod.methodProperties.methodFullURL = null;
            newmethod.methodProperties.methodRelativeURL = null;
            newmethod.methodProperties.protocolType =
                    Method.HTTP_MESSAGE_PROTOCOL;
            newmethod.methodProperties.returnMIMETypes = new String[0];
            newmethod.methodProperties.methodParms =
                    new MethodParm[parms.size()];
            for (int j = 0; j < parms.size(); j++) {
                ParameterDefinition parm = (ParameterDefinition) parms.get(j);
                MethodParm p = new MethodParm();
                p.parmDefaultValue = parm.getDefaultValue();
                p.parmDomainValues =
                        (String[]) parm.validValues().toArray(new String[0]);
                p.parmLabel = parm.getLabel();
                p.parmName = parm.getName();
                p.parmPassBy = MethodParm.PASS_BY_VALUE;
                p.parmRequired = Boolean.toString(parm.isRequired());
                p.parmType = MethodParm.USER_INPUT;
                newmethod.methodProperties.methodParms[j] = p;
            }
            try {
                setSDepMethod(newmethod.methodName, newmethod.methodLabel);
                setMethodProperties(newmethod.methodName,
                                    newmethod.methodProperties);
            } catch (DeploymentBuilderException e) {
                assertMethodLoadErrorMsg("Problem loading sdef contract methods");
            }
        }
        return methodTable;
    }

    // Action Listener for button group
    class BaseURLActionListener
            implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            rb_chosen = rb_buttonGroup.getSelection().getActionCommand();
            if (rb_chosen.equalsIgnoreCase("baseURL")) {
                baseURL.setEnabled(true);
            } else if (rb_chosen.equalsIgnoreCase("noBaseURLMS")) {
                baseURL.setEnabled(false);
                baseURL.setText("");
                cleanupMethodURLs();
            } else if (rb_chosen.equalsIgnoreCase("noBaseURL")) {
                baseURL.setEnabled(false);
                baseURL.setText("");
                cleanupMethodURLs();
            }
        }
    }

    private void cleanupMethodURLs() {
        Method[] methods = getMethods();
        for (int i = 0; i < methods.length; i++) {
            methods[i].methodProperties.methodFullURL = " ";
            methods[i].methodProperties.methodRelativeURL = " ";
            methodMap.put(methods[i].methodName, methods[i]);
        }
    }

    public boolean hasBaseURL() {
        if (rb_chosen.equalsIgnoreCase("baseURL")) {
            return true;
        }
        return false;
    }

    public String getBaseURL() {
        if (hasBaseURL()) {
            return baseURL.getText();
        }
        return null;
    }

    public boolean isMultiServer() {
        if (rb_chosen.equalsIgnoreCase("noBaseURLMS")) {
            return true;
        }
        return false;
    }

    public boolean isLocalHTTP() {
        if (rb_chosen.equalsIgnoreCase("noBaseURL")) {
            return true;
        }
        return false;
    }

    public HashMap getMethodMap() {
        return methodMap;
    }

    public Method[] getMethods() {
        Vector v_methods = new Vector();
        Collection c = methodMap.values();
        Iterator methods = c.iterator();
        while (methods.hasNext()) {
            Method m = (Method) methods.next();
            v_methods.add(m);
        }
        return (Method[]) v_methods.toArray(new Method[0]);
    }

    public void setSDepMethod(String methodName, String methodDesc)
            throws DeploymentBuilderException {
        if (editMethodMode) {
            int currentRowIndex = methodTable.getSelectedRow();
            String oldMethodName =
                    (String) methodTable.getValueAt(currentRowIndex, 0);
            Method methodData = (Method) methodMap.get(oldMethodName);
            if (!oldMethodName.equalsIgnoreCase(methodName)) {
                methodMap.remove(oldMethodName);
            }
            methodData.methodName = methodName;
            methodData.methodLabel = methodDesc;
            methodMap.put(methodData.methodName, methodData);
            methodTable.setValueAt(methodData.methodName, currentRowIndex, 0);
            methodTable.setValueAt(methodData.methodLabel, currentRowIndex, 1);
        } else {
            if (methodMap.containsKey(methodName)) {
                throw new DeploymentBuilderException("MethodsPane.setSDepMethod: Method name exists already");
            }
            methodTableModel.addRow(new Object[] {methodName, methodDesc});
            Method methodData = new Method();
            methodData.methodName = methodName;
            methodData.methodLabel = methodDesc;
            methodMap.put(methodName, methodData);
        }
    }

    public void setMethodProperties(String methodName,
                                    MethodProperties mproperties) {
        Method method = (Method) methodMap.get(methodName);
        method.methodProperties = mproperties;
        methodMap.put(methodName, method);
    }

    private void addMethod() {
        methodDialog = new MethodDialog(this, "Add Method", true);
    }

    private void editMethod() {
        editMethodMode = true;
        int currentRowIndex = methodTable.getSelectedRow();
        if (methodTable.getRowCount() <= 0) {
            editMethodMode = false;
            assertNoMethodMsg("There are no rows to edit.");
            return;
        } else if (currentRowIndex == -1) {
            editMethodMode = false;
            assertNoMethodMsg("You must select a method row before attempting editing.");
            return;
        }
        methodDialog =
                new MethodDialog(this,
                                 "Edit Method",
                                 true,
                                 (String) methodTable
                                         .getValueAt(currentRowIndex, 0),
                                 (String) methodTable
                                         .getValueAt(currentRowIndex, 1));
        editMethodMode = false;
    }

    private void deleteMethod() {
        int currentRowIndex = methodTable.getSelectedRow();
        if (methodTable.getRowCount() <= 0) {
            assertNoMethodMsg("There are no rows to delete.");
            return;
        } else if (currentRowIndex == -1) {
            assertNoMethodMsg("You must select a method row before attempting deletion.");
            return;
        }
        String methodName = (String) methodTable.getValueAt(currentRowIndex, 0);
        methodTableModel.removeRow(currentRowIndex);
        if (methodMap.containsKey(methodName)) {
            methodMap.remove(methodName);
        }
    }

    private void addMethodProperties() {
        if (methodTable.getRowCount() <= 0) {
            assertNoMethodMsg("You must enter a method before entering properties");
            return;
        } else if (methodTable.getSelectedRowCount() <= 0) {
            assertNoMethodMsg("You must select a method row before entering properties");
            return;
        } else if (rb_baseURL != null && baseURL != null
                && rb_baseURL.isSelected()
                && baseURL.getText().trim().equalsIgnoreCase("")) {
            assertNoMethodMsg("You must enter the Base URL for the service before entering method properties");
            return;
        } else {
            int currentRowIndex = methodTable.getSelectedRow();
            String methodName =
                    (String) methodTable.getValueAt(currentRowIndex, 0);
            if (methodName == null || methodName.trim().equalsIgnoreCase("")) {
                assertNoMethodMsg("You must enter a method name before entering properties");
                return;
            } else {
                Method m = (Method) methodMap.get(methodName);
                MethodPropertiesDialog properties =
                        new MethodPropertiesDialog(this,
                                                   m.methodName,
                                                   m.methodProperties);
            }
            return;
        }
    }

    private void helpTable() {
        JTextArea helptxt = new JTextArea();
        helptxt.setLineWrap(true);
        helptxt.setWrapStyleWord(true);
        helptxt.setBounds(0, 0, 550, 20);
        helptxt
                .append("The Methods Tab is used to define a 'service contract'"
                        + " which is a set of abstract method definitions.  Define the method names and"
                        + " any user-supplied parameters to those methods.");

        JOptionPane.showMessageDialog(this,
                                      helptxt,
                                      "Help for Abstract Methods Tab",
                                      JOptionPane.OK_OPTION);
    }

    protected void assertNoMethodMsg(String msg) {
        JOptionPane.showMessageDialog(this,
                                      new String(msg),
                                      "No Method Message",
                                      JOptionPane.INFORMATION_MESSAGE);
    }

    protected void assertMethodExistsMsg(String msg) {
        JOptionPane.showMessageDialog(this,
                                      new String(msg),
                                      "Method Exists Message",
                                      JOptionPane.INFORMATION_MESSAGE);
    }

    protected void assertMethodLoadErrorMsg(String msg) {
        JOptionPane.showMessageDialog(this,
                                      new String(msg),
                                      "Method Loading Problem",
                                      JOptionPane.INFORMATION_MESSAGE);
    }

    private List getSDefMethods(String sDefPID) {
        List methodDefs = null;
        try {
            methodDefs = Util.getMethodDefinitions(sDefPID);
        } catch (IOException e) {
            assertNoMethodMsg("Could not obtain methods from Service Definition object.");
        }
        return methodDefs;
    }
}
