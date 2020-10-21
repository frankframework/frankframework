package nl.nn.adapterframework.filesystem;

import nl.nn.adapterframework.configuration.ConfigurationException;

public class MsGraphFileSystemTest extends SelfContainedBasicFileSystemTest<GraphItem, MsGraphFileSystem> {

	@Override
	protected MsGraphFileSystem createFileSystem() throws ConfigurationException {
		return new MsGraphFileSystem() {
			@Override
			public void open() throws FileSystemException {
				//super.open();
				graphClient = new TestBase().graphClient;
			}
		};
	}
	
}
