package nl.nn.adapterframework.pipes;

import edu.emory.mathcs.backport.java.util.Arrays;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunResult;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.OutputStream;
import java.util.Collection;

@RunWith(Parameterized.class)
public class PgpPipeTest {

	private IPipeLineSession session;

	protected PGPPipe encryptPipe;
	protected PGPPipe decryptPipe;

	private String expectation;
	private String[] encryptParams, decryptParams;

	private final String MESSAGE = "My Secret!!";

	private static final String PGP_FOLDER = "src/test/resources/PGP/";
	private static final String[] sender = {"test@ibissource.org", "ibistest", "first/private.asc", "first/public.asc", "first/public.asc;second/public.asc"};
	private static final String[] recipient = {"second@ibissource.org", "secondtest", "second/private.asc", "second/public.asc", "first/public.asc;second/public.asc"};

	@Parameterized.Parameters(name = "{index} - {0} - {1}")
	public static Collection<Object[]> data() {
		// action, secretkey, password, publickey, senders, recipients

		return Arrays.asList(new Object[][]{
				{"Encrypt&Sign then Decrypt&Verify", "success",
						new String[]{"sign", sender[2], sender[1], sender[4], sender[0], recipient[0]},
						new String[]{"verify", recipient[2], recipient[1], recipient[4], sender[0], recipient[0]}},
//				{"Same Key", "success",
//						new String[]{sender[0], sender[1], sender[2], sender[0], sender[3]},
//						new String[]{sender[0], sender[1], sender[2], sender[3]}},
		});
	}

	public PgpPipeTest(String name, String expectation, String[] encryptParams, String[] decryptParams) {
		setup();
		this.expectation = expectation;
		this.encryptParams = encryptParams;
		this.decryptParams = decryptParams;
	}

	@Test
	public void dotest() throws Throwable {
		try {
			configurePipe(encryptPipe, encryptParams);
			configurePipe(decryptPipe, decryptParams);

			PipeRunResult encryptionResult = encryptPipe.doPipe(MESSAGE, session);
			OutputStream mid = (OutputStream) encryptionResult.getResult();
			System.out.println(mid.toString());
			assertMessage(mid.toString(), MESSAGE);
			PipeRunResult decryptionResult = decryptPipe.doPipe(encryptionResult.getResult(), session);
			OutputStream result = (OutputStream) decryptionResult.getResult();
			System.out.println(result.toString());

			Assert.assertEquals(MESSAGE, result.toString());
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

		encryptPipe = new PGPPipe();
		decryptPipe = new PGPPipe();

		encryptPipe.registerForward(new PipeForward("success", null));
		encryptPipe.setName(encryptPipe.getClass().getSimpleName() + " under test");

		decryptPipe.registerForward(new PipeForward("success", null));
		decryptPipe.setName(decryptPipe.getClass().getSimpleName() + " under test");
	}

	private void configurePipe(PGPPipe pipe, String[] params) throws ConfigurationException {
		// Just so we dont have to change numbers every time we change order.
		int i = 0;
		pipe.setAction(params[i++]);
		pipe.setSecretKey(addFolderPath(params[i++]));
		pipe.setSecretPassword(params[i++]);
		pipe.setPublicKey(addFolderPath(params[i++]));
		pipe.setSenders(params[i++]);
		pipe.setRecipients(params[i]);
		pipe.configure();
	}

	private String addFolderPath(String param) {
		String[] keys = param.split(";");
		StringBuilder stringBuilder = new StringBuilder(param.length() + PGP_FOLDER.length() * keys.length);
		for(int i = 0; i < keys.length; i++) {
			stringBuilder
					.append(PGP_FOLDER)
					.append(keys[i])
					.append(i != keys.length - 1 ? ";" : "");
		}
		return stringBuilder.toString();
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
