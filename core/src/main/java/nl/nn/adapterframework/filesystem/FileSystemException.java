package nl.nn.adapterframework.filesystem;

import nl.nn.adapterframework.core.IbisException;

public class FileSystemException extends IbisException {

	public FileSystemException() {
		super();
	}

	public FileSystemException(String message, Throwable cause) {
		super(message, cause);
	}

	public FileSystemException(String message) {
		super(message);
	}

	public FileSystemException(Throwable cause) {
		super(cause);
	}

}
