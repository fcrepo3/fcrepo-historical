package fedora.server.storage.types;

import fedora.server.storage.types.MethodParmDef;

/**
 *
 * <p><b>Title:</b> DisseminationBindingInfo.java</p>
 * <p><b>Description:</b> Data struture for holding information necessary to
 * complete a dissemination request.</p>
 *
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2002, 2003 by The
 * Rector and Visitors of the University of Virginia and Cornell University.
 * All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
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