/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */

package fedora.client.bmech.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import javax.xml.transform.dom.DOMResult;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import fedora.common.Constants;

import fedora.client.bmech.data.*;
import fedora.client.bmech.BMechBuilderException;

/**
 * @author payette@cs.cornell.edu
 */
public class MethodMapGenerator
    implements Constants {

  private Document document;

  public MethodMapGenerator(BMechTemplate newBMech)
    throws BMechBuilderException
  {
    createDOM();
    genMethodMap(newBMech);
  }

  public MethodMapGenerator(BObjTemplate newBDef)
    throws BMechBuilderException
  {
    createDOM();
    genMethodMap(newBDef);
  }

  private void createDOM() throws BMechBuilderException
  {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    try
    {
        DocumentBuilder builder =   factory.newDocumentBuilder();
        document = builder.newDocument();

    }
    catch (ParserConfigurationException pce)
    {
      // Parser with specified options can't be built
      pce.printStackTrace();
      throw new BMechBuilderException("MethodMapGenerator: error configuring parser."
        + "Underlying exception: " + pce.getMessage());
    }
  }

  private void genMethodMap(BObjTemplate newBDef)
  {
    Method[] methods = newBDef.getMethods();
    Element root = (Element)document.createElementNS(METHOD_MAP.uri, "fmm:MethodMap");
    root.setAttributeNS(XMLNS.uri, "xmlns:fmm", METHOD_MAP.uri);
    String bdeflabel = (newBDef.getbObjLabel() == null) ? "fix me" : newBDef.getbObjLabel();
    root.setAttribute("name", ("MethodMap - " + bdeflabel));
    document.appendChild(root);

    for (int i=0; i<methods.length; i++)
    {
      Element method = document.createElementNS(METHOD_MAP.uri, "fmm:Method");
      String mname = methods[i].methodName;
      String mlabel = (methods[i].methodLabel == null) ? "fix me" : methods[i].methodLabel;
      method.setAttribute("operationName", mname.trim());
      method.setAttribute("label", mlabel.trim());
      root.appendChild(method);

      // Append Method Parm elements
      MethodParm[] parms = methods[i].methodProperties.methodParms;
      for (int j=0; j<parms.length; j++)
      {
        Element parm = null;
        if (parms[j].parmType.equalsIgnoreCase(MethodParm.USER_INPUT))
        {
          parm = document.createElementNS(METHOD_MAP.uri, "fmm:UserInputParm");
        }
        else
        {
          //FIXIT!  throw error on invalid parm type.
        }
        String name = (parms[j].parmName == null) ? "" : parms[j].parmName;
        parm.setAttribute("parmName", name);
        String passby = (parms[j].parmPassBy == null) ? "" : parms[j].parmPassBy;
        parm.setAttribute("passBy", passby);
        String req = (parms[j].parmRequired == null) ? "" : parms[j].parmRequired;
        parm.setAttribute("required", req);
        String def = (parms[j].parmDefaultValue == null) ? "" : parms[j].parmDefaultValue;
        parm.setAttribute("defaultValue", def);
        String label = (parms[j].parmLabel == null) ? "" : parms[j].parmLabel;
        parm.setAttribute("label", label);

        if (parms[j].parmDomainValues.length > 0)
        {
          Element parmDomain = document.createElementNS(METHOD_MAP.uri, "fmm:ValidParmValues");
          for (int k=0; k<parms[j].parmDomainValues.length; k++)
          {
            Element parmDomainVal = document.createElementNS(METHOD_MAP.uri, "fmm:ValidParm");
            String value = (parms[j].parmDomainValues[k] == null)
              ? "" : parms[j].parmDomainValues[k];
            parmDomainVal.setAttribute("value", value);
            parmDomain.appendChild(parmDomainVal);
          }
          parm.appendChild(parmDomain);
        }
        method.appendChild(parm);
      }
    }
  }

  private void genMethodMap(BMechTemplate newBMech)
  {
    Method[] methods = newBMech.getMethods();
    Element root = (Element)document.createElementNS(METHOD_MAP.uri, "fmm:MethodMap");
    String bmlabel = (newBMech.getbObjLabel() == null) ? "fix me" : newBMech.getbObjLabel();
    root.setAttribute("name", ("MethodMap - " + bmlabel));
    root.setAttribute("bDefPID", newBMech.getbDefContractPID());
    document.appendChild(root);

    for (int i=0; i<methods.length; i++)
    {
      Element method = document.createElementNS(METHOD_MAP.uri, "fmm:Method");
      String mname = methods[i].methodName;
      String mlabel = (methods[i].methodLabel == null) ? "fix me" : methods[i].methodLabel;
      method.setAttribute("operationName", mname.trim());
      method.setAttribute("operationLabel", mlabel.trim());
      method.setAttribute("wsdlMsgName", (mname.trim() + "Request"));
      method.setAttribute("wsdlMsgOutput", "dissemResponse");
      root.appendChild(method);

      // Append Method Parm elements
      MethodParm[] parms = methods[i].methodProperties.methodParms;
      for (int j=0; j<parms.length; j++)
      {
        Element parm = null;
        if (parms[j].parmType.equalsIgnoreCase(MethodParm.DATASTREAM_INPUT))
        {
          parm = document.createElementNS(METHOD_MAP.uri, "fmm:DatastreamInputParm");
        }
        else if (parms[j].parmType.equalsIgnoreCase(MethodParm.USER_INPUT))
        {
          parm = document.createElementNS(METHOD_MAP.uri, "fmm:UserInputParm");
        }
        else if (parms[j].parmType.equalsIgnoreCase(MethodParm.DEFAULT_INPUT))
        {
          parm = document.createElementNS(METHOD_MAP.uri, "fmm:DefaultInputParm");
        }
        else
        {
          //FIXIT!  throw error on invalid parm type.
        }

        String name = (parms[j].parmName == null) ? "" : parms[j].parmName;
        parm.setAttribute("parmName", name);
        String passby = (parms[j].parmPassBy == null) ? "" : parms[j].parmPassBy;
        parm.setAttribute("passBy", passby);
        String req = (parms[j].parmRequired == null) ? "" : parms[j].parmRequired;
        parm.setAttribute("required", req);
        String def = (parms[j].parmDefaultValue == null) ? "" : parms[j].parmDefaultValue;
        parm.setAttribute("defaultValue", def);
        String label = (parms[j].parmLabel == null) ? "" : parms[j].parmLabel;
        parm.setAttribute("label", label);

        if (parms[j].parmDomainValues.length > 0)
        {
          Element parmDomain = document.createElementNS(METHOD_MAP.uri, "fmm:ValidParmValues");
          for (int k=0; k<parms[j].parmDomainValues.length; k++)
          {
            Element parmDomainVal = document.createElementNS(METHOD_MAP.uri, "fmm:ValidParm");
            String value = (parms[j].parmDomainValues[k] == null)
              ? "" : parms[j].parmDomainValues[k];
            parmDomainVal.setAttribute("value", value);
            parmDomain.appendChild(parmDomainVal);
          }
          parm.appendChild(parmDomain);
        }
        method.appendChild(parm);
      }
      // Append Method Return Type element
      String[] mimeTypes = methods[i].methodProperties.returnMIMETypes;
      StringBuffer sb = new StringBuffer();
      for (int k=0; k<mimeTypes.length; k++)
      {
        sb.append(mimeTypes[k].toString() + " ");
      }
      Element methodReturn = document.createElementNS(METHOD_MAP.uri, "fmm:MethodReturnType");
      methodReturn.setAttribute("wsdlMsgName", "dissemResponse");
      methodReturn.setAttribute("wsdlMsgTOMIME", sb.toString().trim());
      method.appendChild(methodReturn);
    }
  }

  public Element getRootElement()
  {
    return document.getDocumentElement();
  }

  public Document getDocument()
  {
    return document;
  }

  public void printMethodMap()
  {
    try
    {
      String str = new XMLWriter(new DOMResult(document)).getXMLAsString();
      System.out.println(str);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
}