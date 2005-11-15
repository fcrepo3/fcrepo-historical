package fedora.server.storage;

import java.util.*;

public class DOReaderCache extends Thread {

    private int m_maxReaders;
    private int m_maxCachedSeconds;

    private Map m_readers;
    private List m_pidList;

    private boolean m_stopRequested;

    public DOReaderCache(int maxReaders,
                         int maxCachedSeconds) {

        m_maxReaders = maxReaders;
        m_maxCachedSeconds = maxCachedSeconds;

        m_readers = new HashMap();
        m_pidList = new ArrayList();

        m_stopRequested = false;
        this.start();
    }

    /**
     * Until closed, check for and remove any expired entries every second.
     */
    public void run() {
        while (!m_stopRequested) {
            removeExpired();
            if (!m_stopRequested) {
                try { Thread.sleep(1000); } catch (Exception e) { }
            }
        }
    }

    private void removeExpired() {
        long cutoffTime = System.currentTimeMillis() - (1000 * m_maxCachedSeconds);
        synchronized (m_readers) {
            if (m_pidList.size() > 0) {
                boolean done = false;
                List expiredList = new ArrayList();
                Iterator pids = m_pidList.iterator();
                while (pids.hasNext() && !done) {
                    String pid = (String) pids.next();
                    List l = (List) m_readers.get(pid);
                    long cachedTime = ((Long) l.get(1)).longValue();
                    if (cachedTime < cutoffTime) {
                        expiredList.add(pid);
                    } else {
                        done = true;
                    }
                }
                pids = expiredList.iterator();
                while (pids.hasNext()) {
                    remove((String) pids.next());
                }
            }
        }
    }

    /**
     * Remove a DOReader from the cache.
     *
     * If it doesn't exist in the cache, do nothing.
     */
    public void remove(String pid) {
        synchronized (m_readers) {
            if (m_readers.remove(pid) != null) {
                m_pidList.remove(pid);
            }
        }
    }

    /**
     * Add a DOReader to the cache.
     *
     * If it already exists in the cache, allow it to 
     */
    public void put(DOReader reader) {
        String pid = null;
        try { pid = reader.GetObjectPID(); } catch (Exception e) { }
        Long time = new Long(System.currentTimeMillis());
        List l = new ArrayList();
        l.add(reader);
        l.add(time);
        synchronized (m_readers) {
            m_readers.put(pid, l);
            m_pidList.add(pid);
            if (m_readers.size() > m_maxReaders) {
                Object overflowPid = m_pidList.remove(0);
                m_readers.remove(overflowPid);
            }
        }
    }

    /**
     * Get a DOReader from the cache.
     *
     * If it doesn't exist in the cache, return null.
     */
    public DOReader get(String pid) {
        DOReader reader = null;
        synchronized (m_readers) {
            List l = (List) m_readers.get(pid);
            if (l != null) {
                reader = (DOReader) l.get(0);
            }
        }
        return reader;
    }

    public void close() {
        // make sure the thread finishes
        m_stopRequested = true;
    }

}