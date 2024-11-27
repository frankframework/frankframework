package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunResult;
import org.frankframework.stream.Message;
import org.frankframework.util.EnumUtils;
import org.frankframework.util.StringUtil;

public class PgpPipeTest {

	private PipeLineSession session;
	protected PGPPipe encryptPipe;
	protected PGPPipe decryptPipe;
	private final String MESSAGE = "My Secret!!";
	private final String PGP_FOLDER = "PGP/";

	private static final String[] sender = {"test@ibissource.org", "ibistest", "first/private.asc", "first/public.asc", "first/public.asc;second/public.asc"};
	private static final String[] recipient = {"second@ibissource.org", "secondtest", "second/private.asc", "second/public.asc", "first/public.asc;second/public.asc"};

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
				{"Sign wrong params", "org.frankframework.configuration.ConfigurationException",
						new String[]{"sign", null, null, recipient[3], null, recipient[0]},
						new String[]{"decrypt", recipient[2], recipient[1], null, null, null}},
				{"Wrong password", "org.bouncycastle.openpgp.PGPException",
						new String[]{"encrypt", null, null, recipient[3], null, recipient[0]},
						new String[]{"decrypt", recipient[2], "wrong password :/", null, null, null}},
				{"Decrypt Plaintext", "org.bouncycastle.openpgp.PGPException",
						new String[]{"decrypt", recipient[2], recipient[1], null, null, null},
						new String[]{"decrypt", recipient[2], recipient[1], null, null, null}},
		});
	}

	public void initPgpPipeTest(String name, String expectation, String[] encryptParams, String[] decryptParams) {
	}

	@MethodSource("data")
	@ParameterizedTest(name = "{index} - {0} - {1}")
	void testAllMessages(String name, String expectation, String[] encryptParams, String[] decryptParams) throws Throwable {
		initPgpPipeTest(name, expectation, encryptParams, decryptParams);
		try {
			// Configure pipes
			configurePipe(encryptPipe, encryptParams);
			configurePipe(decryptPipe, decryptParams);

			// Encryption phase
			Message encryptMessage = new Message(MESSAGE);
			PipeRunResult encryptionResult = encryptPipe.doPipe(encryptMessage, session);

			// Make sure it's PGP message
			String mid = encryptionResult.getResult().asString();
			assertMessage(mid, MESSAGE);

			// Decryption phase
			Message decryptMessage = encryptionResult.getResult();
			PipeRunResult decryptionResult = decryptPipe.doPipe(decryptMessage, session);
			byte[] result = decryptionResult.getResult().asByteArray();

			// Assert decrypted message equals to the original message
			assertNotNull(result);
			assertEquals(MESSAGE, new String(result));
			assertEquals("success", expectation);
		} catch (Exception e) {
			if (checkExceptionClass(e, expectation)) {
				assertTrue(true);
			} else {
				throw e;
			}
		}
	}

	/**
	 * Creates pipes and pipeline session base for testing.
	 */
	@BeforeEach
	public void setup() throws ConfigurationException {
		session = new PipeLineSession();

		encryptPipe = new PGPPipe();
		decryptPipe = new PGPPipe();

		encryptPipe.addForward(new PipeForward("success", null));
		encryptPipe.setName(encryptPipe.getClass().getSimpleName() + " under test");

		decryptPipe.addForward(new PipeForward("success", null));
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
		pipe.setAction(EnumUtils.parse(PGPPipe.Action.class, params[i++]));
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
		return StringUtil.splitToStream(param, ";")
				.map(key -> PGP_FOLDER + key)
				.collect(Collectors.joining(";"));
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
			if ("success".equalsIgnoreCase(c))
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
	private boolean checkExceptionClass(Throwable t, Class<?> c) {
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
		assertTrue(message.startsWith("-----BEGIN PGP MESSAGE-----"), "Message does not comply with PGP message beginning.");
		assertFalse(message.contains(secretMessage), "Encrypted version contains the secret message.");
	}
}
