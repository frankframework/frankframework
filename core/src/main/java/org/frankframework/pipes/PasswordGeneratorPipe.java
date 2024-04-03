/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2023 WeAreFrank!

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
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.ElementType;
import org.frankframework.doc.ElementType.ElementTypes;
import org.frankframework.stream.Message;


/**
 * Returns random password.
 *
 * @author  Milan Tomc
 * @since   4.5
 */
@ElementType(ElementTypes.TRANSLATOR)
public class PasswordGeneratorPipe extends FixedForwardPipe {

	private String lCharacters="abcdefghijklmnopqrstuvwxyz";
	private String uCharacters="ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private String numbers="0123456789";
	private String signs=";:_%$#@!><";

	private SecureRandom random;
	private boolean useSecureRandom = true; // more secure but much slower

	int numOfLCharacters=2;
	int numOfUCharacters=2;
	int numOfDigits=2;
	int numOfSigns=2;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		if (useSecureRandom){
			try {
				random= SecureRandom.getInstance("SHA1PRNG");
			} catch (NoSuchAlgorithmException e) {
				try{
					random= SecureRandom.getInstance("IBMSecureRandom");
				}
				catch (NoSuchAlgorithmException ex){
					throw new ConfigurationException("PasswordGeneratorPipe: ", ex);
				}
			}
		}
	}


	@Override
	public PipeRunResult doPipe (Message message, PipeLineSession session) throws PipeRunException {
		String result;
		try {
				//generate password containing: 2 LC-letters, 2 UC-letters, 2 symbols and 2 numbers
				result =  generate(getNumOfLCharacters(),getNumOfUCharacters(),getNumOfSigns(),getNumOfDigits());
			} catch (Exception e) {
				throw new PipeRunException(this, "failed to generate password",e);
			}

		return new PipeRunResult(getSuccessForward(),result);
	}

	protected  String generate(int numOfLCharacters, int numOfUCharacters, int numOfSigns, int numOfNumbers){
		StringBuilder result = new StringBuilder();
		result.append(getRandomElementsOfString(getLCharacters(), numOfLCharacters));
		result.append(getRandomElementsOfString(getUCharacters(), numOfUCharacters));
		result.append(getRandomElementsOfString(getSigns(), numOfSigns));
		result.append(getRandomElementsOfString(getNumbers(), numOfNumbers));
		return garbleString(result.toString());
	}

	protected  String getRandomElementsOfString(String input, int count){
		StringBuilder resultSb=new StringBuilder();
		for (int i=0; i<count;i++){
			int rnd;
			if (useSecureRandom)
				rnd=random.nextInt(input.length());
			else
				rnd = Double.valueOf((Math.random() * input.length() - 0.5)).intValue();
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


	public boolean isUseSecureRandom() {
		return useSecureRandom;
	}

	/**
	 * whether the securerandom algorithm is to be used (slower)
	 * @ff.default true
	 */
	public void setUseSecureRandom(boolean b) {
		useSecureRandom = b;
	}

	public String getLCharacters() {
		return lCharacters;
	}

	/**
	 * the lowercase characters to use
	 * @ff.default ('a'..'z')
	 */
	public void setLCharacters(String lCharacters) {
		this.lCharacters = lCharacters;
	}

	public String getUCharacters() {
		return uCharacters;
	}

	/**
	 * the uppercase characters to use
	 * @ff.default ('A'..'Z')
	 */
	public void setUCharacters(String uCharacters) {
		this.uCharacters = uCharacters;
	}

	public String getNumbers() {
		return numbers;
	}

	/**
	 * the numbers to use
	 * @ff.default ('0'..'9')
	 */
	public void setNumbers(String numbers) {
		this.numbers = numbers;
	}

	public String getSigns() {
		return signs;
	}

	/**
	 * the signs to use
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
	 * the number of lowercase characters in the generated password
	 * @ff.default 2
	 */
	public void setNumOfLCharacters(int i) {
		numOfLCharacters = i;
	}

	/**
	 * the number of digits in the generated password
	 * @ff.default 2
	 */
	public void setNumOfDigits(int i) {
		numOfDigits = i;
	}

	/**
	 * the number of sign characters in the generated password
	 * @ff.default 2
	 */
	public void setNumOfSigns(int i) {
		numOfSigns = i;
	}

	/**
	 * the number of uppercase characters in the generated password
	 * @ff.default 2
	 */
	public void setNumOfUCharacters(int i) {
		numOfUCharacters = i;
	}

}
