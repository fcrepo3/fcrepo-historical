package fedora.client;

import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.MultipartPostMethod;
import org.apache.log4j.Logger;

import fedora.client.FedoraClient;
import fedora.server.utilities.StreamUtility;

/**
 * A client to a Fedora server's upload facility, accessed via a 
 * basic-authenticated multipart POST to the server.
 *
 * See server.management.UploadServlet for protocol details.
 * 
 * @author cwilper@cs.cornell.edu
 */
public class Uploader {

    private MultiThreadedHttpConnectionManager m_cManager=
            new MultiThreadedHttpConnectionManager();

    private String m_uploadURL;
    private UsernamePasswordCredentials m_creds;
    private FedoraClient fc;
    private static final Logger logger =
        Logger.getLogger(FedoraClient.class.getName());    

    /**
     * Construct an uploader to a certain repository as a certain user.
     */
    public Uploader(String host, int port, String user, String pass)
            throws IOException {
        m_uploadURL=Administrator.getProtocol() + "://" + host + ":" + port + "/fedora/management/upload";
        m_creds=new UsernamePasswordCredentials(user, pass);
        String baseURL = Administrator.getProtocol() + "://" + host + ":" + port + "/fedora";
        fc = new FedoraClient(baseURL, user, pass);
    }
    
    /**
     * Construct an uploader to a certain repository as a certain user.
     */
    public Uploader(String protocol, String host, int port, String user, String pass)
            throws IOException 
    {
        m_uploadURL = protocol + "://" + host + ":" + port + "/fedora/management/upload";
        m_creds = new UsernamePasswordCredentials(user, pass);
        String baseURL = protocol + "://" + host + ":" + port + "/fedora";
        fc = new FedoraClient(baseURL, user, pass);        
    }

    /**
     * Send the data from the stream to the server.
     * 
     * This is less efficient than <i>upload(File)</i>, but if you already
     * have a stream, it's convenient.
     *
     * This method takes care of temporarily making a File out of the stream, 
     * making the request, and removing the temporary file.  Having a File 
     * source for the upload is necessary because the content-length must be 
     * sent along with the request as per the HTTP Multipart POST protocol spec.
     */
    public String upload(InputStream in) throws IOException {
        File tempFile=File.createTempFile("fedora-upload-", null);
        FileOutputStream out=new FileOutputStream(tempFile);
        try {
            StreamUtility.pipeStream(in, out, 8192);
            return upload(tempFile);
        } finally {
            in.close();
            out.close();
            if (!tempFile.delete()) {
                System.err.println("WARNING: Could not remove temporary file: "
                        + tempFile.getName());
                tempFile.deleteOnExit();
            }
        }
    }

    /**
     * Send a file to the server, getting back the identifier.
     */
    public String upload(File in) throws IOException {
        MultipartPostMethod post=null;
        
        try {
            // Ping m_uploadURL using HTTP GET to see if any redirects
            // If it is a redirect, use the redirectLocation as uploadURL to perform POST;
            // otherwise, just use m_uploadURL.
            //
            // Note: It would save some coding to just use FedoraClient.get(URL, boolean, boolean),
            // but can't use FedoraClient.get(m_updateURL, true, true) here because this method
            // attempts to evaluate the redirected URL which in this case is /management/upload. This
            // URL is handled by UploadServlet which only accepts POSTS and returns SC_BAD_REQUEST(400)
            // for GET requests so FedoraClient.get(m_updateURL, true, true) will return IOException
            // because of the 400 error.
            HttpClient client = fc.getHttpClient();
            String redirectURL = m_uploadURL;
            
            // Only look for redirect if protocol is http.
            if (m_uploadURL.startsWith("http:")) {
        		GetMethod getMethod = new GetMethod(m_uploadURL);
        		getMethod.setDoAuthentication(true);
        		getMethod.setFollowRedirects(true);
        		HttpInputStream his = new HttpInputStream(client, getMethod, m_uploadURL);
        		int status = his.getStatusCode();
      			if (status != 200) {
					if (300 <= status && status <= 399) {
						// Handle the redirect here !
						logger.debug("Uploader is handling redirect for HTTP STATUS=" + status);      					    
						Header hLoc = his.getResponseHeader("location");
						if (hLoc != null) {
							redirectURL = hLoc.getValue();
							logger.debug("Uploader is trying redirect location: " + hLoc.getValue());      							
						} else {
							try { 
								throw new IOException("Request failed [" + status + " " + his.getStatusText() + "]");
							} finally {
								try { his.close(); } catch (Exception e) {logger.error("Can't close InputStream: " + e.getMessage());}
							}
						}
					} else {
						try { 
							throw new IOException("Request failed [" + his.getStatusCode() + " " + his.getStatusText() + "]");
						} finally {
							try { his.close(); } catch (Exception e) {logger.error("Can't close InputStream: " + e.getMessage());}
						}
					}
				}        		
            }
            client=fc.getHttpClient();
            post=new MultipartPostMethod(redirectURL);
            post.setDoAuthentication(true);
            post.addParameter("file", in);
            int resultCode=0;
            if (Administrator.INSTANCE!=null) {
                // do the work in a separate thread
                // construct the message
                StringBuffer msg=new StringBuffer();
                msg.append("Uploading " + in.length() + " bytes ");
                msg.append("to " + m_uploadURL);
                // paint it to the progress bar
                Dimension d=null;
                d=Administrator.PROGRESS.getSize();
                Administrator.PROGRESS.setString(msg.toString());
                Administrator.PROGRESS.setValue(100);
                Administrator.PROGRESS.paintImmediately(0, 0, (int) d.getWidth()-1, (int) d.getHeight()-1);
                // then start the thread, passing parms in
                HashMap PARMS=new HashMap();
                PARMS.put("client", client);
                PARMS.put("post", post);
                SwingWorker worker=new SwingWorker(PARMS) {
                    public Object construct() {
                        try {
                            return new Integer(((HttpClient) parms.get("client")).executeMethod((MultipartPostMethod) parms.get("post")));
                        } catch (Exception e) {
                            thrownException=e;
                            return "";
                        }
                    }
                };
                worker.start();
                // The following code will run in the (safe) 
                // Swing event dispatcher thread.
                int ms=200;
                while (!worker.done) {
                    try {
                        Administrator.PROGRESS.setValue(ms);
                        Administrator.PROGRESS.paintImmediately(0, 0, (int) d.getWidth()-1, (int) d.getHeight()-1);
                        Thread.sleep(100);
                        ms=ms+100;
                        if (ms>=2000) ms=200;
                    } catch (InterruptedException ie) { }
                }
                if (worker.thrownException!=null)
                    throw worker.thrownException;
                Administrator.PROGRESS.setValue(2000);
                Administrator.PROGRESS.paintImmediately(0, 0, (int) d.getWidth()-1, (int) d.getHeight()-1);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) { }
                resultCode=((Integer) worker.getValue()).intValue();
            } else {
                resultCode=client.executeMethod(post);
            }
            if (resultCode!=201) {
                throw new IOException(HttpStatus.getStatusText(resultCode)
                        + ": " 
                        + replaceNewlines(post.getResponseBodyAsString(), " "));
            }
            return replaceNewlines(post.getResponseBodyAsString(), "");
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        } finally {
            if (post!=null) post.releaseConnection();
        }

    }

    /**
     * Replace newlines with the given string.
     */
    private static String replaceNewlines(String in, String replaceWith) {
        return in.replaceAll("\r", replaceWith).replaceAll("\n", replaceWith);
    }

    /**
     * Test this class by uploading the given file three times.
     *
     * First, with the provided credentials, as an InputStream.
     * Second, with the provided credentials, as a File.
     * Third, with bogus credentials, as a File.
     */
    public static void main(String[] args) {
        try {
            if (args.length==5) {
                Uploader uploader=new Uploader(args[0], Integer.parseInt(args[1]), args[2], args[3]);
                File f=new File(args[4]);
                System.out.println(uploader.upload(new FileInputStream(f)));
                System.out.println(uploader.upload(f));
                uploader=new Uploader(args[0], Integer.parseInt(args[1]), args[2], args[3] + "test");
                System.out.println(uploader.upload(f));
            } else {
                System.err.println("Usage: Uploader host port user pass file");
            }
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
        }
    }

}