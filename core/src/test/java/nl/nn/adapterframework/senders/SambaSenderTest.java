package nl.nn.adapterframework.senders;

public class SambaSenderTest extends SambaFileSystemSenderTest {

	@Override
	public IFileSystemSender createFileSystemSender() {
		SambaSender sambaSender = new SambaSender();
		sambaSender.setShare(share);
		sambaSender.setUsername(username);
		sambaSender.setPassword(password);
		return sambaSender;
	}

}
