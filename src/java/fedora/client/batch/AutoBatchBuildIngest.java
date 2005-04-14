package fedora.client.batch;

import java.util.Properties;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/**
 *
 * <p><b>Title:</b> AutoBatchBuildIngest.java</p>
 * <p><b>Description:</b> </p>
 *
 * @author rlw@virginia.edu
 * @version $Id$
 */
public class AutoBatchBuildIngest {
    private Properties batchProperties = new Properties();

    public AutoBatchBuildIngest(String objectTemplate, String objectSpecificDir,
        String objectDir, String logFile, String logFormat, String objectFormat, String host,
        String port, String username, String password, String protocol) throws Exception {

        this.batchProperties.setProperty("merge-objects", "yes");
        this.batchProperties.setProperty("ingest", "yes");
        this.batchProperties.setProperty("template", objectTemplate);
        this.batchProperties.setProperty("specifics", objectSpecificDir);
        this.batchProperties.setProperty("objects", objectDir);
        this.batchProperties.setProperty("ingested-pids", logFile);
        this.batchProperties.setProperty("pids-format", logFormat);
        this.batchProperties.setProperty("object-format", objectFormat);
        this.batchProperties.setProperty("server-fqdn", host);
        this.batchProperties.setProperty("server-port", port);
        this.batchProperties.setProperty("username", username);
        this.batchProperties.setProperty("password", password);
		this.batchProperties.setProperty("server-protocol", protocol);

        BatchTool batchTool = new BatchTool(this.batchProperties, null, null);
        batchTool.prep();
        batchTool.process();
    }

    public static final void main(String[] args) throws Exception {
        boolean errors = false;
        String objectFormat = null;
        if (args.length == 9) {
            if (!new File(args[0]).exists() && !new File(args[0]).isFile()) {
                System.out.println("Specified object template file path: \""
                                   + args[0] + "\" does not exist.");
                errors = true;
            }
            if (!new File(args[1]).isDirectory()) {
                System.out.println("Specified object specific directory: \""
                                   + args[1] + "\" is not directory.");
                errors = true;
            }
            if (!new File(args[2]).isDirectory()) {
                System.out.println("Specified object directory: \""
                                   + args[2] + "\" is not a directory.");
                errors = true;
            }
            if (!args[4].equals("xml") && !args[4].equals("text")) {
                System.out.println("Format for log file must must be either: \""
                                   + "\"xml\"  or  \"txt\"");
                errors = true;
            }            
            String[] server = args[5].split(":");
            if (server.length != 2) {
                System.out.println("Specified server name does not specify "
                                   + "port number: \"" + args[5] + "\" .");
                errors = true;
            }
            
			if (!args[8].equals("http") && !args[8].equals("https")) {
				System.out.println("Protocl must be either: \""
								   + "\"http\"  or  \"https\"");
				errors = true;
			}
			
      	    // Verify format of template file to see if it is a METS or FOXML template
      	    BufferedReader br = new BufferedReader(new FileReader(args[0]));
      	    String line;
      	    while ((line=br.readLine()) != null) {
      	        System.out.println(line);
      	        if(line.indexOf("<foxml:")!=-1) {
      	            objectFormat = "foxml1.0";
      	        		break;
      	        }
      	        if(line.indexOf("<METS:")!=-1) {
      	            objectFormat = "metslikefedora1";
      	        		break;
      	        }      	        
      	    }
      	    br.close();
      	    br=null;
      	    
      	    if (objectFormat==null) {
      	        errors = true;
      	    }            
            if (!errors) {
                System.out.println("\n*** Format of template files is: "+objectFormat+" . Generated objects will be in "+objectFormat+" format.\n");
                AutoBatchBuildIngest autoBatch = new AutoBatchBuildIngest(args[0], args[1], args[2], args[3], args[4], objectFormat, server[0], server[1], args[6], args[7], args[8]);
            }
        } else {
            if (objectFormat==null && args.length==9) {
                System.out.println("\nUnknown format for template file.\n"
                        + "Template file must either be METS or FOXML.\n");                
            } else {
		            System.out.println("\n**** Wrong Number of Arguments *****\n");
		            System.out.println("AutoBatchBuildIngest requires 9 arguments.");
		            System.out.println("(1) - full path to object template file");
		            System.out.println("(2) - full path to object specific directory");
		            System.out.println("(3) - full path to object directory");
		            System.out.println("(4) - full path to log file");
		            System.out.println("(5) - format of log file (xml or text)\n");
		            System.out.println("(6) - host name and port of Fedora server (host:port)");
		            System.out.println("(7) - admin username of Fedora server");
		            System.out.println("(8) - password for admin user of Fedora server\n");
					System.out.println("(9) - protocol to communicate with Fedora server (http or https)");
        		}
        }
    }
}