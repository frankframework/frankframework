package nl.nn.adapterframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

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
			assertNotEquals(hash, secondHash, "TWO HASHES ARE EQUAL");

			String wrongPassword = "" + (i + 1);
			assertFalse(PasswordHash.validatePassword(wrongPassword, hash), "WRONG PASSWORD ACCEPTED");
			assertTrue(PasswordHash.validatePassword(password, hash), "GOOD PASSWORD NOT ACCEPTED");
		}
	}
}
