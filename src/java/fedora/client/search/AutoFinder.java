package fedora.client.search;

import java.io.*;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.xml.rpc.ServiceException;

import org.apache.axis.types.NonNegativeInteger;

import fedora.client.APIAStubFactory;
import fedora.client.Downloader;
import fedora.server.access.FedoraAPIA;
import fedora.server.types.gen.FieldSearchQuery;
import fedora.server.types.gen.FieldSearchResult;
import fedora.server.types.gen.ListSession;
import fedora.server.types.gen.ObjectFields;

/**
 * <p><b>Title:</b> AutoFinder.java</p>
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
 * @author cwilper@cs.cornell.edu
 * @version $Id$
 */
public class AutoFinder {

    private FedoraAPIA m_apia;

    public static SimpleDateFormat DATE_FORMATTER=
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    public AutoFinder(String host, int port, String user, String pass)
            throws MalformedURLException, ServiceException {
        m_apia=APIAStubFactory.getStub(host, port, user, pass);
    }

    public FieldSearchResult findObjects(String[] resultFields,
            int maxResults, FieldSearchQuery query)
            throws RemoteException {
        return findObjects(m_apia, resultFields, maxResults, query);
    }

    public FieldSearchResult resumeFindObjects(String sessionToken)
            throws RemoteException {
        return resumeFindObjects(m_apia, sessionToken);
    }

    public static FieldSearchResult findObjects(FedoraAPIA skeleton,
            String[] resultFields, int maxResults, FieldSearchQuery query)
            throws RemoteException {
        return skeleton.findObjects(resultFields,
                new NonNegativeInteger("" + maxResults), query);
    }

    public static FieldSearchResult resumeFindObjects(FedoraAPIA skeleton,
            String sessionToken)
            throws RemoteException {
        return skeleton.resumeFindObjects(sessionToken);
    }

    // fieldQuery is the syntax used by API-A-Lite,
    // such as "fType=O pid~demo*".  Leave blank to match all.
    public static String[] getPIDs(String host, int port, String fieldQuery)
            throws Exception {
        String firstPart="http://" + host + ":" + port + "/fedora/search?xml=true";
        Downloader dLoader=new Downloader(host, port, "na", "na");
        String url=firstPart + "&pid=true&query=" + fieldQuery;
        InputStream in=dLoader.get(url);
        String token="";
        SearchResultParser resultParser;
        HashSet pids=new HashSet();
        while (token!=null) {
            resultParser=new SearchResultParser(in);
            if (resultParser.getToken()!=null) {
                // resumeFindObjects
                token=resultParser.getToken();
                in=dLoader.get(firstPart + "&sessionToken=" + token);
            } else {
                token=null;
            }
            pids.addAll(resultParser.getPIDs());
        }
        String[] result=new String[pids.size()];
        int i=0;
        Iterator iter=pids.iterator();
        while (iter.hasNext()) {
            result[i++]=(String) iter.next();
        }
        return result;
    }

    public static void showUsage(String message) {
        System.err.println(message);
        System.err.println("Usage: fedora-find host port fields phrase");
        System.err.println("");
        System.err.println("    hostname - The Fedora server host or ip address.");
        System.err.println("        port - The Fedora server port.");
        System.err.println("      fields - Space-delimited list of fields.");
        System.err.println("      phrase - Phrase to search for in any field (with ? and * wildcards)");
    }

    public static void printValue(String name, String value) {
        if (value!=null) System.out.println("   " + name + "  " + value);
    }

    public static void printValue(String name, String[] value) {
        if (value!=null) {
            for (int i=0; i<value.length; i++) {
                AutoFinder.printValue(name, value[i]);
            }
        }
    }

    public static void printValue(String name, Calendar value) {
        if (value!=null) {
            AutoFinder.printValue(name, AutoFinder.DATE_FORMATTER.format(value.getTime()));
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length==3) {
            // just list all pids
            System.out.println("Doing query...");
            String[] pids=AutoFinder.getPIDs(args[0], Integer.parseInt(args[1]), args[2]);
            System.out.println("All PIDs in " + args[0] + ":" + Integer.parseInt(args[1]) + " with field query " + args[2]);
            for (int i=0; i<pids.length; i++) {
                System.out.println(pids[i]);
            }
            System.out.println(pids.length + " total.");
            System.exit(0);
        }
        if (args.length!=4) {
            AutoFinder.showUsage("Four arguments required.");
        }
        try {
            AutoFinder finder=new AutoFinder(args[0], Integer.parseInt(args[1]),
                    null, null);
            FieldSearchQuery query=new FieldSearchQuery();
            query.setTerms(args[3]);
            FieldSearchResult result=finder.findObjects(args[2].split(" "),
                    20, query);
            int matchNum=0;
            while (result!=null) {
                for (int i=0; i<result.getResultList().length; i++) {
                    ObjectFields o=result.getResultList()[i];
                    matchNum++;
                    System.out.println("#" + matchNum);
                    AutoFinder.printValue("pid        ", o.getPid());
                    AutoFinder.printValue("fType      ", o.getFType());
                    AutoFinder.printValue("cModel     ", o.getCModel());
                    AutoFinder.printValue("state      ", o.getState());
                    AutoFinder.printValue("ownerId     ", o.getOwnerId());
                    AutoFinder.printValue("cDate      ", o.getCDate());
                    AutoFinder.printValue("mDate      ", o.getMDate());
                    AutoFinder.printValue("dcmDate    ", o.getDcmDate());
                    AutoFinder.printValue("bDef       ", o.getBDef());
                    AutoFinder.printValue("bMech      ", o.getBMech());
                    AutoFinder.printValue("title      ", o.getTitle());
                    AutoFinder.printValue("creator    ", o.getCreator());
                    AutoFinder.printValue("subject    ", o.getSubject());
                    AutoFinder.printValue("description", o.getDescription());
                    AutoFinder.printValue("publisher  ", o.getPublisher());
                    AutoFinder.printValue("contributor", o.getContributor());
                    AutoFinder.printValue("date       ", o.getDate());
                    AutoFinder.printValue("type       ", o.getType());
                    AutoFinder.printValue("format     ", o.getFormat());
                    AutoFinder.printValue("identifier ", o.getIdentifier());
                    AutoFinder.printValue("source     ", o.getSource());
                    AutoFinder.printValue("language   ", o.getLanguage());
                    AutoFinder.printValue("relation   ", o.getRelation());
                    AutoFinder.printValue("coverage   ", o.getCoverage());
                    AutoFinder.printValue("rights     ", o.getRights());
                    System.out.println("");
                }
                ListSession sess=result.getListSession();
                if (sess!=null) {
                    result=finder.resumeFindObjects(sess.getToken());
                } else {
                    result=null;
                }
            }
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getClass().getName()
                    + ((e.getMessage()==null) ? "" : ": " + e.getMessage()));
        }
    }

}