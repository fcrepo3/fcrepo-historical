package fedora.client.bmech;

import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JDialog;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.FlowLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JFrame;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class MethodDialog extends JDialog {

    private JTextField methodName;
    private JTextField methodDescription;
    private MethodsPane parent;

    //public MethodDialog(JFrame owner, String title, boolean modal)
    public MethodDialog(MethodsPane parent, String title, boolean modal)
    {
        super();
        //super(owner, title, modal);
        this.parent = parent;
        setTitle(title);
        setModal(modal);
        setSize(300, 200);
        getContentPane().setLayout(new BorderLayout());

        // Text Fields Panel
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new GridLayout(3,2));
        textPanel.add(new JLabel("Method Name: "));
        textPanel.add(methodName = new JTextField());
        textPanel.add(new JLabel("Method Description: "));
        textPanel.add(methodDescription = new JTextField());

        // Dialog Buttons Panel
        JButton done = new JButton("Done");
        done.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            saveMethodFields();
          }
        } );
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            cancel();
          }
        } );
        JPanel mainButtonPanel = new JPanel();
        mainButtonPanel.add(done);
        mainButtonPanel.add(cancel);

        getContentPane().add(textPanel, BorderLayout.CENTER);
        getContentPane().add(mainButtonPanel, BorderLayout.SOUTH);
        setVisible(true);
    }

    private void saveMethodFields()
    {
      System.out.println("Saving values back in parent");
      try
      {
      parent.setBMechMethod(
        methodName.getText().trim(), methodDescription.getText().trim());
      setVisible(false);
      dispose();
      }
      catch (BMechBuilderException e)
      {
        parent.assertMethodExistsMsg("The method name already exists.");
      }
    }

    private void cancel()
    {
      setVisible(false);
      dispose();
    }
}