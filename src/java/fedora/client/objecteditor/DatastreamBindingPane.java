package fedora.client.objecteditor;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.*;

import fedora.client.Administrator;
import fedora.client.objecteditor.types.DatastreamInputSpec;
import fedora.client.objecteditor.types.DatastreamBindingRule;
import fedora.server.types.gen.Datastream;
import fedora.server.types.gen.DatastreamBinding;
import fedora.server.types.gen.Disseminator;


public class DatastreamBindingPane
        extends JPanel
        implements DatastreamListener {

    private Disseminator m_diss;
    private Datastream[] m_datastreams;
    private DatastreamInputSpec m_spec;
    private HashMap m_ruleForKey;
    private static DatastreamBindingComparator s_dsBindingComparator=
            new DatastreamBindingComparator();

    static ImageIcon notFulfilledIcon=new ImageIcon(Administrator.cl.getResource("images/fedora/exclaim16.gif"));
    static ImageIcon fulfilledIcon=new ImageIcon(Administrator.cl.getResource("images/fedora/checkmark16.gif"));

    public DatastreamBindingPane(Datastream[] currentVersions,
                                 Disseminator diss,
                                 DatastreamInputSpec spec) {
        m_diss=diss;
        m_datastreams=currentVersions;
        m_spec=spec;

        // put rules in a hash by key so they're easy to use later
        m_ruleForKey=new HashMap();
        for (int i=0; i<spec.bindingRules().size(); i++) {
            DatastreamBindingRule rule=(DatastreamBindingRule) spec.bindingRules().get(i);
            m_ruleForKey.put(rule.getKey(), rule);
        }

        // sort existing values, prepping for putting them in the table model
        SortedMap dsBindingMap=getSortedBindingMap(
                m_diss.getDsBindMap().getDsBindings());

        // construct the tabbedpane, one tab per binding key
        JTabbedPane bindingTabbedPane=new JTabbedPane();
        bindingTabbedPane.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
        Iterator keys=dsBindingMap.keySet().iterator();
        int tabNum=-1;
        while (keys.hasNext()) {
            tabNum++;
            String key=(String) keys.next();
            Set values=(Set) dsBindingMap.get(key);
            SingleKeyBindingPanel p=new SingleKeyBindingPanel(
                    key, 
                    values, 
                    (DatastreamBindingRule) m_ruleForKey.get(key));
            bindingTabbedPane.add(key, p);
            bindingTabbedPane.setBackgroundAt(tabNum, Administrator.DEFAULT_COLOR);
            if (tabNum==0) {
                bindingTabbedPane.setIconAt(tabNum, notFulfilledIcon);
            } else {
                bindingTabbedPane.setIconAt(tabNum, fulfilledIcon);
            }
        }
        setLayout(new BorderLayout());
        add(bindingTabbedPane, BorderLayout.CENTER);

    }

    public void datastreamAdded(Datastream ds) {
        // append to the end of the array
        Datastream[] newArray=new Datastream[m_datastreams.length+1];
        for (int i=0; i<m_datastreams.length; i++) {
            newArray[i]=m_datastreams[i];
        }
        newArray[m_datastreams.length]=ds;
        m_datastreams=newArray;
    }

    public void datastreamModified(Datastream ds) {
        // swap the value in the array
        for (int i=0; i<m_datastreams.length; i++) {
            if (ds.getID().equals(m_datastreams[i].getID())) {
                m_datastreams[i]=ds;
            }
        }
    }

    public void datastreamPurged(String dsID) {
        // remove the datastream from the array, if it's there
        int where=-1;
        for (int i=0; i<m_datastreams.length; i++) {
            if (dsID.equals(m_datastreams[i].getID())) {
                where=i;
            }
        }
        if (where!=-1) {
            Datastream[] newArray=new Datastream[m_datastreams.length-1];
            for (int i=0; i<m_datastreams.length-1; i++) {
                if (i<where) {
                    newArray[i]=m_datastreams[i];
                } else if (i>where) {
                    newArray[i-1]=m_datastreams[i];
                }
            }
            m_datastreams=newArray;
        }
    }

    /**
     * Get a SortedMap of SortedSets of DatastreamBinding objects, keyed
     * by sorted binding key, and sorted by seqNo, respectively.
     */
    public static SortedMap getSortedBindingMap(DatastreamBinding[] bindingArray) {
        TreeMap map=new TreeMap(); // automagically sorts by key
        for (int i=0; i<bindingArray.length; i++) {
            String key=bindingArray[i].getBindKeyName();
            if (!map.containsKey(key)) {
                map.put(key, new TreeSet(s_dsBindingComparator));
            }
            ((SortedSet) map.get(key)).add(bindingArray[i]);
        }
        return map;
    }

    class SingleKeyBindingPanel
            extends JPanel {

        private DatastreamBindingTableModel m_tableModel;
        private JTable m_table;
        private JLabel m_statusLabel;
        private JButton m_addButton;
        private JButton m_insertButton;
        private JButton m_removeButton;
        private JButton m_upButton;
        private JButton m_downButton;

        public SingleKeyBindingPanel(String bindingKey, 
                                     Set dsBindings, 
                                     DatastreamBindingRule rule) {
            // TODO: create a copy of dsBindings here so we can impl isDirty()
            JTextArea topTextArea=createTopTextArea(rule);
            topTextArea.setBackground(getBackground());

            JPanel statusPane=new JPanel();
            statusPane.setBorder(BorderFactory.createEmptyBorder(4,0,0,0));
            m_statusLabel=new JLabel("2 more datastreams needed.", notFulfilledIcon, SwingConstants.TRAILING);
            statusPane.add(m_statusLabel);

            m_tableModel=new DatastreamBindingTableModel(dsBindings);
            m_table=new JTable(m_tableModel);
            m_table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
            m_table.setRowSelectionAllowed(true);
            m_table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            m_table.setShowVerticalLines(false);
            m_table.getColumnModel().getColumn(0).setMinWidth(90);
            m_table.getColumnModel().getColumn(0).setMaxWidth(90);            

            JPanel middlePane=new JPanel(new BorderLayout());
            middlePane.add(new JScrollPane(m_table), BorderLayout.CENTER);
            middlePane.add(statusPane, BorderLayout.SOUTH);

            GridBagLayout gridbag=new GridBagLayout();
            JPanel buttonPane=new JPanel(gridbag);
            buttonPane.setBorder(BorderFactory.createEmptyBorder(0,4,0,0));
            GridBagConstraints c=new GridBagConstraints();
            c.gridx=0;
            c.fill=GridBagConstraints.BOTH;
            c.anchor=GridBagConstraints.NORTH;
            c.weightx=1.0;
            c.weighty=0.0;

            m_addButton=new JButton("Add...");
            gridbag.setConstraints(m_addButton, c);
            buttonPane.add(m_addButton);
            m_addButton.setMinimumSize(new Dimension(2,18));

            m_insertButton=new JButton("Insert...");
            gridbag.setConstraints(m_insertButton, c);
            buttonPane.add(m_insertButton);
            m_insertButton.setMinimumSize(new Dimension(2,18));

            m_removeButton=new JButton("Remove");
            gridbag.setConstraints(m_removeButton, c);
            buttonPane.add(m_removeButton);
            m_removeButton.setMinimumSize(new Dimension(2,18));

            Component strut=Box.createVerticalStrut(8);
            gridbag.setConstraints(strut, c);
            buttonPane.add(strut);

            m_upButton=new JButton("Up");
            gridbag.setConstraints(m_upButton, c);
            buttonPane.add(m_upButton);
            m_upButton.setMinimumSize(new Dimension(2,18));

            m_downButton=new JButton("Down");
            gridbag.setConstraints(m_downButton, c);
            buttonPane.add(m_downButton);
            m_downButton.setMinimumSize(new Dimension(2,18));

            c.weighty=1.0;
            c.fill=GridBagConstraints.VERTICAL;
            Component glue=Box.createVerticalGlue();
            gridbag.setConstraints(glue, c);
            buttonPane.add(glue);

            JPanel bottomPane=new JPanel(new BorderLayout());
            bottomPane.setBorder(BorderFactory.createEmptyBorder(4,0,0,0));
            bottomPane.add(middlePane, BorderLayout.CENTER);
            bottomPane.add(buttonPane, BorderLayout.EAST);

            setLayout(new BorderLayout());
            setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
            add(topTextArea, BorderLayout.NORTH);
            add(bottomPane, BorderLayout.CENTER);
        }

        private JTextArea createTopTextArea(
                DatastreamBindingRule rule) {
            StringBuffer buf=new StringBuffer();
            // requires x to y datastreams...
            buf.append("Requires ");
            if (rule.getMin()==0) {
                if (rule.getMax()==-1) {
                    buf.append("any number of datastreams");
                } else {
                    buf.append("up to ");
                    buf.append(rule.getMax());
                    buf.append(" datastream");
                    if (rule.getMax()>1) {
                    }
                }
            } else {
                if (rule.getMin()==rule.getMax()) {
                    buf.append(rule.getMin());
                    buf.append(" datastream");
                    if (rule.getMax()>1) {
                        buf.append('s');
                    }
                } else {
                    buf.append(rule.getMin());
                    buf.append(" to ");
                    buf.append(rule.getMax());
                    buf.append(" datastreams");
                }
            }
            // of type...
            String[] types=rule.getTypes();
            buf.append(" of ");
            if (rule.accepts("*/*")) {
                buf.append("any type.");
            } else {
                buf.append("type ");
                buf.append(types[0]);
                if (types.length==2) {
                    buf.append(" or ");
                    buf.append(types[1]);
                } else if (types.length>2) {
                    for (int i=1; i<types.length; i++) {
                        buf.append(", ");
                        if (i==types.length-1) {
                            buf.append("or ");
                        }
                        buf.append(types[i]);
                    }
                }
            }
            // add \n(inputInstruction) if available
            if (rule.getInputInstruction()!=null 
                    && rule.getInputInstruction().length()>0) {
                buf.append(": ");
                buf.append(rule.getInputInstruction());
            } else {
                buf.append(".");
            }
            // finally, set up and return the JTextArea
            JTextArea result=new JTextArea(buf.toString());
            result.setLineWrap(true);
            result.setEditable(false);
            result.setWrapStyleWord(true);
            return result;
        }

    }

    class DatastreamBindingTableModel
            extends AbstractTableModel {

        public DatastreamBinding[] m_bindings;        

        public DatastreamBindingTableModel(Set values) {
            m_bindings=new DatastreamBinding[values.size()*10];
            Iterator iter=values.iterator();
            int i=0;
            while (iter.hasNext()) {
                DatastreamBinding n=(DatastreamBinding) iter.next();
                m_bindings[i++]=n;
                m_bindings[i++]=n;
                m_bindings[i++]=n;
                m_bindings[i++]=n;
                m_bindings[i++]=n;
                m_bindings[i++]=n;
                m_bindings[i++]=n;
                m_bindings[i++]=n;
                m_bindings[i++]=n;
                m_bindings[i++]=n;
            }
        }

        public int getRowCount() {
            return m_bindings.length;
        }

        public int getColumnCount() {
            return 2;
        }

        public String getColumnName(int i) {
            if (i==0) return "Datastream";
            return "Binding Label";
        }

        public boolean isCellEditable(int row, int column) {
            return (column==1);
        }

        public Object getValueAt(int row, int column) {
            if (column==0) {
                return m_bindings[row].getDatastreamID();
            } else {
                return m_bindings[row].getBindLabel();
            }
        }

        public void setValueAt(Object value, int row, int column) {
            if (column==1) {
                m_bindings[row].setBindLabel((String) value);
                fireTableCellUpdated(row, column);
            }
        }
        
    }

}