package fedora.client.console;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Cursor;
import java.lang.reflect.InvocationTargetException;
import javax.swing.JDialog;

import fedora.server.utilities.MethodInvokerThread;

public class InvokeDialogListener
        implements ActionListener {
        
    private JDialog m_dialog;
    private ConsoleCommandInvoker m_invoker;
        
    public InvokeDialogListener(JDialog dialog, ConsoleCommandInvoker invoker) {
        m_dialog=dialog;
        m_invoker=invoker;
    }
    
    public void actionPerformed(ActionEvent event) {
        m_dialog.hide();
        if (event.getActionCommand().equals("Ok")) {
            try {
                MethodInvokerThread th=new MethodInvokerThread(m_invoker,
                        m_invoker.getClass().getMethod("invoke", new Class[0]), 
                        new Object[0]);
                th.start();
/*            } catch (InvocationTargetException ite) {
                System.out.println("Invocation target exception: " + ite.getMessage());
            } catch (InterruptedException ie) {
                System.out.println("Interrupted while doing request: " + ie.getMessage());
*/
            } catch (NoSuchMethodException nsme) {
                System.out.println("No such method as invoke()? This Shouldnt happen!");
            }
            // m_invoker.invoke();
        }
    }
}