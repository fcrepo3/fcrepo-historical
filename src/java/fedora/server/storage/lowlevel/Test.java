package fedora.server.storage.lowlevel;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 *
 * <p><b>Title:</b> Test.java</p>
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
 * <p>The entire file consists of original code.  Copyright &copy; 2002, 2003 by The
 * Rector and Visitors of the University of Virginia and Cornell University.
 * All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 *
 * @author wdn5e@virginia.edu
 * @version $Id$
 */
class Test {
	private static final ILowlevelStorage lowlevelStorage = FileSystemLowlevelStorage.getPermanentStore();
	private Test() {
	}

	private static final int bufferLength = 512;
	private static final void stream2streamCopy (InputStream in, OutputStream out) throws IOException {
		byte[] buffer= new byte[bufferLength];
		int bytesRead = 0;
		while ((bytesRead = in.read(buffer,0,bufferLength)) != -1) {
			out.write(buffer,0,bytesRead);
		}
	}

	public static void main(String[] argv) {
		System.out.println("\nbeginning Test " + (lowlevelStorage == null));
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String line;
		try {
			while ( (line = in.readLine()) != null) {
				String[] parts = line.split("\\s");
				if (parts.length < 1) {
					System.err.println("enter action (pid (path)), separated only by whitespace");
				} else {
				String action = parts[0];
				if (action.equals("audit") || action.equals("rebuild")) {
					if (action.equals("audit")) {
						if (parts.length != 1) {
							System.err.println("enter audit");
						} else {
							try {
								lowlevelStorage.audit();
							} catch (Exception e) {
								System.out.println("error1 " + e.getMessage());
							}
						}
					} else if (action.equals("rebuild")) {
						if (parts.length != 1) {
							System.err.println("enter rebuild");
						} else {
							try {
								lowlevelStorage.rebuild();
							} catch (Exception e) {
								System.out.println("error2 " + e.getMessage());
							}
						}
					}
				} else {
					if (parts.length < 2) {
						System.err.println("enter action (pid (path)), separated only by whitespace");
					} else {
					String pid = parts[1];
					if (action.equals("remove")) {
						if (parts.length != 2) {
							System.err.println("enter remove pid");
						} else {
						try {
							lowlevelStorage.remove(pid);
						} catch (Exception e) {
							System.out.println("error3: " + e.getMessage());
						}
						}
					} else {
						if (parts.length != 3) {
							System.err.println("enter action (pid (path)), separated only by whitespace");
						} else {
						File f = new File(parts[2]);
						System.out.println("f is null:  " + (f == null));
						if (action.equals("retrieve")) {
							FileOutputStream fos = new FileOutputStream(f);
							try {
								InputStream is = lowlevelStorage.retrieve(pid);
								stream2streamCopy (is, fos);
								is.close();
								fos.close();
							} catch (Exception e) {
								System.out.println("error4 " + e.getMessage());
							}
						} else if (action.equals("add") || action.equals("replace")) {
							FileInputStream fis = new FileInputStream(f);
							if (action.equals("add")) {
								try {
									lowlevelStorage.add(pid,fis);
								} catch (Exception e) {
									System.out.println("error5 " + e.getMessage());
								}
							} else if (action.equals("replace")) {
								try {
									lowlevelStorage.replace(pid,fis);
								} catch (Exception e) {
									System.out.println("error6 " + e.getMessage());
								}
							}
						} else {
							System.err.println("action must be add, replace, retrieve, or remove");
						}
						}
					}
					}
					}
				}
			}
		} catch (IOException e0) {
			System.err.println("problem reading System.in");
		}
	}
}
