package fedora.server.test;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author Ross Wayland
 * @version 1.0
 */


import javax.xml.namespace.QName;
import org.apache.axis.client.Service;
import org.apache.axis.client.Call;
import java.util.Calendar;
import java.io.ByteArrayInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;


public class TestClientAPIA
{

  public TestClientAPIA()
  {
  }

  public static void main(String[] args)
  {
    String PID = "uva-lib:1225";
    String qName1 = "http://www.fedora.info/definitions/1/0/api/";
    String endpoint = "http://localhost:8080/fedora/access/soap";
    Calendar asOfDate = null;

    try
    {
      Service service = new Service();
      Call call = (Call) service.createCall();
      call.setTargetEndpointAddress( new java.net.URL(endpoint) );
      call.setOperationName(new javax.xml.namespace.QName(qName1,
          "GetBehaviorDefinitions") );

      String[] bDefs = (String[])call.invoke(new Object[] { PID });
      System.out.println("\n*****GETBEHAVIORDEFINITIONS RESULTS*****");
      for (int i=0; i<bDefs.length; i++)
      {
        System.out.println("bDef["+i+"] = "+bDefs[i]);
      }

      // Test GetBehaviormethods
      String bDefPID = "uva-bdef-image-w:101";
      call.setOperationName(new javax.xml.namespace.QName(qName1,
          "GetBehaviorMethods") );
      QName qn = new QName("http://www.fedora.info/definitions/1/0/types/",
                            "MethodDef");
      QName qn2 = new QName("http://www.fedora.info/definitions/1/0/types/",
                            "MethodParmDef");
      call.registerTypeMapping(fedora.server.types.gen.MethodDef.class,
          qn,
          new org.apache.axis.encoding.ser.BeanSerializerFactory(
          fedora.server.types.gen.MethodDef.class, qn),
          new org.apache.axis.encoding.ser.BeanDeserializerFactory(
          fedora.server.types.gen.MethodDef.class, qn));
      call.registerTypeMapping(fedora.server.types.gen.MethodParmDef.class,
          qn2,
          new org.apache.axis.encoding.ser.BeanSerializerFactory(
          fedora.server.types.gen.MethodParmDef.class, qn2),
          new org.apache.axis.encoding.ser.BeanDeserializerFactory(
          fedora.server.types.gen.MethodParmDef.class, qn2));
      fedora.server.types.gen.MethodDef[] methodDefs =
          (fedora.server.types.gen.MethodDef[])
          call.invoke( new Object[] { PID, bDefPID, asOfDate} );
      System.out.println("\n\n*****GETBEHAVIORMETHODS RESULTS*****");
      for (int i=0; i<methodDefs.length; i++)
      {
        System.out.println("httpBindingAddress :"+
            methodDefs[i].getHttpBindingAddress());
        System.out.println("httpBindingOperationLocation :"+
            methodDefs[i].getHttpBindingOperationLocation());
        System.out.println("httpBindingURL :"+
            methodDefs[i].getHttpBindingURL());
        System.out.println("methodLabel :"+
            methodDefs[i].getMethodLabel());
        System.out.println("methodName :"+
            methodDefs[i].getMethodName());
        fedora.server.types.gen.MethodParmDef[] parmDefs = null;
        parmDefs = methodDefs[i].getMethodParms();
        if (parmDefs != null)
        {
          for (int j=0; j<parmDefs.length; j++)
          {
            System.out.println("parmDefaultValue :"+
                parmDefs[j].getParmDefaultValue());
            System.out.println("parmLabel :"+parmDefs[j].getParmLabel());
            System.out.println("parmName :"+parmDefs[j].getParmName());
            System.out.println("parmRequired :"+parmDefs[j].isParmRequired());
          }
        }
      }

      // Test GetBehaviormethodsAsWSDL
       bDefPID = "uva-bdef-image-w:101";
       call.setOperationName(new javax.xml.namespace.QName(qName1,
           "GetBehaviorMethodsAsWSDL") );
       qn = new QName("http://www.fedora.info/definitions/1/0/types/",
                             "MIMETypedStream");
       call.registerTypeMapping(fedora.server.types.gen.MIMETypedStream.class,
           qn,
           new org.apache.axis.encoding.ser.BeanSerializerFactory(
           fedora.server.types.gen.MIMETypedStream.class, qn),
           new org.apache.axis.encoding.ser.BeanDeserializerFactory(
           fedora.server.types.gen.MIMETypedStream.class, qn));
       fedora.server.types.gen.MIMETypedStream methodWSDL =
           (fedora.server.types.gen.MIMETypedStream)
           call.invoke( new Object[] { PID, bDefPID, asOfDate} );
       String mime = methodWSDL.getMIMEType();
       System.out.println("\n\n*****GETBEHAVIORMETHODS RESULTS*****\nMIME: "+
                          mime);
       ByteArrayInputStream bais =
           new ByteArrayInputStream(methodWSDL.getStream());
       int c = 0;
       while ((c = bais.read()) > 0)
       {
         System.out.write(c);
      }

      // Test GetDissemination
      PID = "1007.lib.dl.test/text_ead/viu00003";
      bDefPID = "web_ead";
      String method = "get_admin";
      call.setOperationName(new javax.xml.namespace.QName(qName1,
          "GetDissemination") );
      fedora.server.types.gen.MIMETypedStream dissemination =
          (fedora.server.types.gen.MIMETypedStream)
          call.invoke( new Object[] { PID, bDefPID, method, asOfDate} );
      if (dissemination != null)
      {
        mime = dissemination.getMIMEType();
        System.out.println("\n\n****DISSEMINATION RESULTS*****\n"+
                           "Dissemination MIME: "+mime);
        BufferedReader br = new BufferedReader(
            new InputStreamReader(
            new ByteArrayInputStream(dissemination.getStream())));

        String line = null;
        while ((line=br.readLine()) != null )
        {
          System.out.println(line);
        }
      }


    //Test View Objecct

    call.setOperationName(new javax.xml.namespace.QName(qName1,
        "GetObjectMethods") );
    fedora.server.types.gen.ObjectMethodsDef[] objectView = null;
    qn = new QName("http://www.fedora.info/definitions/1/0/types/",
                            "ObjectMethodsDef");
    call.registerTypeMapping(fedora.server.types.gen.ObjectMethodsDef.class,
        qn,
        new org.apache.axis.encoding.ser.BeanSerializerFactory(
        fedora.server.types.gen.ObjectMethodsDef.class, qn),
        new org.apache.axis.encoding.ser.BeanDeserializerFactory(
        fedora.server.types.gen.ObjectMethodsDef.class, qn));
    objectView = (fedora.server.types.gen.ObjectMethodsDef[])
      call.invoke( new Object[] { PID, asOfDate} );
    //Object[] o = objectView;
    for (int i=0; i<objectView.length; i++)
    {
      fedora.server.types.gen.ObjectMethodsDef ov =
               new fedora.server.types.gen.ObjectMethodsDef();
      ov = objectView[i];
      System.out.println("objDef["+i+"] "+
                         "\np1"+ov.getPID()+
                         "\np2"+ov.getBDefPID()+
                         "\np3"+ov.getMethodName()+
                         "\np4"+ov.getAsOfDate());
    }
    } catch (Exception e)
    {
      e.printStackTrace();
      System.out.println(e.getStackTrace());
    }

  }
}