/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */

package fedora.server.storage.service;

import java.util.Vector;

/**
 *
 * <p><b>Title:</b> Part.java</p>
 * <p><b>Description:</b> A data structure for holding WSDL Message part.</p>
 *
 * @author payette@cs.cornell.edu
 * @version $Id$
 */
public class Part
{
  public String partName;
  public String partTypeName;
  public String partBaseTypeNamespaceURI;
  public String partBaseTypeLocalName;

  // consider...
  public Vector enumerationOfValues;
  public String defaultValue;
}