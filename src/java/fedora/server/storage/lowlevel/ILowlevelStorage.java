package fedora.server.storage.lowlevel;
import java.io.InputStream;
import fedora.server.errors.LowlevelStorageException;
import fedora.server.errors.ObjectAlreadyInLowlevelStorageException;
import fedora.server.errors.ObjectNotInLowlevelStorageException;

/**
 *
 * <p><b>Title:</b> ILowlevelStorage.java</p>
 * <p><b>Description:</b> </p>
 *
 * @author wdn5e@virginia.edu
 * @version $Id$
 */
public interface ILowlevelStorage {
	public void add(String pid, InputStream content) throws LowlevelStorageException, ObjectAlreadyInLowlevelStorageException;
	public void replace(String pid, InputStream content) throws LowlevelStorageException, ObjectNotInLowlevelStorageException;
	public InputStream retrieve(String pid) throws LowlevelStorageException, ObjectNotInLowlevelStorageException;
	public void remove(String pid) throws LowlevelStorageException, ObjectNotInLowlevelStorageException;
	public void rebuild () throws LowlevelStorageException;
	public void audit () throws LowlevelStorageException;
}
