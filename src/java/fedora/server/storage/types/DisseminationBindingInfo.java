/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */

package fedora.server.storage.types;

import fedora.server.storage.types.MethodParmDef;

/**
 *
 * <p><b>Title:</b> DisseminationBindingInfo.java</p>
 * <p><b>Description:</b> Data struture for holding information necessary to
 * complete a dissemination request.</p>
 *
 * @author rlw@virginia.edu
 * @version $Id$
 */
public class DisseminationBindingInfo
{
  public String DSBindKey = null;
  public MethodParmDef[] methodParms = null;
  public String dsLocation = null;
  public String dsControlGroupType = null;
  public String dsID = null;
  public String dsVersionID = null;
  public String AddressLocation = null;
  public String OperationLocation = null;
  public String ProtocolType = null;
  public String dsState = null;
}