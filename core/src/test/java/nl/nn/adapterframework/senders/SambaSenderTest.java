package nl.nn.adapterframework.senders;

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
