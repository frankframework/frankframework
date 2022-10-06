package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.PasswordHash;

/**
 * PasswordHashPipe Tester.
 * 
 * @author <Sina>
 */
public class PasswordHashPipeTest extends PipeTestBase<PasswordHashPipe> {

	@Override
	public PasswordHashPipe createPipe() {
		return new PasswordHashPipe();
	}

	/**
	 * Method: doPipe(Object input, PipeLineSession session)
	 */
	@Test
	public void testHashPipe() throws Exception {
		pipe.configure();
		PipeRunResult res = doPipe(pipe, "password", session);
		assertTrue(PasswordHash.validatePassword("password", res.getResult().asString()));
		int hashLength = res.getResult().asString().length();
		assertEquals("success", res.getPipeForward().getName());
		assertEquals(135, hashLength);
	}

	@Test
	public void testValidatePipe() throws Exception {
		pipe.configure();
		PipeRunResult res = doPipe(pipe, "password", session);
		assertTrue(PasswordHash.validatePassword("password", res.getResult().asString()));
		assertEquals("success", res.getPipeForward().getName());
	}

	@Test
	public void testValidatePipeFailAsNotTheSame() throws Exception {
		String hashed = PasswordHash.createHash("password");
		session.put("key", hashed + "2132"); // this will make test fail as validation of the hash and the paswword will not
												// be the same
		pipe.setHashSessionKey("key");
		pipe.configure();
		pipe.registerForward(new PipeForward("failure", "random/path"));
		PipeRunResult res = doPipe(pipe, "password", session);
		assertEquals("failure", res.getPipeForward().getName());
	}

	@Test
	public void testValidatePassAsTheSame() throws Exception {
		String hashed = PasswordHash.createHash("password");
		session.put("key", hashed); // this will make test fail as validation of the hash and the paswword will not
									// be the same
		pipe.setHashSessionKey("key");
		pipe.configure();
		pipe.registerForward(new PipeForward("failure", "random/path"));
		PipeRunResult res = doPipe(pipe, "password", session);
		assertEquals("success", res.getPipeForward().getName());
	}

	@Test
	public void testTwoHashesNotTheSame() throws Exception {
		pipe.configure();

		PipeRunResult res1 = doPipe(pipe, "a", session);
		PipeRunResult res2 = doPipe(pipe, "a", session);
		assertNotEquals(res1.getResult().asString(), res2.getResult().asString());
	}

}
