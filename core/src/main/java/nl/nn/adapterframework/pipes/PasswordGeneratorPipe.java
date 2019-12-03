/*
   Copyright 2013 Nationale-Nederlanden

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
package nl.nn.adapterframework.pipes;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;


/**
 * Returns random password.
 *
 * @author  Milan Tomc
 * @since   4.5
 */
public class PasswordGeneratorPipe extends FixedForwardPipe {
	
	private String lCharacters="abcdefghijklmnopqrstuvwxyz";
	private String uCharacters="ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private String numbers="0123456789";
	private String signs=";:_%$#@!><";
	
	private SecureRandom random;
	private boolean useSecureRandom = true; // more secure but mutch slower
 
	int numOfLCharacters=2; 
	int numOfUCharacters=2; 
	int numOfDigits=2;
	int numOfSigns=2; 
    
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

	
	public PipeRunResult doPipe (Object input, IPipeLineSession session) throws PipeRunException {
		
		String result;
		 try {
				//generate password containing: 2 LC-letters, 2 UC-letters, 2 symbols and 2 numbers
				result =  generate(getNumOfLCharacters(),getNumOfUCharacters(),getNumOfSigns(),getNumOfDigits());
			} catch (Exception e) {
				throw new PipeRunException(this, "failed to generate password",e);
			}

		return new PipeRunResult(getForward(),result);
	}

    
	protected  String generate(int numOfLCharacters, int numOfUCharacters, int numOfSigns, int numOfNumbers){
		StringBuffer resultSb=new StringBuffer();
		resultSb.append(getRandomElementsOfString(getLCharacters(), numOfLCharacters));
		resultSb.append(getRandomElementsOfString(getUCharacters(), numOfUCharacters));
		resultSb.append(getRandomElementsOfString(getSigns(), numOfSigns));
		resultSb.append(getRandomElementsOfString(getNumbers(), numOfNumbers));
		String result=garbleString(resultSb.toString());
		return result;
	}

	protected  String getRandomElementsOfString(String input, int count){
		StringBuffer resultSb=new StringBuffer();
		for (int i=0; i<count;i++){
			int rnd;
			if (useSecureRandom)
				rnd=random.nextInt(input.length());
			else
				rnd=new Double((Math.random()*input.length()-0.5)).intValue();
			resultSb.append(input.charAt(rnd));
            
		}
		return resultSb.toString();
	}
	/**
	 * Change the order of the characters in a <code>String</code>
	 */
	protected String garbleString(String input){
		List clist=new Vector();
		for (int n=0;n<input.length(); n++){
			clist.add(""+input.charAt(n));
		}
		Collections.shuffle(clist);
		StringBuffer resultSb=new StringBuffer();
		String currentChar=null;
		for (Iterator t=clist.iterator();t.hasNext();){
			currentChar=(String)t.next();
			resultSb.append(currentChar);
		}
		return resultSb.toString();
	}


	/**
	 */
	public boolean isUseSecureRandom() {
		return useSecureRandom;
	}

	/**
	 */
	@IbisDoc({"whether the securerandom algorithm is to be used (slower)", "true"})
	public void setUseSecureRandom(boolean b) {
		useSecureRandom = b;
	}

	public String getLCharacters() {
		return lCharacters;
	}

	@IbisDoc({"the lowercase characters to use", "('a'..'z')"})
	public void setLCharacters(String lCharacters) {
		this.lCharacters = lCharacters;
	}

	public String getUCharacters() {
		return uCharacters;
	}

	@IbisDoc({"the uppercase characters to use", "('a'..'z')"})
	public void setUCharacters(String uCharacters) {
		this.uCharacters = uCharacters;
	}

	public String getNumbers() {
		return numbers;
	}

	@IbisDoc({"the numbers to use", "('0'..'9')"})
	public void setNumbers(String numbers) {
		this.numbers = numbers;
	}

	public String getSigns() {
		return signs;
	}

	@IbisDoc({"the signs to use", "(;:_%$#@!&gt;&lt;)"})
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

	@IbisDoc({"the number of lowercase characters in the generated password", "2"})
	public void setNumOfLCharacters(int i) {
		numOfLCharacters = i;
	}

	@IbisDoc({"the number of digits in the generated password", "2"})
	public void setNumOfDigits(int i) {
		numOfDigits = i;
	}

	@IbisDoc({"the number of sign characters in the generated password", "2"})
	public void setNumOfSigns(int i) {
		numOfSigns = i;
	}

	@IbisDoc({"the number of uppercase characters in the generated password", "2"})
	public void setNumOfUCharacters(int i) {
		numOfUCharacters = i;
	}

}
