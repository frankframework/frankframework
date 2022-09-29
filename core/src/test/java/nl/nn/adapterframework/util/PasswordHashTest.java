package nl.nn.adapterframework.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PasswordHashTest {

	@Test
	public void testBasicHashAndValidation() throws Exception {
		// Print out 10 hashes
		for(int i = 0; i < 10; i++) {
			assertTrue(PasswordHash.createHash("p\r\nassw0Rd!").contains(":"));
			assertEquals(134, PasswordHash.createHash("p\r\nassw0Rd!").length());
		}

		// Test password validation
		for(int i = 0; i < 100; i++) {
			String password = "" + i;
			String hash = PasswordHash.createHash(password);
			String secondHash = PasswordHash.createHash(password);
			assertNotEquals("TWO HASHES ARE EQUAL", hash, secondHash);

			String wrongPassword = "" + (i + 1);
			assertFalse("WRONG PASSWORD ACCEPTED", PasswordHash.validatePassword(wrongPassword, hash));
			assertTrue("GOOD PASSWORD NOT ACCEPTED", PasswordHash.validatePassword(password, hash));
		}
	}
}
