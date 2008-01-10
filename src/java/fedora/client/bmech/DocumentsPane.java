/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */

package fedora.client.bmech;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import fedora.client.bmech.data.Datastream;

/**
 * @author Sandy Payette
 */
public class DocumentsPane
        extends JPanel {

    private static final long serialVersionUID = 1L;

    private JTextField docLabel1;

    private JTextField docURL1;

    private JTextField docMIME1;

    private final JRadioButton rb_storeYes1;

    private final JRadioButton rb_storeNo1;

    private String rb_chosen1;

    private final ButtonGroup rb_buttonGroup1 = new ButtonGroup();

    private JTextField docLabel2;

    private JTextField docURL2;

    private JTextField docMIME2;

    private final JRadioButton rb_storeYes2;

    private final JRadioButton rb_storeNo2;

    private String rb_chosen2;

    private final ButtonGroup rb_buttonGroup2 = new ButtonGroup();

    private JTextField docLabel3;

    private JTextField docURL3;

    private JTextField docMIME3;

    private final JRadioButton rb_storeYes3;

    private final JRadioButton rb_storeNo3;

    private String rb_chosen3;

    private final ButtonGroup rb_buttonGroup3 = new ButtonGroup();

    public DocumentsPane() {
        setLayout(new GridLayout(3, 1));

        // Text Panel 1
        JPanel textPane1 = new JPanel();
        textPane1.setBorder(new TitledBorder("Service Documentation"));
        textPane1.setLayout(new GridLayout(5, 2));
        textPane1.add(new JLabel("Document Label: "));
        textPane1.add(docLabel1 = new JTextField());
        textPane1.add(new JLabel("Document URL: "));
        textPane1.add(docURL1 = new JTextField());
        textPane1.add(new JLabel("Document MIMEType: "));
        textPane1.add(docMIME1 = new JTextField());

        ActionListener rb_listen1 = new Doc1ActionListener();
        rb_storeNo1 = new JRadioButton("no", true);
        rb_storeNo1.setActionCommand("no1");
        rb_storeNo1.addActionListener(rb_listen1);
        rb_storeYes1 = new JRadioButton("yes", false);
        rb_storeYes1.setActionCommand("yes1");
        rb_storeYes1.addActionListener(rb_listen1);
        rb_chosen1 = "no1";
        rb_buttonGroup1.add(rb_storeYes1);
        rb_buttonGroup1.add(rb_storeNo1);

        textPane1.add(new JLabel("Store Local Copy?: "));
        textPane1.add(rb_storeNo1);
        textPane1.add(new JLabel(""));
        textPane1.add(rb_storeYes1);

        // Text Panel 2
        JPanel textPane2 = new JPanel();
        textPane2.setBorder(new TitledBorder("Service Documentation"));
        textPane2.setLayout(new GridLayout(5, 2));
        textPane2.add(new JLabel("Document Label: "));
        textPane2.add(docLabel2 = new JTextField());
        textPane2.add(new JLabel("Document URL: "));
        textPane2.add(docURL2 = new JTextField());
        textPane2.add(new JLabel("Document MIMEType: "));
        textPane2.add(docMIME2 = new JTextField());

        ActionListener rb_listen2 = new Doc1ActionListener();
        rb_storeNo2 = new JRadioButton("no", true);
        rb_storeNo2.setActionCommand("no2");
        rb_storeNo2.addActionListener(rb_listen2);
        rb_storeYes2 = new JRadioButton("yes", false);
        rb_storeYes2.setActionCommand("yes2");
        rb_storeYes2.addActionListener(rb_listen2);
        rb_chosen2 = "no2";
        rb_buttonGroup2.add(rb_storeYes2);
        rb_buttonGroup2.add(rb_storeNo2);

        textPane2.add(new JLabel("Store Local Copy?: "));
        textPane2.add(rb_storeNo2);
        textPane2.add(new JLabel(""));
        textPane2.add(rb_storeYes2);

        // Text Panel 3
        JPanel textPane3 = new JPanel();
        textPane3.setBorder(new TitledBorder("Service Documentation"));
        textPane3.setLayout(new GridLayout(5, 2));
        textPane3.add(new JLabel("Document Label: "));
        textPane3.add(docLabel3 = new JTextField());
        textPane3.add(new JLabel("Document URL: "));
        textPane3.add(docURL3 = new JTextField());
        textPane3.add(new JLabel("Document MIMEType: "));
        textPane3.add(docMIME3 = new JTextField());

        ActionListener rb_listen3 = new Doc1ActionListener();
        rb_storeNo3 = new JRadioButton("no", true);
        rb_storeNo3.setActionCommand("no3");
        rb_storeNo3.addActionListener(rb_listen3);
        rb_storeYes3 = new JRadioButton("yes", false);
        rb_storeYes3.setActionCommand("yes3");
        rb_storeYes3.addActionListener(rb_listen3);
        rb_chosen3 = "no3";
        rb_buttonGroup3.add(rb_storeYes3);
        rb_buttonGroup3.add(rb_storeNo3);

        textPane3.add(new JLabel("Store Local Copy?: "));
        textPane3.add(rb_storeNo3);
        textPane3.add(new JLabel(""));
        textPane3.add(rb_storeYes3);

        add(textPane1);
        add(textPane2);
        add(textPane3);
        setVisible(true);
    }

    public Datastream[] getDocDatastreams() {
        Vector docs = new Vector();
        if (docURL1.getText() != null && !docURL1.getText().trim().equals("")) {
            Datastream doc = new Datastream();
            doc.dsLabel = docLabel1.getText();
            doc.dsURL = docURL1.getText();
            doc.dsMIMEType = docMIME1.getText();
            if (rb_chosen1.equalsIgnoreCase("no1")) {
                // store as external referenced content
                doc.dsControlGrpType = "E";
            } else {
                // store as managed content
                doc.dsControlGrpType = "M";
            }
            docs.add(doc);
        }

        if (docURL2.getText() != null && !docURL2.getText().trim().equals("")) {
            Datastream doc = new Datastream();
            doc.dsLabel = docLabel2.getText();
            doc.dsURL = docURL2.getText();
            doc.dsMIMEType = docMIME2.getText();
            if (rb_chosen2.equalsIgnoreCase("no2")) {
                // store as external referenced content
                doc.dsControlGrpType = "E";
            } else {
                // store as managed content
                doc.dsControlGrpType = "M";
            }
            docs.add(doc);
        }

        if (docURL3.getText() != null && !docURL3.getText().trim().equals("")) {
            Datastream doc = new Datastream();
            doc.dsLabel = docLabel3.getText();
            doc.dsURL = docURL3.getText();
            doc.dsMIMEType = docMIME3.getText();
            if (rb_chosen3.equalsIgnoreCase("no3")) {
                // store as external referenced content
                doc.dsControlGrpType = "E";
            } else {
                // store as managed content
                doc.dsControlGrpType = "M";
            }
            docs.add(doc);
        }
        return (Datastream[]) docs.toArray(new Datastream[0]);
    }

    // Action Listener for button group
    class Doc1ActionListener
            implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            rb_chosen1 = rb_buttonGroup1.getSelection().getActionCommand();
        }
    }

    class Doc2ActionListener
            implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            rb_chosen2 = rb_buttonGroup2.getSelection().getActionCommand();
        }
    }

    class Doc3ActionListener
            implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            rb_chosen3 = rb_buttonGroup3.getSelection().getActionCommand();
        }
    }
}
