package fedora.client.bmech;

import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;
import javax.swing.JTable;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import javax.swing.JComponent;
import java.awt.BorderLayout;
import javax.swing.JLabel;
import javax.swing.border.TitledBorder;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.BoxLayout;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Set;
import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;
import fedora.client.bmech.data.*;

/**
 *
 * <p><b>Title:</b> MethodsPane.java</p>
 * <p><b>Description:</b> </p>
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
 * @author payette@cs.cornell.edu
 * @version 1.0
 */
public class MethodsPane extends JPanel {

    //private BMechBuilder parent;
    protected JInternalFrame parent;
    private JRadioButton rb_baseURL;
    private JRadioButton rb_noBaseURL;
    private final ButtonGroup rb_buttonGroup = new ButtonGroup();
    protected String rb_chosen;
    private JTextField baseURL;
    private JTable methodTable;
    protected DefaultTableModel methodTableModel;
    protected MethodDialog methodDialog;
    private boolean editMethodMode = false;

    // Method Map: key=methodData.methodName, value=methodData
    private HashMap methodMap = new HashMap();
    //private Vector dsBindingKeys = new Vector();

    public MethodsPane(BDefBuilder parent)
    {
      this.parent = parent;
      JPanel methodsPanel = setMethodsPanel();
      methodsPanel.setBorder(new TitledBorder("Abstract Method Definitions:"));
      add(methodsPanel, BorderLayout.CENTER);
      setVisible(true);
    }

    public MethodsPane(BMechBuilder parent)
    {
        this.parent = parent;
        setLayout(new BorderLayout());
        ActionListener rb_listen = new BaseURLActionListener();
        rb_baseURL = new JRadioButton("Base URL: ", true);
        rb_baseURL.setActionCommand("baseURL");
        rb_baseURL.addActionListener(rb_listen);
        rb_chosen = "baseURL";
        rb_noBaseURL = new JRadioButton("No Base URL (Fedora Multi-server mechanism)", false);
        rb_noBaseURL.setActionCommand("noBaseURL");
        rb_noBaseURL.addActionListener(rb_listen);
        rb_buttonGroup.add(rb_baseURL);
        rb_buttonGroup.add(rb_noBaseURL);

        JPanel serviceBasePanel = new JPanel();
        serviceBasePanel.setBorder(new TitledBorder("Service Address"));
        serviceBasePanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2,2,2,2);
        gbc.gridy = 0;
        gbc.gridx = 0;
        serviceBasePanel.add(rb_baseURL, gbc);
        gbc.gridx = 1;
        serviceBasePanel.add(baseURL = new JTextField(30), gbc);
        gbc.gridy = 1;
        gbc.gridx = 0;
        serviceBasePanel.add(rb_noBaseURL, gbc);

        JPanel methodsPanel = setMethodsPanel();
        methodsPanel.setBorder(new TitledBorder("Service Method Definitions:"));
        add(serviceBasePanel, BorderLayout.NORTH);
        add(methodsPanel, BorderLayout.CENTER);
        setVisible(true);
    }

    private JPanel setMethodsPanel()
    {
        // Table Panel
        methodTableModel = new DefaultTableModel();
        // Create a JTable that disallow edits (edits done via dialog box only)
        methodTable = new JTable(methodTableModel) {
          public boolean isCellEditable(int rowIndex, int vColIndex) {
            return false;
          }
        };

        methodTable.setColumnSelectionAllowed(false);
        methodTable.setRowSelectionAllowed(true);
        //methodTable.setBackground(Color.WHITE);

        methodTableModel.addColumn("Method Name");
        methodTableModel.addColumn("Method Description");

        JScrollPane scrollpane = new JScrollPane(methodTable);
        scrollpane.getViewport().setBackground(Color.white);

        // Table Buttons Panel
        JButton jb1 = new JButton("New");
        jb1.setMinimumSize(new Dimension(100,30));
        jb1.setMaximumSize(new Dimension(100,30));
        jb1.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            addMethod();
          }
        } );
        JButton jb1b = new JButton("Edit");
        jb1b.setMinimumSize(new Dimension(100,30));
        jb1b.setMaximumSize(new Dimension(100,30));
        jb1b.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            editMethod();
          }
        } );
        JButton jb2 = new JButton("Delete");
        jb2.setMinimumSize(new Dimension(100,30));
        jb2.setMaximumSize(new Dimension(100,30));
        jb2.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            deleteMethod();
          }
        } );
        JButton jb3 = new JButton("Properties");
        jb3.setMinimumSize(new Dimension(100,30));
        jb3.setMaximumSize(new Dimension(100,30));
        jb3.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            addMethodProperties();
          }
        } );
        JButton jb4 = new JButton("Help");
        jb4.setMinimumSize(new Dimension(100,30));
        jb4.setMaximumSize(new Dimension(100,30));
        jb4.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            helpTable();
          }
        } );
        JPanel t_buttonPanel = new JPanel();
        //t_buttonPanel.setLayout(new GridLayout(3,1));
        t_buttonPanel.setLayout(new BoxLayout(t_buttonPanel, BoxLayout.Y_AXIS));
        t_buttonPanel.add(jb1);
        t_buttonPanel.add(jb1b);
        t_buttonPanel.add(jb2);
        t_buttonPanel.add(jb3);
        t_buttonPanel.add(jb4);

        JPanel methodsPanel = new JPanel(new BorderLayout());
        //methodsPanel.setBorder(new TitledBorder("Service Method Definitions:"));
        methodsPanel.add(scrollpane, BorderLayout.CENTER);
        methodsPanel.add(t_buttonPanel, BorderLayout.EAST);
        return methodsPanel;
    }

    // Action Listener for button group
    class BaseURLActionListener implements ActionListener
    {
      public void actionPerformed(ActionEvent e)
      {
        rb_chosen = rb_buttonGroup.getSelection().getActionCommand();
      }
    }

    public boolean hasBaseURL()
    {
      if (rb_chosen.equalsIgnoreCase("baseURL"))
      {
        return true;
      }
      return false;
    }

    public String getBaseURL()
    {
      if (hasBaseURL())
      {
        return baseURL.getText();
      }
      return null;
    }

    public HashMap getMethodMap()
    {
      return methodMap;
    }

    public Method[] getMethods()
    {
      Vector v_methods = new Vector();
      Collection c = methodMap.values();
      Iterator methods = c.iterator();
      while (methods.hasNext())
      {
         Method m = (Method)methods.next();
         v_methods.add(m);
      }
      return (Method[])v_methods.toArray(new Method[0]);
    }

    public void setBMechMethod(String methodName, String methodDesc)
      throws BMechBuilderException
    {
      if (editMethodMode)
      {
        int currentRowIndex = methodTable.getSelectedRow();
        String oldMethodName = (String)methodTable.getValueAt(currentRowIndex,0);
        Method methodData = (Method)methodMap.get(oldMethodName);
        if (!(oldMethodName.equalsIgnoreCase(methodName)))
        {
          methodMap.remove(oldMethodName);
        }
        methodData.methodName = methodName;
        methodData.methodLabel = methodDesc;
        methodMap.put(methodData.methodName, methodData);

        methodTable.setValueAt(methodData.methodName, currentRowIndex,0);
        methodTable.setValueAt(methodData.methodLabel, currentRowIndex,1);
      }
      else
      {
        if (methodMap.containsKey(methodName))
        {
          throw new BMechBuilderException("MethodsPane.setBMechMethod: Method name exists already");
        }
        methodTableModel.addRow(new Object[]{methodName, methodDesc});
        Method methodData = new Method();
        methodData.methodName = methodName;
        methodData.methodLabel = methodDesc;
        methodMap.put(methodName, methodData);
      }
    }

    public void setMethodProperties(String methodName, MethodProperties mproperties)
    {
      Method method = (Method)methodMap.get(methodName);
      method.methodProperties = mproperties;
      methodMap.put(methodName, method);

      String[] dsParmNames = method.methodProperties.dsBindingKeys;
      for (int i=0; i<dsParmNames.length; i++)
      {
        Vector dsBindingKeys = ((BMechBuilder)parent).getBMechTemplate().getDSBindingKeys();
        if (!dsBindingKeys.contains(dsParmNames[i]))
        {
          dsBindingKeys.add(dsParmNames[i]);
        }
        ((BMechBuilder)parent).getBMechTemplate().setDSBindingKeys(dsBindingKeys);
      }
    }

    private void addMethod()
    {
      methodDialog = new MethodDialog(this, "Add Method", true);
    }

    private void editMethod()
    {
      editMethodMode = true;
      int currentRowIndex = methodTable.getSelectedRow();
      methodDialog = new MethodDialog(
        this, "Edit Method", true,
        (String)methodTable.getValueAt(currentRowIndex,0),
        (String)methodTable.getValueAt(currentRowIndex,1));
      editMethodMode = false;
    }

    private void deleteMethod()
    {
      int currentRowIndex = methodTable.getSelectedRow();
      String methodName = (String)methodTable.getValueAt(currentRowIndex,0);
      methodTableModel.removeRow(currentRowIndex);
      if (methodMap.containsKey(methodName))
      {
        methodMap.remove(methodName);
      }
    }

    private void addMethodProperties()
    {
      if (methodTable.getRowCount() <= 0)
      {
        assertNoMethodMsg("You must enter a method before entering properties");
        return;
      }
      else if (methodTable.getSelectedRowCount() <=0)
      {
        assertNoMethodMsg("You must select a method row before entering properties");
        return;
      }
      else
      {
        int currentRowIndex = methodTable.getSelectedRow();
        String methodName = (String)methodTable.getValueAt(currentRowIndex,0);
        if (methodName == null || methodName.trim().equals(""))
        {
          assertNoMethodMsg("You must enter a method name before entering properties");
          return;
        }
        else
        {
          Method m = (Method)methodMap.get(methodName);
          MethodPropertiesDialog properties =
            new MethodPropertiesDialog(this, m.methodName, m.methodProperties);
        }
        return;
      }
    }

    private void helpTable()
    {
      return;
    }

    protected void assertNoMethodMsg(String msg)
    {
      JOptionPane.showMessageDialog(
        this, new String(msg), "No Method Message",
        JOptionPane.INFORMATION_MESSAGE);
    }

    protected void assertMethodExistsMsg(String msg)
    {
      JOptionPane.showMessageDialog(
        this, new String(msg), "Method Exists Message",
        JOptionPane.INFORMATION_MESSAGE);
    }
}