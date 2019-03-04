package nl.nn.adapterframework.filesystemsenders;

import nl.nn.adapterframework.senders.IFileSystemSender;
import nl.nn.adapterframework.senders.SambaSenderOld;

public class SambaSenderTest extends SambaFileSystemSenderTest {

	@Override
	public IFileSystemSender createFileSystemSender() {
		SambaSenderOld sambaSender = new SambaSenderOld();
		sambaSender.setShare(share);
		sambaSender.setUsername(username);
		sambaSender.setPassword(password);
		return sambaSender;
	}

}
