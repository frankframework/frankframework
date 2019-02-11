package nl.nn.adapterframework.senders;

import jcifs.smb.SmbFile;
import nl.nn.adapterframework.filesystem.FileSystemSender;
import nl.nn.adapterframework.filesystem.SambaFileSystem;

public class SambaFileSystemSender extends FileSystemSender<SmbFile, SambaFileSystem> {

	public SambaFileSystemSender() {
		setFileSystem(new SambaFileSystem());
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
