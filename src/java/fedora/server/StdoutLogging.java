package fedora.server;

/**
 *
 * <p><b>Title:</b> StdoutLogging.java</p>
 * <p><b>Description:</b> A basic logging implementation that goes to stdout.</p>
 *
 * <p>Useful for testing modules standalone.</p>
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
 * @author cwilper@cs.cornell.edu
 * @version $Id$
 */
public abstract class StdoutLogging
        implements Logging {

    public static final int SEVERE=0;
    public static final int WARNING=1;
    public static final int INFO=2;
    public static final int CONFIG=3;
    public static final int FINE=4;
    public static final int FINER=5;
    public static final int FINEST=6;

    private int m_level;
    private Logging m_target;

    public StdoutLogging(Logging target) {
        m_target=target;
        m_level=FINEST;
    }

    public void setLogLevel(int level) {
        m_level=level;
    }

    public void logSevere(String message) {
        if (m_target!=null) {
            m_target.logSevere(message);
        } else {
            if (loggingSevere()) {
                System.out.println("SEVERE: " + message);
            }
        }
    }

    public void logWarning(String message) {
        if (m_target!=null) {
            m_target.logWarning(message);
        } else {
            if (loggingWarning()) {
                System.out.println("WARNING: " + message);
            }
        }
    }

    public void logInfo(String message) {
        if (m_target!=null) {
            m_target.logInfo(message);
        } else {
            if (loggingInfo()) {
                System.out.println("INFO: " + message);
            }
        }
    }

    public void logConfig(String message) {
        if (m_target!=null) {
            m_target.logConfig(message);
        } else {
            if (loggingConfig()) {
                System.out.println("CONFIG: " + message);
            }
        }
    }

    public void logFine(String message) {
        if (m_target!=null) {
            m_target.logFine(message);
        } else {
            if (loggingFine()) {
                System.out.println("FINE: " + message);
            }
        }
    }

    public void logFiner(String message) {
        if (m_target!=null) {
            m_target.logFiner(message);
        } else {
            if (loggingFiner()) {
                System.out.println("FINER: " + message);
            }
        }
    }

    public void logFinest(String message) {
        if (m_target!=null) {
            m_target.logFinest(message);
        } else {
            if (loggingFinest()) {
                System.out.println("FINEST: " + message);
            }
        }
    }

    public boolean loggingSevere() {
        if (m_target!=null) {
            return m_target.loggingSevere();
        }
        return m_level>=SEVERE;
    }

    public boolean loggingWarning() {
        if (m_target!=null) {
            return m_target.loggingWarning();
        }
        return m_level>=WARNING;
    }

    public boolean loggingInfo() {
        if (m_target!=null) {
            return m_target.loggingInfo();
        }
        return m_level>=INFO;
    }

    public boolean loggingConfig() {
        if (m_target!=null) {
            return m_target.loggingConfig();
        }
        return m_level>=CONFIG;
    }

    public boolean loggingFine() {
        if (m_target!=null) {
            return m_target.loggingFine();
        }
        return m_level>=FINE;
    }

    public boolean loggingFiner() {
        if (m_target!=null) {
            return m_target.loggingFiner();
        }
        return m_level>=FINER;
    }

    public boolean loggingFinest() {
        if (m_target!=null) {
            return m_target.loggingFinest();
        }
        return m_level>=FINEST;
    }

}