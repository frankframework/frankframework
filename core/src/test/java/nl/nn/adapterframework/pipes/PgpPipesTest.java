package nl.nn.adapterframework.pipes;

import edu.emory.mathcs.backport.java.util.Arrays;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.*;
import org.junit.Assert;
import org.junit.Before;
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

	private static final String[] firstKey = {"test@ibissource.org", "ibistest", "first/private.asc", "first/public.asc"};
	private static final String[] secondKey = {"second@ibissource.org", "secondtest", "second/private.asc", "second.public.asc"};

	@Parameterized.Parameters(name = "{index} - {0} - {1}")
	public static Collection<Object[]> data() {
		// sender, key, private, recipient, public
		return Arrays.asList(new Object[][]{
				{"Same Key", "success",
						new String[]{firstKey[0], firstKey[1], firstKey[2], firstKey[0], firstKey[3]},
						new String[]{firstKey[0], firstKey[1], firstKey[2], firstKey[0], firstKey[3]}},
				{"Same Values", "success",
						new String[]{firstKey[0], firstKey[1], firstKey[2], secondKey[0], secondKey[3]},
						new String[]{firstKey[0], secondKey[1], secondKey[2], secondKey[0], firstKey[3]}},
				{"No Sign", "success",
						new String[]{null, null, null, firstKey[0], firstKey[3]},
						new String[]{null, secondKey[1], firstKey[2], firstKey[0], null}}
		});
	}

	public PgpPipesTest(String name, String expectation, String[] encryptParams, String[] decryptParams) {
		setup();
		this.expectation = expectation;
		this.encryptParams = encryptParams;
		this.decryptParams = decryptParams;
	}

	@Test
	public void testSameKeyForBoth() throws Exception {
		try {
			configureEncryptPipe(encryptParams);
			configureDecryptPipe(decryptParams);

			PipeRunResult encryptionResult = encryptPipe.doPipe(message, session);
			OutputStream mid = (OutputStream) encryptionResult.getResult();
			System.out.println(mid.toString());

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
		encryptPipe.setPrivateKeyPath(pgpFolder + params[2]);
		encryptPipe.setRecipient(params[3]);
		encryptPipe.setPublicKeyPath(pgpFolder + params[4]);
		encryptPipe.configure();
	}

	private void configureDecryptPipe(String[] params) throws ConfigurationException {
		decryptPipe.setSenders(params[0]);
		decryptPipe.setKeyPassword(params[1]);
		decryptPipe.setPrivateKeyPath(pgpFolder + params[2]);
		decryptPipe.setRecipient(params[3]);
		decryptPipe.setPublicKeyPath(pgpFolder + params[4]);
		decryptPipe.configure();
	}

	private boolean checkExceptionClass(Throwable t, String c) {
		try {
			return checkExceptionClass(t, Class.forName(c));
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return false;
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
}
