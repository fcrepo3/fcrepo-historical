package fedora.client.export;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import javax.xml.rpc.ServiceException;

import fedora.client.APIMStubFactory;
import fedora.server.management.FedoraAPIM;
import fedora.server.utilities.StreamUtility;

public class AutoExporter {

    private FedoraAPIM m_apim;    

    public AutoExporter(String host, int port, String user, String pass) 
            throws MalformedURLException, ServiceException {
        m_apim=APIMStubFactory.getStub(host, port, user, pass);
    }

    public void export(String pid, OutputStream outStream) throws RemoteException, IOException {
        export(m_apim, pid, outStream);
    }

    public static void export(FedoraAPIM skeleton, String pid, OutputStream outStream) 
            throws RemoteException, IOException {
        byte[] bytes=skeleton.getObjectXML(pid); 
        for (int i=0; i<bytes.length; i++) {
            outStream.write(bytes[i]);
        }
        outStream.close();
    }

    public static void showUsage(String errMessage) {
        System.out.println("Error: " + errMessage);
        System.out.println("");
        System.out.println("Usage: AutoExporter host port username password filename pid");
    }

    public static void main(String[] args) {
        try {
            if (args.length!=6) {
                AutoExporter.showUsage("You must provide six arguments.");
            } else {
                String hostName=args[0];
                int portNum=Integer.parseInt(args[1]);
                String username=args[2];
                String password=args[3];
                String pid=args[5];
                // third arg==file... must exist
                File f=new File(args[4]);
                if (f.exists()) {
                    AutoExporter.showUsage("Third argument must be the path to a non-existing file.");
                } else {
                    AutoExporter a=new AutoExporter(hostName, portNum, username, password);
                    a.export(pid, new FileOutputStream(f));
                }
            }
        } catch (Exception e) {
            AutoExporter.showUsage(e.getClass().getName() + " - " 
                + (e.getMessage()==null ? "(no detail provided)" : e.getMessage()));
        }
    }

}