package nl.nn.adapterframework.pipes;

import edu.emory.mathcs.backport.java.util.Arrays;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.unmanaged.SpringJmsConnector;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.OutputStream;
import java.util.Collection;

@RunWith(Parameterized.class)
public class PgpPipesTest {

	private IPipeLineSession session;

	protected PGPSignAndEncryptPipe encryptPipe;
	protected PGPDecryptAndVerifyPipe decryptPipe;

	private String expectation;
	private String[] encryptParams, decryptParams;

	String message = "My Secret!!";
	private final String pgpFolder = "src/test/resources/PGP/";

	private static final String[] sender = {"test@ibissource.org", "ibistest", "first/private.asc", "first/public.asc"};
	private static final String[] recipient = {"second@ibissource.org", "secondtest", "second/private.asc", "second/public.asc"};

	@Parameterized.Parameters(name = "{index} - {0} - {1}")
	public static Collection<Object[]> data() {
		// sender, key, private, recipient, public
		// sender, key, private, public
		return Arrays.asList(new Object[][]{
				{"Encrypt&Sign then Decrypt&Verify", "success",
						new String[]{sender[0], sender[1], sender[2], recipient[0], recipient[3]},
						new String[]{sender[0], recipient[1], recipient[2], sender[3]}},
				{"Same Key", "success",
						new String[]{sender[0], sender[1], sender[2], sender[0], sender[3]},
						new String[]{sender[0], sender[1], sender[2], sender[3]}},
				{"Encrypt&Decrypt", "success",
						new String[]{null, null, null, sender[0], sender[3]},
						new String[]{null, sender[1], sender[2], null}},
				{"Nulls", "nl.nn.adapterframework.configuration.ConfigurationException",
						new String[]{null, null, null, null, null},
						new String[]{null, null, null, null}},
				{"Wrong password", "org.bouncycastle.openpgp.PGPException",
						new String[]{null, null, null, sender[0], sender[3]},
						new String[]{null, "wrong key", sender[2], null}},
				{"Wrong key", "org.bouncycastle.openpgp.PGPException",
						new String[]{null, null, null, sender[0], sender[3]},
						new String[]{null, recipient[1], recipient[2], null}},
		});
	}

	public PgpPipesTest(String name, String expectation, String[] encryptParams, String[] decryptParams) {
		setup();
		this.expectation = expectation;
		this.encryptParams = encryptParams;
		this.decryptParams = decryptParams;
	}

	@Test
	public void dotest() throws Throwable {
		try {
			configureEncryptPipe(encryptParams);
			configureDecryptPipe(decryptParams);

			PipeRunResult encryptionResult = encryptPipe.doPipe(message, session);
			OutputStream mid = (OutputStream) encryptionResult.getResult();
			System.out.println(mid.toString());
			assertMessage(mid.toString(), message);
			PipeRunResult decryptionResult = decryptPipe.doPipe(encryptionResult.getResult(), session);
			OutputStream result = (OutputStream) decryptionResult.getResult();
			System.out.println(result.toString());

			Assert.assertEquals(message, result.toString());
			Assert.assertEquals("success", expectation);
		} catch (Exception e) {
			if (checkExceptionClass(e, expectation)) {
				Assert.assertTrue(true);
			} else {
				throw e;
			}
		}
	}

	public void setup() {
		session = new PipeLineSessionBase();

		encryptPipe = new PGPSignAndEncryptPipe();
		decryptPipe = new PGPDecryptAndVerifyPipe();

		encryptPipe.registerForward(new PipeForward("success", null));
		encryptPipe.setName(encryptPipe.getClass().getSimpleName() + " under test");

		decryptPipe.registerForward(new PipeForward("success", null));
		decryptPipe.setName(decryptPipe.getClass().getSimpleName() + " under test");
	}

	private void configureEncryptPipe(String[] params) throws ConfigurationException {
		encryptPipe.setSender(params[0]);
		encryptPipe.setKeyPassword(params[1]);
		encryptPipe.setPrivateKeyPath(params[2] == null ? null : pgpFolder + params[2]);
		encryptPipe.setRecipient(params[3]);
		encryptPipe.setPublicKeyPath(params[4] == null ? null : pgpFolder + params[4]);
		encryptPipe.setPersonalPublic(pgpFolder + sender[3]);
		encryptPipe.configure();
	}

	private void configureDecryptPipe(String[] params) throws ConfigurationException {
		decryptPipe.setSenders(params[0]);
		decryptPipe.setKeyPassword(params[1]);
		decryptPipe.setPrivateKeyPath(params[2] == null ? null : pgpFolder + params[2]);
		decryptPipe.setPublicKeyPath(params[3] == null ? null : pgpFolder + params[3]);
		decryptPipe.configure();
	}

	private boolean checkExceptionClass(Throwable t, String c) throws Throwable {
		try {
			return checkExceptionClass(t, Class.forName(c));
		} catch (ClassNotFoundException e) {
			if (c.equalsIgnoreCase("success"))
				return false;
			throw t;
		}
	}

	private boolean checkExceptionClass(Throwable t, Class c) {
		if (c.isInstance(t)) {
			return true;
		} else if (t.getCause() != null) {
			return checkExceptionClass(t.getCause(), c);
		}
		return false;
	}

	private void assertMessage(String message, String secretMessage) {
		Assert.assertTrue("Message does not comply with PGP message beginning.", message.startsWith("-----BEGIN PGP MESSAGE-----"));
		Assert.assertFalse("Encrypted version contains the secret message.", message.contains(secretMessage));
	}
}
