package fedora.client;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import javax.swing.JTextArea;

/**
 *
 * <p><b>Title:</b> BatchOutputCatcher.java</p>
 * <p><b>Description:</b> </p>
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
 * <p>The entire file consists of original code.  Copyright &copy; 2002-2004 by The
 * Rector and Visitors of the University of Virginia and Cornell University.
 * All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 *
 * @author wdn5e@virginia.edu
 * @version $Id$
 */
public class BatchOutputCatcher extends OutputStream {

	private JTextArea jTextArea = null;

	public void write(int b) {
		byte bv = (new Integer(b)).byteValue();
		jTextArea.append(new String(new byte[] {bv} ));
	}

	public BatchOutputCatcher(JTextArea jTextArea) {
		this.jTextArea = jTextArea;
	}

}
