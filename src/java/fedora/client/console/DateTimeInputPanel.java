package fedora.client.console;

import java.awt.BorderLayout;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import fedora.server.utilities.DateUtility;

/**
 * <p><b>Title:</b> DateTimeInputPanel.java</p>
 * <p><b>Description:</b> Creates an input panel for entering datetime stamps.
 * The input format must be of the form:</p>
 * <ul>
 * YYYY-MM-DDTHH:mm:ss
 * </ul>
 * <p>where</p>
 * <ul>
 * <li>YYYY - 4 digit year</li>
 * <li>MM - 2 digit month</li>
 * <li>DD - 2 digit day of month</li>
 * <li>hh - 2 digit hour of day using 24 hour clock</li>
 * <li>mm - 2 digit minutes of the hour</li>
 * <li>ss - 2 digit seconds of the minute</li>
 * </ul>
 *
 * @author rlw@virginia.edu
 * @version $Id$
 */
public class DateTimeInputPanel
        extends InputPanel {
	
	private static final long serialVersionUID = 1L;
    private JRadioButton m_nullRadioButton;
    private JTextField m_textField;

    public DateTimeInputPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JPanel nullPanel=new JPanel();
        nullPanel.setLayout(new BorderLayout());
        m_nullRadioButton=new JRadioButton("Use null");
        m_nullRadioButton.setSelected(true);
        nullPanel.add(m_nullRadioButton, BorderLayout.WEST);
        add(nullPanel);

        JPanel textPanel=new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.X_AXIS));
        JRadioButton textRadioButton=new JRadioButton("Use text: ");
        textRadioButton.setSelected(false);
        textPanel.add(textRadioButton);
        m_textField=new JTextField(10);
        textPanel.add(m_textField);
        add(textPanel);

        ButtonGroup g=new ButtonGroup();
        g.add(m_nullRadioButton);
        g.add(textRadioButton);

    }

    public Object getValue() {
        if (m_nullRadioButton.isSelected()) {
            return null;
        } else {
            return DateUtility.convertStringToDate(m_textField.getText());
        }
    }

}
