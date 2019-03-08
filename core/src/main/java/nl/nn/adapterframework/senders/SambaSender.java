package nl.nn.adapterframework.senders;

import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.filesystem.Samba2FileSystem;

public class SambaSender extends FileSystemSender<String, Samba2FileSystem> {

	public SambaSender() {
		setFileSystem(new Samba2FileSystem());
	}

	@IbisDoc({ "in case the user account is bound to a domain", "" })
	public void setDomain(String domain) {
		getFileSystem().setDomain(domain);
	}

	@IbisDoc({ "the smb share username", "" })
	public void setUsername(String username) {
		getFileSystem().setUsername(username);
	}

	@IbisDoc({ "the smb share password", "" })
	public void setPassword(String password) {
		getFileSystem().setPassword(password);
	}

	@IbisDoc({ "alias used to obtain credentials for the smb share", "" })
	public void setAuthAlias(String authAlias) {
		getFileSystem().setAuthAlias(authAlias);
	}

	@IbisDoc({ "used when creating folders or overwriting existing files (when renaming or moving)", "false" })
	public void setForce(boolean force) {
		getFileSystem().setForce(force);
	}

	@IbisDoc({ "Shared folder name in the samba server", "" })
	public void setShare(String share) {
		getFileSystem().setShare(share);
	}

}
