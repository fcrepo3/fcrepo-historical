package fedora.server.storage.service;

/**
 *
 * <p><b>Title:</b> HTTPOperation.java</p>
 * <p><b>Description:</b> A data structure for holding WSDL HTTP binding
 * information for an operation.</p>
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
public class HTTPOperation extends AbstractOperation
{

  /**
   * operationLocation:  a relative URI for the operation.
   * The URI is ultimately combined with the URI in the http:address element to
   * (see Port object) form the full URI for the HTTP request.
   */
  public String operationLocation;

  /**
   * inputBinding:
   */
  public HTTPOperationInOut inputBinding;


  /**
   * outputBinding:
   */
  public HTTPOperationInOut outputBinding;
}