package fedora.client.export;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

import fedora.client.Administrator;
import fedora.client.APIAStubFactory;
import fedora.client.APIMStubFactory;
import fedora.client.FTypeDialog;
import fedora.client.export.AutoExporter;
import fedora.client.search.AutoFinder;

import fedora.server.access.FedoraAPIA;
import fedora.server.management.FedoraAPIM;

import fedora.server.types.gen.Condition;
import fedora.server.types.gen.ComparisonOperator;
import fedora.server.types.gen.FieldSearchQuery;
import fedora.server.types.gen.FieldSearchResult;
import fedora.server.types.gen.ListSession;
import fedora.server.types.gen.ObjectFields;

public class Export {
/*
    public static int ONE=0;
    public static int MULTI=1;

    // launch interactively
    public Export(int kind) {
        try {
            if (kind==ONE) {
                JFileChooser browse=new JFileChooser(Administrator.getLastDir());
                int returnVal = browse.showOpenDialog(Administrator.getDesktop());
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = browse.getSelectedFile();
                    Administrator.setLastDir(file.getParentFile());
                    String pid=oneFromFile(file, Administrator.APIM, null);
                    JOptionPane.showMessageDialog(Administrator.getDesktop(),
                        "Ingest succeeded.  PID='" + pid + "'.");
                }
            } else if (kind==MULTI) {
                JFileChooser browse=new JFileChooser(Administrator.getLastDir());
                browse.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int returnVal = browse.showOpenDialog(Administrator.getDesktop());
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = browse.getSelectedFile();
                    Administrator.setLastDir(file);
                    FTypeDialog dlg=new FTypeDialog();
                    if (dlg.getResult()!=null) {
                        String fTypes=dlg.getResult();
                        long st=System.currentTimeMillis();
                        String[] pids=multi(file, fTypes, Administrator.APIM);
                        long et=System.currentTimeMillis();
                        JOptionPane.showMessageDialog(Administrator.getDesktop(),
                            "Export of " + pids.length + " objects finished.\n"
                            + "Time elapsed: " + getDuration(et-st));  
                    }
                }
            }
        } catch (Exception e) {
            String msg=e.getMessage();
            if (msg==null) {
                msg=e.getClass().getName();
            }
            JOptionPane.showMessageDialog(Administrator.getDesktop(),
                    msg,
                    "Export Failure",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private static String getDuration(long millis) {
        long tsec=millis/1000;
        long h=tsec/60/60;
        long m=(tsec - (h*60*60))/60;
        long s=(tsec - (h*60*60) - (m*60));
        StringBuffer out=new StringBuffer();
        if (h>0) {
            out.append(h + " hour");
            if (h>1) out.append('s');
        }
        if (m>0) {
            if (h>0) out.append(", ");
            out.append(m + " minute");
            if (m>1) out.append('s');
        }
        if (s>0 || (h==0 && m==0)) {
            if (h>0 || m>0) out.append(", ");
            out.append(s + " second");
            if (s!=1) out.append('s');
        }
        return out.toString();
    }

    // if logMessage is null, will use original path in logMessage
    public static String oneFromFile(File file, FedoraAPIM targetRepository,
                                     String logMessage)
            throws Exception {
        System.out.println("Ingesting from file " + file.getPath());
        LAST_PATH=file.getPath();
        return AutoIngestor.ingestAndCommit(targetRepository,
                                            new FileInputStream(file),
                                            getMessage(logMessage, file));
    }
    
    // if logMessage is null, will use original path in logMessage
    public static String[] multiFromDirectory(File dir, String fTypes, 
                                              FedoraAPIM targetRepository,
                                              String logMessage)
            throws Exception {
        String tps=fTypes.toUpperCase();
        Set toIngest;
        HashSet pidSet=new HashSet();
        if (tps.indexOf("D")!=-1) {
            toIngest=getFiles(dir, "FedoraBDefObject");
            System.out.println("Found " + toIngest.size() + " behavior definitions.");
            pidSet.addAll(ingestAll(toIngest, targetRepository, logMessage)); 
        }
        if (tps.indexOf("M")!=-1) {
            toIngest=getFiles(dir, "FedoraBMechObject");
            System.out.println("Found " + toIngest.size() + " behavior mechanisms.");
            pidSet.addAll(ingestAll(toIngest, targetRepository, logMessage)); 
        }
        if (tps.indexOf("O")!=-1) {
            toIngest=getFiles(dir, "FedoraObject");
            System.out.println("Found " + toIngest.size() + " regular objects.");
            pidSet.addAll(ingestAll(toIngest, targetRepository, logMessage)); 
        }
        Iterator iter=pidSet.iterator();
        String[] pids=new String[pidSet.size()];
        int i=0;
        while (iter.hasNext()) {
            pids[i++]=(String) iter.next(); 
        }
        return pids;
    }
    
    private static Set ingestAll(Set fileSet, 
                                 FedoraAPIM targetRepository, 
                                 String logMessage) 
            throws Exception {
        HashSet set=new HashSet();
        Iterator iter=fileSet.iterator();
        while (iter.hasNext()) {
            File f=(File) iter.next();
            set.add(oneFromFile(f, targetRepository, logMessage)); 
        }
        return set;
    }
    
    private static Set getFiles(File dir, String fTypeString) 
            throws Exception {
        if (!dir.isDirectory()) {
            throw new IOException("Not a directory: " + dir.getPath());
        }
        HashSet set=new HashSet();
        File[] files=dir.listFiles();
        for (int i=0; i<files.length; i++) {
            if (files[i].isDirectory()) {
                set.addAll(getFiles(files[i], fTypeString));
            } else {
                // if the file is a candidate, add it
                BufferedReader in=new BufferedReader(new FileReader(files[i]));
                boolean isCandidate=false;
                String line;
                while ( (line=in.readLine()) != null ) {  
                    if (line.indexOf(fTypeString)!=-1) {
                        isCandidate=true;
                    }
                }
                if (isCandidate) {
                    set.add(files[i]);
                }
            }
        }
        return set;
    }
    
    // if logMessage is null, will make informative one up
    public static String oneFromRepository(FedoraAPIM sourceRepository, 
                                           String pid,
                                           FedoraAPIM targetRepository,
                                           String logMessage)
            throws Exception {
        System.out.println("Ingesting " + pid + " from source repository.");
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        AutoExporter.export(sourceRepository, 
                            pid, 
                            out, 
                            false);
        ByteArrayInputStream in=new ByteArrayInputStream(out.toByteArray());
        String realLogMessage=logMessage;
        if (realLogMessage==null) {
            realLogMessage="Ingested from source repository with pid " + pid;
        }
        return AutoIngestor.ingestAndCommit(targetRepository,
                                            in,
                                            realLogMessage);
    }
    
    // if logMessage is null, will make informative one up
    public static String[] multiFromRepository(String sourceHost,
                                               int sourcePort,
                                               String sourceUser,
                                               String sourcePass,
                                               String fTypes,
                                               FedoraAPIM targetRepos,
                                               String logMessage)
            throws Exception {
        FedoraAPIA sourceAccess=APIAStubFactory.getStub(sourceHost,
                                                        sourcePort,
                                                        sourceUser,
                                                        sourcePass);
        FedoraAPIM sourceRepos=APIMStubFactory.getStub(sourceHost,
                                                        sourcePort,
                                                        sourceUser,
                                                        sourcePass);
        return multiFromRepository(sourceAccess, sourceRepos, fTypes, targetRepos, logMessage);
   }
   
   public static String[] multiFromRepository(FedoraAPIA sourceAccess,
                                              FedoraAPIM sourceRepos,
                                              String fTypes,
                                              FedoraAPIM targetRepos,
                                              String logMessage)
            throws Exception {
        String tps=fTypes.toUpperCase();
        Set pidSet=new HashSet();
        if (tps.indexOf("D")!=-1) {
            pidSet.addAll(ingestAll(sourceAccess,
                                      sourceRepos,
                                      "D",
                                      targetRepos,
                                      logMessage));
        }
        if (tps.indexOf("M")!=-1) {
            pidSet.addAll(ingestAll(sourceAccess,
                                      sourceRepos,
                                      "M",
                                      targetRepos,
                                      logMessage));
        }
        if (tps.indexOf("O")!=-1) {
            pidSet.addAll(ingestAll(sourceAccess,
                                      sourceRepos,
                                      "O",
                                      targetRepos,
                                      logMessage));
        }
        Iterator iter=pidSet.iterator();
        String[] pids=new String[pidSet.size()];
        int i=0;
        while (iter.hasNext()) {
            pids[i++]=(String) iter.next(); 
        }
        return pids;
    }

    private static Set ingestAll(FedoraAPIA sourceAccess,
                                 FedoraAPIM sourceRepos,
                                 String fType,
                                 FedoraAPIM targetRepos,
                                 String logMessage) 
            throws Exception {
        // get pids with fType='$fType', adding all to set at once,
        // then singleFromRepository(sourceRepos, pid, targetRepos, logMessage)
        // for each, then return the set
        HashSet set=new HashSet();
        Condition cond=new Condition();
        cond.setProperty("fType");
        cond.setOperator(ComparisonOperator.fromValue("eq"));
        cond.setValue(fType);
        Condition[] conds=new Condition[1];
        conds[0]=cond;
        FieldSearchQuery query=new FieldSearchQuery();
        query.setConditions(conds);
        query.setTerms(null);
        String[] fields=new String[1];
        fields[0]="pid";
        FieldSearchResult res=AutoFinder.findObjects(sourceAccess,
                                                     fields,
                                                     1000,
                                                     query);
        boolean exhausted=false;
        while (res!=null && !exhausted) {
            ObjectFields[] ofs=res.getResultList();
            for (int i=0; i<ofs.length; i++) {
                set.add(ofs[i].getPid());
            }
            if (res.getListSession()!=null && res.getListSession().getToken()!=null) {
                res=AutoFinder.resumeFindObjects(sourceAccess, 
                                                 res.getListSession().getToken());
            } else {
                exhausted=true;
            }
        }
        String friendlyName="regular objects";
        if (fType.equals("D"))
            friendlyName="behavior definitions";
        if (fType.equals("M"))
            friendlyName="behavior mechanisms";
        System.out.println("Found " + set.size() + " " + friendlyName + " to export.");
        Iterator iter=set.iterator();
        while (iter.hasNext()) {
            String pid=(String) iter.next();
            oneFromRepository(sourceRepos,
                              pid,
                              targetRepos,
                              logMessage);
        }
        return set;
    }
    */

    /**
     * Print error message and show usage for command-line interface.
     */
    public static void badArgs(String msg) {
        System.err.println("Error  : " + msg);
        System.err.println();
        System.err.println("Command: fedora-export");
        System.err.println();
        System.err.println("Summary: Exports one or more objects from a Fedora repository.");
        System.err.println();
        System.err.println("Syntax:");
        System.err.println("  fedora-export HST:PRT USR PSS PID|FTYPS PATH");
        System.err.println();
        System.err.println("Where:");
        System.err.println("  HST        is the repository's hostname.");
        System.err.println("  PRT        is the repository's port number.");
        System.err.println("  USR        is the id of the repository user.");
        System.err.println("  PSS        is the password of repository user.");
        System.err.println("  PID        is the id of the object to export from the source repository.");
        System.err.println("  FTYPS      is any combination of the characters O, D, and M, specifying");
        System.err.println("             which Fedora object type(s) should be exported. O=regular objects,");
        System.err.println("             D=behavior definitions, and M=behavior mechanisms.");
        System.err.println("  PATH       is the directory to export to.");
        System.err.println();
        System.err.println("Examples:");
        System.err.println("fedora-export example.com:80 fedoraAdmin fedoraAdmin changeme:1 .");
        System.err.println();
        System.err.println("  Exports changeme:1 from example.com:80 to the current directory.");
        System.err.println();
        System.err.println("fedora-export example.com:80 fedoraAdmin fedoraAdmin DMO /tmp/fedoradump");
        System.err.println();
        System.err.println("  Exports all objects from example.cocm:80 to /tmp/fedoradump");
        System.err.println();
        System.exit(1);
    }

    /**
     * Command-line interface for doing exports.
     */
    public static void main(String[] args) {
        try {
            if (args.length<1) {
                Export.badArgs("Not enough arguments.");
            }
        } catch (Exception e) {
            System.err.print("Error  : ");
            if (e.getMessage()==null) {
                e.printStackTrace();
            } else {
                System.err.print(e.getMessage());
            }
        }
    }

}