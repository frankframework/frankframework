package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunResult;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.OutputStream;

public class PgpPipesTest {

	private IPipeLineSession session = new PipeLineSessionBase();

	protected PGPSignAndEncryptPipe encryptPipe;
	protected PGPDecryptAndVerifyPipe decryptPipe;

	private final String pgpFolder = "src/test/resources/PGP/";

	@Before
	public void setup() throws ConfigurationException {
		encryptPipe = new PGPSignAndEncryptPipe();
		decryptPipe = new PGPDecryptAndVerifyPipe();

		encryptPipe.registerForward(new PipeForward("success", null));
		encryptPipe.setName(encryptPipe.getClass().getSimpleName() + " under test");

		decryptPipe.registerForward(new PipeForward("success", null));
		decryptPipe.setName(decryptPipe.getClass().getSimpleName() + " under test");
	}

	@Test
	public void testSameKeyForBoth() throws Exception {
		configureEncryptPipe("test@ibissource.org", "test@ibissource.org", "ibistest", "private.asc", "public.asc");
		configureDecryptPipe("test@ibissource.org", "test@ibissource.org", "ibistest", "private.asc", "public.asc");

		String message = "My Secret!!";

		PipeRunResult encryptionResult = encryptPipe.doPipe(message, session);
		OutputStream mid = (OutputStream) encryptionResult.getResult();
		System.out.println(mid.toString());
		PipeRunResult decryptionResult = decryptPipe.doPipe(encryptionResult.getResult(), session);

		OutputStream result = (OutputStream) decryptionResult.getResult();
		System.out.println(result.toString());
		Assert.assertEquals(message, result.toString());
	}

	private void configureEncryptPipe(String sender, String recipient,
									  String password, String privatePath, String publicPath) throws ConfigurationException {
		encryptPipe.setSender(sender);
		encryptPipe.setRecipient(recipient);
		encryptPipe.setKeyPassword(password);
		encryptPipe.setPrivateKeyPath(pgpFolder + privatePath);
		encryptPipe.setPublicKeyPath(pgpFolder + publicPath);
		encryptPipe.configure();
	}

	private void configureDecryptPipe(String sender, String recipient,
									  String password, String privatePath, String publicPath) throws ConfigurationException {
		decryptPipe.setSenders(sender);
		decryptPipe.setRecipient(recipient);
		decryptPipe.setKeyPassword(password);
		decryptPipe.setPrivateKeyPath(pgpFolder + privatePath);
		decryptPipe.setPublicKeyPath(pgpFolder + publicPath);
		decryptPipe.configure();
	}

}
