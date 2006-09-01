package fedora.server.journal.readerwriter.singlefile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Map;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import fedora.server.errors.ModuleInitializationException;
import fedora.server.journal.JournalException;
import fedora.server.journal.JournalReader;
import fedora.server.journal.ServerInterface;
import fedora.server.journal.entry.ConsumerJournalEntry;
import fedora.server.journal.recoverylog.JournalRecoveryLog;

/**
 * 
 * <p>
 * <b>Title:</b> SingleFileJournalReader.java
 * </p>
 * <p>
 * <b>Description:</b> A rudimentary implementation of JournalReader that just
 * reads all entries from a single Journal file. Useful only for System tests.
 * </p>
 * 
 * @author jblake@cs.cornell.edu
 * @version $Id$
 */

public class SingleFileJournalReader extends JournalReader implements
        SingleFileJournalConstants {
    private final File journalFile;

    private final XMLEventReader reader;

    private boolean open = true;

    private boolean advancedPastHeader = false;

    /**
     * Get the name of the journal file from the server parameters, wrap it in
     * an XMLEventReader, and advance past the document header to the first
     * JournalEntry.
     * 
     * @throws JournalException
     */
    public SingleFileJournalReader(Map parameters, String role,
            JournalRecoveryLog recoveryLog, ServerInterface server)
            throws ModuleInitializationException, JournalException {
        super(parameters, role, recoveryLog, server);
        recoveryLog.log("Using a SingleFileJournalReader");

        if (!parameters.containsKey(PARAMETER_JOURNAL_FILENAME)) {
            throw new ModuleInitializationException("Parameter '"
                    + PARAMETER_JOURNAL_FILENAME + "' not set.", role);
        }

        String filename = (String) parameters.get(PARAMETER_JOURNAL_FILENAME);
        this.journalFile = new File(filename);

        if (!this.journalFile.exists()) {
            throw new ModuleInitializationException("Journal file '"
                    + this.journalFile.getPath() + "' does not exist.", role);
        }
        if (!this.journalFile.isFile()) {
            throw new ModuleInitializationException("Journal file '"
                    + this.journalFile.getPath() + "' is not a file.", role);
        }
        if (!this.journalFile.canRead()) {
            throw new ModuleInitializationException("Journal file '"
                    + this.journalFile.getPath() + "' is not readable.", role);
        }

        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            this.reader = factory.createXMLEventReader(new FileReader(
                    this.journalFile));

        } catch (FileNotFoundException e) {
            throw new ModuleInitializationException("Problem dumping file",
                    role);
        } catch (XMLStreamException e) {
            throw new ModuleInitializationException(
                    "Error opening XML Event reader on Journal file '"
                            + this.journalFile.getPath() + "'", role, e);
        }

    }

    /**
     * Advance past the document header to the first JournalEntry.
     */
    private void advanceIntoFile() throws XMLStreamException, JournalException {
        XMLEvent event = reader.nextEvent();
        if (!event.isStartDocument()) {
            throw new JournalException(
                    "Expecting XML document header, but event was '" + event
                            + "'");
        }

        event = reader.nextTag();
        if (!isStartTagEvent(event, QNAME_TAG_JOURNAL)) {
            throw new JournalException(
                    "Expecting FedoraJournal start tag, but event was '"
                            + event + "'");
        }

        String hash = getOptionalAttributeValue(event.asStartElement(),
                QNAME_ATTR_REPOSITORY_HASH);
        checkRepositoryHash(hash);
    }

    /**
     * Advance past any white space, and then see whether we have any more
     * JournalEntry tags. If we don't, just return null.
     */
    public synchronized ConsumerJournalEntry readJournalEntry()
            throws JournalException, XMLStreamException {
        if (!open) {
            return null;
        }

        if (!advancedPastHeader) {
            advanceIntoFile();
            advancedPastHeader = true;
        }

        XMLEvent next = this.reader.peek();

        // advance past any whitespace events.
        while (next.isCharacters() && next.asCharacters().isWhiteSpace()) {
            this.reader.nextEvent();
            next = this.reader.peek();
        }
        if (isStartTagEvent(next, QNAME_TAG_JOURNAL_ENTRY)) {
            String identifier = peekAtJournalEntryIdentifier();
            ConsumerJournalEntry journalEntry = super
                    .readJournalEntry(this.reader);
            journalEntry.setIdentifier(identifier);
            return journalEntry;
        } else if (isEndTagEvent(next, QNAME_TAG_JOURNAL)) {
            return null;
        } else {
            throw getNotNextMemberOrEndOfGroupException(QNAME_TAG_JOURNAL,
                    QNAME_TAG_JOURNAL_ENTRY, next);
        }
    }

    /**
     * Create an identifier string for the Journal Entry, so we can easily
     * connect the entries in the Recovery Log with those in the Journal.
     * 
     * Call this before calling
     * {@link JournalReader#readJournalEntry(XMLEventReader)}, because the
     * reader is positioned at the beginning of the JournalEntry, so a peek()
     * will give us the start tag, with the info we need.
     */
    private String peekAtJournalEntryIdentifier() throws XMLStreamException {
        XMLEvent event = this.reader.peek();

        String timeString = "unknown";
        if (event.isStartElement()) {
            StartElement start = event.asStartElement();
            Attribute timeStamp = start
                    .getAttributeByName(QNAME_ATTR_TIMESTAMP);
            if (timeStamp != null) {
                timeString = timeStamp.getValue();
            }
        }

        return "entry='" + timeString + "'";
    }

    /**
     * On the first call, Just close the reader.
     */
    public synchronized void shutdown() throws JournalException {
        try {
            if (open) {
                reader.close();
                open = false;
            }
        } catch (XMLStreamException e) {
            throw new JournalException(e);
        }
    }

    public String toString() {
        return super.toString() + ", journalFile='" + journalFile.getPath()
                + "'";
    }

}
