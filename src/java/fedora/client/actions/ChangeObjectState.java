package fedora.client.actions;

import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import fedora.client.Administrator;

/**
 * Change an object or a group of object's state.
 *
 * @author cwilper@cs.cornell.edu
 * @version $Id$
 */
public class ChangeObjectState
        extends AbstractAction {

    private Set m_pids;
    private String m_newState;

    public ChangeObjectState(String pid, String stateString) {
        super(stateString);
        m_pids=new HashSet();
        m_pids.add(pid);
        m_newState=stateString.toUpperCase().substring(0, 1);
    }

    public ChangeObjectState(Set pids, String stateString) {
        super(stateString);
        m_pids=pids;
        m_newState=stateString.toUpperCase().substring(0, 1);
    }

    public void actionPerformed(ActionEvent ae) {
        String reason=JOptionPane.showInputDialog("Enter a log message for the state change.");
        if (reason!=null) {
            try {
                Iterator pidIter=m_pids.iterator();
                while (pidIter.hasNext()) {
                    String pid=(String) pidIter.next();
                    Administrator.APIM.modifyObject(pid, m_newState, null, reason);
                }
                String s="s";
                if (m_pids.size()==1) s="";
                JOptionPane.showMessageDialog(Administrator.getDesktop(),
                        "Success.\n"
                        + "Set state of " + m_pids.size() + " object" + s + " to '" + m_newState + "'.");
            } catch (Exception e) {
                String message=e.getMessage();
                if (message==null) {
                     message=e.getClass().getName();
                }
            	Administrator.showErrorDialog(Administrator.getDesktop(), "Failure Changing State", 
            			message, e);
            }
        }
    }

}