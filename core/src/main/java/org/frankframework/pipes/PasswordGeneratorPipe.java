/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2023-2024 WeAreFrank!

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
package org.frankframework.pipes;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.stream.Message;
import org.frankframework.util.UUIDUtil;


/**
 * Generates a random password.
 *
 * @author Milan Tomc
 * @since 4.5
 */
@EnterpriseIntegrationPattern(EnterpriseIntegrationPattern.Type.TRANSLATOR)
public class PasswordGeneratorPipe extends FixedForwardPipe {

	private String lCharacters = "abcdefghijklmnopqrstuvwxyz";
	private String uCharacters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private String numbers = "0123456789";
	private String signs = ";:_%$#@!><";

	private static SecureRandom random; // different algorithm than standard Random

	int numOfLCharacters = 2;
	int numOfUCharacters = 2;
	int numOfDigits = 2;
	int numOfSigns = 2;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		if (random == null) {
			try {
				random = SecureRandom.getInstance("SHA1PRNG");
			} catch (NoSuchAlgorithmException e) {
				random = UUIDUtil.RANDOM; // fallback to shared UUIDUtil random
			}
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		String result;
		try {
			//generate password containing: 2 LC-letters, 2 UC-letters, 2 symbols and 2 numbers
			result = generate(getNumOfLCharacters(), getNumOfUCharacters(), getNumOfSigns(), getNumOfDigits());
		} catch (Exception e) {
			throw new PipeRunException(this, "failed to generate password", e);
		}

		return new PipeRunResult(getSuccessForward(), result);
	}

	protected String generate(int numOfLCharacters, int numOfUCharacters, int numOfSigns, int numOfNumbers) {
		String result = getRandomElementsOfString(getLCharacters(), numOfLCharacters) +
				getRandomElementsOfString(getUCharacters(), numOfUCharacters) +
				getRandomElementsOfString(getSigns(), numOfSigns) +
				getRandomElementsOfString(getNumbers(), numOfNumbers);
		return garbleString(result);
	}

	protected String getRandomElementsOfString(String input, int count) {
		StringBuilder resultSb = new StringBuilder();
		for (int i = 0; i < count; i++) {
			int rnd = random.nextInt(input.length());
			resultSb.append(input.charAt(rnd));
		}
		return resultSb.toString();
	}

	/**
	 * Change the order of the characters in a <code>String</code>
	 */
	protected String garbleString(String input) {
		List<String> clist = new ArrayList<>();
		for (int n = 0; n < input.length(); n++) {
			clist.add(String.valueOf(input.charAt(n)));
		}
		Collections.shuffle(clist);
		StringBuilder resultSb = new StringBuilder();
		String currentChar;
		for (String s : clist) {
			currentChar = s;
			resultSb.append(currentChar);
		}
		return resultSb.toString();
	}

	/**
	 * Whether the secureRandom algorithm is used.
	 * @ff.default true
	 * @deprecated the current implementation always uses SecureRandom. Please remove this attribute from the Configuration.
	 */
	@Deprecated(forRemoval = true, since = "8.2.0")
	@ConfigurationWarning("the current implementation always uses SecureRandom. Please remove this attribute from the Configuration.")
	public void setUseSecureRandom(boolean b) {
		// do nothing
	}

	public String getLCharacters() {
		return lCharacters;
	}

	/**
	 * The lowercase characters to use.
	 * @ff.default ('a'..'z')
	 */
	public void setLCharacters(String lCharacters) {
		this.lCharacters = lCharacters;
	}

	public String getUCharacters() {
		return uCharacters;
	}

	/**
	 * The uppercase characters to use.
	 * @ff.default ('A'..'Z')
	 */
	public void setUCharacters(String uCharacters) {
		this.uCharacters = uCharacters;
	}

	public String getNumbers() {
		return numbers;
	}

	/**
	 * The numbers to use.
	 * @ff.default ('0'..'9')
	 */
	public void setNumbers(String numbers) {
		this.numbers = numbers;
	}

	public String getSigns() {
		return signs;
	}

	/**
	 * The signs to use.
	 * @ff.default (;:_%$#@!&gt;&lt;)
	 */
	public void setSigns(String signs) {
		this.signs = signs;
	}

	public int getNumOfLCharacters() {
		return numOfLCharacters;
	}

	public int getNumOfDigits() {
		return numOfDigits;
	}

	public int getNumOfSigns() {
		return numOfSigns;
	}

	public int getNumOfUCharacters() {
		return numOfUCharacters;
	}

	/**
	 * The number of lowercase characters in the generated password.
	 * @ff.default 2
	 */
	public void setNumOfLCharacters(int i) {
		numOfLCharacters = i;
	}

	/**
	 * The number of digits in the generated password.
	 * @ff.default 2
	 */
	public void setNumOfDigits(int i) {
		numOfDigits = i;
	}

	/**
	 * The number of sign characters in the generated password.
	 * @ff.default 2
	 */
	public void setNumOfSigns(int i) {
		numOfSigns = i;
	}

	/**
	 * The number of uppercase characters in the generated password.
	 * @ff.default 2
	 */
	public void setNumOfUCharacters(int i) {
		numOfUCharacters = i;
	}

}
