package fedora.server.access.localservices;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import fedora.server.storage.types.MIMETypedStream;
import fedora.server.errors.HttpServiceNotFoundException;

/**
 * <p>Title: HttpService.java</p>
 * <p>Description: Provides a local Http Behavior Mechanism service that</p>
 * <p>handles mechanisms invoking services via Http ( simple HTTP GETs, </p>
 * <p>cgi-scripts, java servlets, JSPs, etc.</p>
 *
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author Ross Wayland
 * @version 1.0
 */
public class HttpService
{
  /**
   * A method that reads the contents of the specified URL and returns the
   * result as a MIMETypedStream
   *
   * @param urlString URL of the content
   * @return MIMETypedStream
   * @throws HttpServiceNotFoundException
   */
  public MIMETypedStream getHttpContent(String urlString)
      throws HttpServiceNotFoundException
  {
    try
    {
      MIMETypedStream httpContent = null;
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      URL url = new URL(urlString);
      HttpURLConnection connection = (HttpURLConnection)url.openConnection();
      String contentType = connection.getContentType();
      InputStream is = connection.getInputStream();
      int byteStream = 0;
      while((byteStream = is.read()) >=0 )
      {
        baos.write(byteStream);
      }
      httpContent = new MIMETypedStream(contentType, baos.toByteArray());
      return(httpContent);

    } catch (Throwable th)
    {
      throw new HttpServiceNotFoundException("HTTPService ERROR: "
          + th.getClass().getName() + th.getMessage());
    }
  }

  public static void main(String[] args)
  {
    HttpService hs = new HttpService();
    String url = "http://icarus.lib.virginia.edu/test/dummy.html";
    try
    {
      MIMETypedStream content = hs.getHttpContent(url);
      System.out.println("MIME: "+content.MIMEType);
      System.out.write(content.stream);
    } catch (Exception e)
    {
      System.err.println(e.getMessage());
    }
  }
}