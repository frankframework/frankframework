package nl.nn.adapterframework.pipes;

import java.util.Collection;
import java.util.StringTokenizer;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import edu.emory.mathcs.backport.java.util.Arrays;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.stream.Message;

@RunWith(Parameterized.class)
public class PgpPipeTest {

	private IPipeLineSession session;

	protected PGPPipe encryptPipe;
	protected PGPPipe decryptPipe;

	private String expectation;
	private String[] encryptParams, decryptParams;

	private final String MESSAGE = "My Secret!!";
	private final String PGP_FOLDER = "PGP/";

	private static final String[] sender = {"test@ibissource.org", "ibistest", "first/private.asc", "first/public.asc", "first/public.asc;second/public.asc"};
	private static final String[] recipient = {"second@ibissource.org", "secondtest", "second/private.asc", "second/public.asc", "first/public.asc;second/public.asc"};

	@Parameterized.Parameters(name = "{index} - {0} - {1}")
	public static Collection<Object[]> data() {
		// List of the parameters for pipes is as follows:
		// action, secretkey, password, publickey, senders, recipients
		return Arrays.asList(new Object[][]{
				{"Sign then Verify", "success",
						new String[]{"sign", sender[2], sender[1], sender[4], sender[0], recipient[0]},
						new String[]{"verify", recipient[2], recipient[1], recipient[4], sender[0], recipient[0]}},
				{"Encrypt then Decrypt", "success",
						new String[]{"encrypt", null, null, recipient[3], null, recipient[0]},
						new String[]{"dEcryPt", recipient[2], recipient[1], null, null, null}},
				{"Sign then Decrypt", "success",
						new String[]{"sign", sender[2], sender[1], sender[4], sender[0], recipient[0]},
						new String[]{"decrypt", recipient[2], recipient[1], null, null, null}},
				{"Verify Someone Signed", "success",
						new String[]{"sign", sender[2], sender[1], sender[4], sender[0], recipient[0]},
						new String[]{"verify", recipient[2], recipient[1], recipient[4], null, recipient[0]}},
				{"Encrypt then Verify", "org.bouncycastle.openpgp.PGPException",
						new String[]{"encrypt", null, null, recipient[3], null, recipient[0]},
						new String[]{"verify", recipient[2], recipient[1], recipient[4], sender[0], recipient[0]}},
				{"Sign wrong params", "nl.nn.adapterframework.configuration.ConfigurationException",
						new String[]{"sign", null, null, recipient[3], null, recipient[0]},
						new String[]{"decrypt", recipient[2], recipient[1], null, null, null}},
				{"Null action", "nl.nn.adapterframework.configuration.ConfigurationException",
						new String[]{null, null, null, recipient[3], null, recipient[0]},
						new String[]{"decrypt", recipient[2], recipient[1], null, null, null}},
				{"Non-existing action", "nl.nn.adapterframework.configuration.ConfigurationException",
						new String[]{"non-existing action", null, null, recipient[3], null, recipient[0]},
						new String[]{"decrypt", recipient[2], recipient[1], null, null, null}},
				{"Wrong password", "org.bouncycastle.openpgp.PGPException",
						new String[]{"encrypt", null, null, recipient[3], null, recipient[0]},
						new String[]{"decrypt", recipient[2], "wrong password :/", null, null, null}},
				{"Decrypt Plaintext", "org.bouncycastle.openpgp.PGPException",
						new String[]{"decrypt", recipient[2], recipient[1], null, null, null},
						new String[]{"decrypt", recipient[2], recipient[1], null, null, null}},
		});
	}

	public PgpPipeTest(String name, String expectation, String[] encryptParams, String[] decryptParams) {
		this.expectation = expectation;
		this.encryptParams = encryptParams;
		this.decryptParams = decryptParams;
	}

	@Test
	public void dotest() throws Throwable {
		try {
			// Configure pipes
			configurePipe(encryptPipe, encryptParams);
			configurePipe(decryptPipe, decryptParams);

			// Encryption phase
			Message encryptMessage = new Message(MESSAGE);
			PipeRunResult encryptionResult = encryptPipe.doPipe(encryptMessage, session);

			// Make sure it's PGP message
			String mid = new String((byte[]) encryptionResult.getResult().asObject());
			assertMessage(mid, MESSAGE);

			// Decryption phase
			Message decryptMessage = Message.asMessage(encryptionResult.getResult());
			PipeRunResult decryptionResult = decryptPipe.doPipe(decryptMessage, session);
			byte[] result = (byte[]) decryptionResult.getResult().asObject();

			// Assert decrypted message equals to the original message
			Assert.assertEquals(MESSAGE, new String(result));
			Assert.assertEquals("success", expectation);
		} catch (Exception e) {
			if (checkExceptionClass(e, expectation)) {
				Assert.assertTrue(true);
			} else {
				throw e;
			}
		}
	}

	/**
	 * Creates pipes and pipeline session base for testing.
	 */
	@Before
	public void setup() {
		session = new PipeLineSessionBase();

		encryptPipe = new PGPPipe();
		decryptPipe = new PGPPipe();

		encryptPipe.registerForward(new PipeForward("success", null));
		encryptPipe.setName(encryptPipe.getClass().getSimpleName() + " under test");

		decryptPipe.registerForward(new PipeForward("success", null));
		decryptPipe.setName(decryptPipe.getClass().getSimpleName() + " under test");
	}

	/**
	 * Sets the parameters of the pipes
	 * @param pipe Pipe to be configured.
	 * @param params Parameters to be set.
	 * @throws ConfigurationException When there's an exception during configuration.
	 */
	private void configurePipe(PGPPipe pipe, String[] params) throws ConfigurationException {
		// Just so we dont have to change numbers every time we change order.
		int i = 0;
		pipe.setAction(params[i++]);
		pipe.setSecretKey(addFolderPath(params[i++]));
		pipe.setSecretPassword(params[i++]);
		pipe.setPublicKeys(addFolderPath(params[i++]));
		pipe.setVerificationAddresses(params[i++]);
		pipe.setRecipients(params[i]);
		pipe.configure();
	}

	/**
	 * Adds folder's path to every file in the given parameters.
	 * @param param A list of files separated by semicolon.
	 * @return A list of files separated by semicolon including the parent folder's path.
	 */
	private String addFolderPath(String param) {
		if (param == null)
			return null;
		StringTokenizer keys = new StringTokenizer(param, ";");
		StringBuilder stringBuilder = new StringBuilder();
		while (keys.hasMoreTokens()) {
			String key = keys.nextToken();
			stringBuilder
					.append(PGP_FOLDER)
					.append(key)
					.append(keys.hasMoreElements() ? ";" : "");
		}

		return stringBuilder.toString();
	}

	/**
	 * Recursively check if the exception thrown is equal to the exception expected.
	 * @param t Throwable to be checked.
	 * @param c Class to be checked
	 * @return True if one of the causes of the exception is the given class, false otherwise.
	 * @throws Throwable Input t when class is not found.
	 */
	private boolean checkExceptionClass(Throwable t, String c) throws Throwable {
		try {
			return checkExceptionClass(t, Class.forName(c));
		} catch (ClassNotFoundException e) {
			if (c.equalsIgnoreCase("success"))
				return false;
			throw t;
		}
	}

	/**
	 * Recursively check if the exception thrown is equal to the exception expected.
	 * @param t Throwable to be checked.
	 * @param c Class to be checked
	 * @return True if one of the causes of the exception is the given class, false otherwise.
	 */
	private boolean checkExceptionClass(Throwable t, Class c) {
		if (c.isInstance(t)) {
			return true;
		} else if (t.getCause() != null) {
			return checkExceptionClass(t.getCause(), c);
		}
		return false;
	}

	/**
	 * Asserts that the message is a PGP message, and that it does not contain the secret message.
	 * @param message Encrypted message
	 * @param secretMessage Plaintext of the same message.
	 */
	private void assertMessage(String message, String secretMessage) {
		Assert.assertTrue("Message does not comply with PGP message beginning.", message.startsWith("-----BEGIN PGP MESSAGE-----"));
		Assert.assertFalse("Encrypted version contains the secret message.", message.contains(secretMessage));
	}
}
