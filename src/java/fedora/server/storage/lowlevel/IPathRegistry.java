package fedora.server.storage.lowlevel;
import fedora.server.errors.LowlevelStorageException;
import fedora.server.errors.ObjectNotInLowlevelStorageException;
interface IPathRegistry {
	public String get (String pid) throws LowlevelStorageException, ObjectNotInLowlevelStorageException;
	public void put (String pid, String path) throws LowlevelStorageException;
	public void remove (String pid) throws LowlevelStorageException, ObjectNotInLowlevelStorageException;
	public void rebuild (/*String[] storeBases*/) throws LowlevelStorageException;
	public void auditFiles (/*String[] storeBases*/) throws LowlevelStorageException;
	public void auditRegistry () throws LowlevelStorageException;
}
