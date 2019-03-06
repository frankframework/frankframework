package nl.nn.adapterframework.senders;

import nl.nn.adapterframework.filesystem.Samba2FileSystem;

public class SambaSender extends FileSystemSender<String, Samba2FileSystem> {

	public SambaSender() {
		setFileSystem(new Samba2FileSystem());
	}

	public void setDomain(String domain) {
		getFileSystem().setDomain(domain);
	}

	public void setUsername(String username) {
		getFileSystem().setUsername(username);
	}

	public void setPassword(String password) {
		getFileSystem().setPassword(password);
	}

	public void setAuthAlias(String authAlias) {
		getFileSystem().setAuthAlias(authAlias);
	}

	public void setForce(boolean force) {
		getFileSystem().setForce(force);
	}

	public void setShare(String share) {
		getFileSystem().setShare(share);
	}
}
