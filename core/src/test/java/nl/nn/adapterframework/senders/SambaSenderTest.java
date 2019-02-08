package nl.nn.adapterframework.senders;

import org.junit.Ignore;

@Ignore
public class SambaSenderTest extends SambaFileSystemSenderTest {

	@Override
	public IFileSystemSender createFileSystemSender() {
		String share = ""; // the path of smb network must start with "smb://"
		String username = "";
		String password = "";

		SambaSender sambaSender = new SambaSender();
		sambaSender.setShare(share);
		sambaSender.setUsername(username);
		sambaSender.setPassword(password);

		return sambaSender;
	}

}
