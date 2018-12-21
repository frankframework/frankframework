package nl.nn.adapterframework.filesystem;

public interface IFileSystem<F> extends IFileSystemBase<F> {
	
	public boolean isFolder(F f);
	public void createFolder(F f);
	public void removeFolder(F f);
	
}
