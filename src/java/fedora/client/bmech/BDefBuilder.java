package fedora.client.bmech;

import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import javax.swing.JComponent;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Set;
import java.util.Collection;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.io.File;
import java.io.InputStream;

import fedora.client.bmech.data.*;
import fedora.client.bmech.xml.*;
import fedora.client.ingest.AutoIngestor;

public class BDefBuilder extends JInternalFrame
{

    private JTabbedPane tabpane;
    protected BObjTemplate newBDef;
    private int selectedTabPane;
    private String s_host = null;
    private int s_port = 0;
    private String s_user = null;
    private String s_pass = null;


    public static void main(String[] args)
    {
      JFrame frame = new JFrame("BDefBuilder Test");
      frame.addWindowListener(new WindowAdapter() {
          public void windowClosing(WindowEvent e) {System.exit(0);}
      });

      frame.getContentPane().add(
        new BDefBuilder("localhost", 8080, "test", "test"),
          BorderLayout.CENTER);
      frame.setSize(700, 500);
      frame.setVisible(true);
  }

    public BDefBuilder(String host, int port, String user, String pass)
    {
        super("Behavior Definition Builder");
        s_host = host;
        s_port = port;
        s_user = user;
        s_pass = pass;
        setClosable(true);
        setMaximizable(true);
        setSize(700, 500);
        getContentPane().setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        newBDef = new BObjTemplate();

        tabpane = new JTabbedPane();
        tabpane.setBackground(Color.GRAY);
        tabpane.addTab("General", createGeneralPane());
        tabpane.addTab("Abstract Methods", createMethodsPane());
        tabpane.addTab("Documentation", createDocPane());
        // set up listener for JTabbedPane object
        tabpane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                int index = tabpane.getSelectedIndex();
                String title = tabpane.getTitleAt(index);
                System.out.println("index = " +
                                        index);
                System.out.println("title = " +
                                        title);
            }
        });


        // General Buttons Panel
        JButton save = new JButton("Save");
        save.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            saveBDef();
          }
        } );
        JButton ingest = new JButton("Ingest");
        ingest.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            ingestBDef();
          }
        } );
        JButton help = new JButton("Help");
        help.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            showHelp();
          }
        } );
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            cancelBDef();
          }
        } );
        JPanel gbuttonPanel = new JPanel();
        gbuttonPanel.setBackground(Color.WHITE);
        gbuttonPanel.add(save);
        gbuttonPanel.add(ingest);
        gbuttonPanel.add(help);
        gbuttonPanel.add(cancel);

        getContentPane().add(tabpane, BorderLayout.CENTER);
        getContentPane().add(gbuttonPanel, BorderLayout.SOUTH);
        setVisible(true);
    }

    public BObjTemplate getBObjTemplate()
    {
      return newBDef;
    }

    public void saveBDef()
    {
      BDefMETSSerializer mets = savePanelInfo();
      File file = null;
      if (mets != null)
      {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        XMLFileChooserFilter filter = new XMLFileChooserFilter();
        chooser.setFileFilter(filter);
        if (chooser.showSaveDialog(tabpane) == JFileChooser.APPROVE_OPTION)
        {
          file = chooser.getSelectedFile();
          String ext = filter.getExtension(file);
          if (ext == null || !(ext.equalsIgnoreCase("xml")))
          {
            file = new File((file.getPath() + ".xml"));
          }
          try
          {
            mets.writeMETSFile(file);
          }
          catch (Exception e)
          {
            e.printStackTrace();
            assertTabPaneMsg(("BDefBuilder: Error saving METS file for bdef: "
              + e.getMessage()), "BDefBuilder");
          }
        }
        else
        {
          assertTabPaneMsg("BDefBuilder: You did not specify a file to Save.",
            "BDefBuilder");
        }
      }
    }

    public void ingestBDef()
    {
      InputStream in = null;
      String pid = null;
      BDefMETSSerializer mets = savePanelInfo();
      if (mets != null)
      {
        try
        {
          in = mets.writeMETSStream();
        }
        catch (Exception e)
        {
          e.printStackTrace();
          assertTabPaneMsg(("BDefBuilder: Error saving METS to stream for bdef: "
            + e.getMessage()), "BDefBuilder");
        }
        try
        {
          AutoIngestor ingestor = new AutoIngestor(s_host, s_port, s_user, s_pass);
          pid = ingestor.ingestAndCommit(in, "ingest bdef object via BDefBuilder tool");
        }
        catch (Exception e)
        {
          e.printStackTrace();
          assertTabPaneMsg(("BDefBuilder: error ingesting bdef object: "
            + e.getMessage()), null);
        }
        assertTabPaneMsg(("New PID = " + pid), "Successful Ingest");
      }
    }

    public void showHelp()
    {
      return;
    }

    public void cancelBDef()
    {
      setVisible(false);
      dispose();
    }

    public BDefMETSSerializer savePanelInfo()
    {

      Component[] tabs = tabpane.getComponents();
      System.out.println("tabs count: " + tabs.length);
      for (int i=0; i < tabs.length; i++)
      {
        System.out.println("tab name: " + tabs[i].getName());
        if (tabs[i].getName().equalsIgnoreCase("GeneralTab"))
        {
          if (validGeneralTab((GeneralPane)tabs[i]))
          {
            GeneralPane gp = (GeneralPane)tabs[i];
            if (gp.rb_chosen.equalsIgnoreCase("testPID"))
            {
              newBDef.setbObjPID(gp.getBObjectPID());
            }
            else
            {
              newBDef.setbObjPID(null);
            }
            newBDef.setbObjLabel(gp.getBObjectLabel());
            newBDef.setbObjName(gp.getBObjectName());
            newBDef.setDCRecord(gp.getDCElements());
          }
          else
          {
            return null;
          }
        }
        else if (tabs[i].getName().equalsIgnoreCase("MethodsTab"))
        {
          if (validMethodsTab((MethodsPane)tabs[i]))
          {
            MethodsPane mp = (MethodsPane)tabs[i];
            newBDef.setMethodsHashMap(mp.getMethodMap());
            newBDef.setMethods(mp.getMethods());
          }
          else
          {
            return null;
          }
        }
        else if (tabs[i].getName().equalsIgnoreCase("DocumentsTab"))
        {
          if (validDocsTab((DocumentsPane)tabs[i]))
          {
            DocumentsPane docp = (DocumentsPane)tabs[i];
            newBDef.setDocDatastreams(docp.getDocDatastreams());
          }
          else
          {
            return null;
          }
        }
      }
      printBDef();
      MethodMapGenerator mmg = new MethodMapGenerator(newBDef);
      mmg.printMethodMap();
      BDefMETSSerializer mets = null;
      try
      {
        mets = new BDefMETSSerializer(newBDef, mmg.getRootElement());
      }
      catch (Exception e)
      {
        e.printStackTrace();
        assertTabPaneMsg("BDefBuilder: error in creating METS for bdef.", null);
      }
      mets.printMETS();
      return mets;
    }

    private JComponent createGeneralPane()
    {
      GeneralPane gpane = new GeneralPane(this);
      gpane.setName("GeneralTab");
      return gpane;
      //return new JLabel("Insert general stuff here.");
    }

    private JComponent createMethodsPane()
    {
      MethodsPane mpane = new MethodsPane(this);
      mpane.setName("MethodsTab");
      return mpane;
    }

    private JComponent createDocPane()
    {
      DocumentsPane docpane = new DocumentsPane();
      docpane.setName("DocumentsTab");
      return docpane;
    }

    private void printBDef()
    {
      System.out.println("FROM GENERAL TAB===============================");
      System.out.println("bDefPID: " + newBDef.getbObjPID());
      System.out.println("bDefLabel: " + newBDef.getbObjLabel());
      System.out.println("DCRecord: ");
      DCElement[] dcrecord = newBDef.getDCRecord();
      for (int i=0; i<dcrecord.length; i++)
      {
        System.out.println(">>> " + dcrecord[i].elementName + "="
          + dcrecord[i].elementValue);
      }
      System.out.println("FROM METHODS TAB===============================");
      System.out.println("methods: ");
      HashMap m2 = newBDef.getMethodsHashMap();
      Collection methods = m2.values();
      Iterator it_methods = methods.iterator();
      while (it_methods.hasNext())
      {
        Method method = (Method)it_methods.next();
        System.out.println("  method name: " + method.methodName + "\n"
          + "  method desc: " + method.methodLabel + "\n"
          + "  method URL: " + method.methodProperties.methodFullURL + "\n"
          + "  method protocol" + method.methodProperties.protocolType + "\n");
        System.out.println("  method parms:");
        int parmcnt = method.methodProperties.methodParms.length;
        for (int i=0; i<parmcnt; i++)
        {
          MethodParm mp = method.methodProperties.methodParms[i];
          System.out.println(">>>parmName: " + mp.parmName + "\n"
            + ">>>parmType: " + mp.parmType + "\n"
            + ">>>parmLabel: " + mp.parmLabel + "\n"
            + ">>>parmDefaultValue: " + mp.parmDefaultValue + "\n"
            + ">>>parmPassBy: " + mp.parmPassBy + "\n"
            + ">>>parmRequired: " + mp.parmRequired + "\n"
            + ">>>parmDomainValues: " + mp.parmDomainValues + "\n");
        }
      }
    }

    private boolean validGeneralTab(GeneralPane gp)
    {
      if (gp.getBObjectPID() == null || gp.getBObjectPID().trim().equals(""))
      {
        assertTabPaneMsg("BDefPID is missing on General Tab.", gp.getName());
        return false;
      }
      else if (gp.getBObjectLabel() == null || gp.getBObjectLabel().trim().equals(""))
      {
        assertTabPaneMsg("Behavior Mechanism Label is missing on General Tab.", gp.getName());
        return false;
      }
      else if (gp.getBObjectName() == null || gp.getBObjectName().trim().equals(""))
      {
        assertTabPaneMsg("Behavior Mechanism Nickname is missing on General Tab.", gp.getName());
        return false;
      }
      else if (gp.getDCElements().length <= 0)
      {
        assertTabPaneMsg("You must enter at least one DC element on General Tab.",
          gp.getName());
        return false;
      }
      return true;
    }

    private boolean validMethodsTab(MethodsPane mp)
    {
      if (mp.getMethods().length <=0)
      {
        assertTabPaneMsg("You must enter at least one method on AbstractMethods Tab.",
          mp.getName());
        return false;
      }
      else
      {
        Method[] methods = mp.getMethods();
        for (int i=0; i<methods.length; i++)
        {
          if (methods[i].methodProperties == null)
          {
            assertTabPaneMsg(new String("You must enter properties for method: "
              + methods[i].methodName), mp.getName());
            return false;
          }
        }
        return true;
      }
    }

    private boolean validDocsTab(DocumentsPane docp)
    {
      Datastream[] docs = docp.getDocDatastreams();
      if (docs.length < 1)
      {
          assertTabPaneMsg(new String("You must enter at least one document"
            + " that describes the behavior definition in the Documents Tab."),
            docp.getName());
          return false;
      }

      for (int i=0; i<docs.length; i++)
      {
        if (docs[i].dsLabel == null)
        {
          assertTabPaneMsg(new String("You must enter a Label for all documents"
            + "listed on the Documents Tab."), docp.getName());
          return false;
        }
        else if (docs[i].dsMIMEType == null)
        {
          assertTabPaneMsg(new String("You must enter a MIME type for all documents"
            + "listed on the Documents Tab."), docp.getName());
          return false;
        }
      }
      return true;
    }

    private void assertTabPaneMsg(String msg, String tabpane)
    {
      JOptionPane.showMessageDialog(
        this, new String(msg), new String(tabpane + " Message"),
        JOptionPane.INFORMATION_MESSAGE);
    }
  }