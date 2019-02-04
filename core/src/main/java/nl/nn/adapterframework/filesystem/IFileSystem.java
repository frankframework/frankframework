package nl.nn.adapterframework.filesystem;

public interface IFileSystem<F> extends IFileSystemBase<F> {

	public boolean isFolder(F f) throws FileSystemException;

	public void createFolder(F f) throws FileSystemException;

	public void removeFolder(F f) throws FileSystemException;

}
