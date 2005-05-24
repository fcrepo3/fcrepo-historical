/*
 * Created on May 22, 2005
 *
 */
package fedora.utilities;

import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author Edwin Shin
 *
 */
public class StreamReaderThread extends Thread {
    StringBuffer mOut;

    InputStreamReader mIn;

    public StreamReaderThread(InputStream in, StringBuffer out) {
        mOut = out;
        mIn = new InputStreamReader(in);
    }

    public void run() {
        int ch;
        try {
            while (-1 != (ch = mIn.read()))
                mOut.append((char) ch);
        } catch (Exception e) {
            mOut.append("\nRead error:" + e.getMessage());
        }
    }
}
