package nl.nn.adapterframework.filesystem;

import org.apache.commons.lang3.NotImplementedException;

import nl.nn.adapterframework.receivers.DirectoryListener;

public class DirectoryListenerWrapper extends DirectoryListener implements IFileSystemListener {

	@Override
	public void setInputFolder(String inputDirectory) {
		setInputDirectory(inputDirectory);
	}

	@Override
	public void setInProcessFolder(String inProcessFolder) {
		setOutputDirectory(inProcessFolder);
	}

	@Override
	public void setProcessedFolder(String processedFolder) {
		setOutputDirectory(processedFolder);
	}

	@Override
	public void setErrorFolder(String errorFolder) {
		setOutputDirectory(errorFolder);
	}

	@Override
	public void setMessageType(String messageType) {
		throw new NotImplementedException("messageType");
	}

}
