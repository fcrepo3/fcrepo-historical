package fedora.server.access;

import java.util.Date;
import fedora.server.storage.types.MethodDef;
import fedora.server.storage.types.ObjectMethodsDef;
/**
 * <p><b>Title: </b>ObjectProfile.java</p>
 * <p><b>Description: </b>Data structure to contain a profile of
 * a digital object that includes both stored information about the object
 * and dynamic information about the object. </p>
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
 * @author payette@cs.cornell.edu
 * @version 1.0
 */
public class ObjectProfile
{
  public String PID = null;
  public String objectLabel = null;
  public String objectType = null;
  public String objectContentModel = null;
  public Date objectCreateDate = null;
  public Date objectLastModDate = null;
  public String dissIndexViewURL = null;
  public String itemIndexViewURL = null;
}