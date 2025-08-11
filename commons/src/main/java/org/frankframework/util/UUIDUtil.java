/*
   Copyright 2023-2025 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.frankframework.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.server.UID;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.UUID;

public class UUIDUtil {
	public static final SecureRandom RANDOM = new SecureRandom();
	private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

	private UUIDUtil() {
		// Private constructor so that the utility-class cannot be instantiated.
	}

	/**
	 * Creates a Universally Unique Identifier, using the host IP address and the java.rmi.server.UID class.
	 */
	public static String createSimpleUUID() {
		UID uid = new UID();

		String uidString = asHex(getIPAddress()) + "-" + uid;
		// Replace semicolons by underscores, so IBIS will support it
		uidString = uidString.replace(':', '_');
		return uidString;
	}

	/**
	 * Creates a Universally Unique Identifier, using the host IP address and the java.rmi.server.UID class.
	 * This method is currently an alias for {@link #createSimpleUUID()}
	 */
	public static String createUUID() {
		return createSimpleUUID();
	}

	/**
	 * Creates a Universally Unique Identifier, via the java.util.UUID class (36 characters or 32 characters without dashes).
	 */
	public static String createRandomUUID(boolean removeDashes) {
		String uuidString = UUID.randomUUID().toString();
		if (removeDashes) {
			return uuidString.replaceAll("-", "");
		}
		return uuidString;
	}

	/**
	 * Creates a Universally Unique Identifier, via the java.util.UUID class (always 36 characters in length).
	 */
	public static String createRandomUUID() {
		return createRandomUUID(false);
	}

	/**
	 * @return the hexadecimal string representation of the byte array.
	 */
	public static String asHex(byte[] buf) {
		char[] chars = new char[2 * buf.length];
		for (int i = 0; i < buf.length; ++i) {
			chars[2 * i] = HEX_CHARS[(buf[i] & 0xF0) >>> 4];
			chars[2 * i + 1] = HEX_CHARS[buf[i] & 0x0F];
		}
		return new String(chars);
	}

	/**
	 * @return the ip address of the machine that the program runs on, as {@code byte[]}.
	 */
	private static byte[] getIPAddress() {
		InetAddress inetAddress = null;

		try {
			inetAddress = InetAddress.getLocalHost();
			return inetAddress.getAddress();
		} catch (UnknownHostException uhe) {
			return new byte[]{127, 0, 0, 1};
		}
	}

	/**
	 * @return a unique UUID string with length 31 (ipaddress with length 4*3, currentTime with length 13, hashcode with length 6) containing only digits 0-9.
	 */
	public static String createNumericUUID() {
		byte[] ipAddress = getIPAddress();
		DecimalFormat df = new DecimalFormat("000");
		String ia1 = df.format(unsignedByteToInt(ipAddress[0]));
		String ia2 = df.format(unsignedByteToInt(ipAddress[1]));
		String ia3 = df.format(unsignedByteToInt(ipAddress[2]));
		String ia4 = df.format(unsignedByteToInt(ipAddress[3]));
		String ia = ia1 + ia2 + ia3 + ia4;

		long hashL = Math.round(RANDOM.nextDouble() * 1_000_000d);
		df = new DecimalFormat("000000");
		String hash = df.format(hashL);

		// Unique string is <ipaddress with length 4*3><currentTime with length 13><hashcode with length 6>

		return ia + System.currentTimeMillis() + hash;
	}

	/**
	 * Converts an unsigned byte to its integer representation.
	 * Examples:
	 * <pre>
	 * Misc.unsignedByteToInt(new Byte(12)) returns 12
	 * Misc.unsignedByteToInt(new Byte(-12)) returns 244
	 * </pre>
	 *
	 * @param b byte to be converted.
	 * @return integer that is converted from unsigned byte.
	 */
	static int unsignedByteToInt(byte b) {
		return b & 0xFF;
	}
}
