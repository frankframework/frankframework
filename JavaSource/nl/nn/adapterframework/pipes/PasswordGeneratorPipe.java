/*
 * $Log: PasswordGeneratorPipe.java,v $
 * Revision 1.5  2011-11-30 13:51:50  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:45  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.3  2009/12/01 14:40:32  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Fixed javadoc lCharacters -> LCharacters and uCharacters -> UCharacters
 *
 * Revision 1.2  2007/10/16 07:53:14  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Added set/get methods for lCharacters, uCharacters, numbers and signs. Fixed some typo's in javadoc.
 *
 * Revision 1.1  2006/09/18 13:08:42  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 *
 */
package nl.nn.adapterframework.pipes;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;


/**
 * Returns random password.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxThreads(int) maxThreads}</td><td>maximum number of threads that may call {@link #doPipe(Object, nl.nn.adapterframework.core.PipeLineSession)} simultaneously</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setForwardName(String) forwardName}</td>  <td>name of forward returned upon completion</td><td>"success"</td></tr>
 * <tr><td>{@link #setLCharacters(String) LCharacters}</td><td>The lowercase characters to use</td><td>('a'..'z')</td></tr>
 * <tr><td>{@link #setUCharacters(String) UCharacters}</td><td>The uppercase characters to use</td><td>('A'..'Z')</td></tr>
 * <tr><td>{@link #setNumbers(String) numbers}</td><td>The numbers to use</td><td>('0'..'9')</td></tr>
 * <tr><td>{@link #setSigns(String) signs}</td><td>The signs to use</td><td>(;:_%$#@!&gt;&lt;)</td></tr>
 * <tr><td>{@link #setUseSecureRandom(boolean) useSecureRandom}</td>  <td>Whether the SecureRandom algorithm is to be used (slower)</td><td>true</td></tr>
 * <tr><td>{@link #setNumOfLCharacters(int) numOfLCharacters}</td><td>The number of lowercase characters in the generated password</td><td>2</td></tr>
 * <tr><td>{@link #setNumOfUCharacters(int) numOfUCharacters}</td><td>The number of uppercase characters in the generated password</td><td>2</td></tr>
 * <tr><td>{@link #setNumOfDigits(int) numOfDigits}</td><td>The number of digits in the generated password</td><td>2</td></tr>
 * <tr><td>{@link #setNumOfSigns(int) numOfSigns}</td><td>The number of sign characters in the generated password</td><td>2</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified</td></tr>
 * </table>
 * </p>
 * @version Id
 * @author  Milan Tomc
 * @since   4.5
 */
public class PasswordGeneratorPipe extends FixedForwardPipe {
	public static final String version = "$RCSfile: PasswordGeneratorPipe.java,v $ $Revision: 1.5 $ $Date: 2011-11-30 13:51:50 $";
	
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

	
	public PipeRunResult doPipe (Object input, PipeLineSession session) throws PipeRunException {
		
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
	 * @param input
	 * @return
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
	 * @return
	 */
	public boolean isUseSecureRandom() {
		return useSecureRandom;
	}

	/**
	 * @param b
	 */
	public void setUseSecureRandom(boolean b) {
		useSecureRandom = b;
	}

	public String getLCharacters() {
		return lCharacters;
	}

	public void setLCharacters(String lCharacters) {
		this.lCharacters = lCharacters;
	}

	public String getUCharacters() {
		return uCharacters;
	}

	public void setUCharacters(String uCharacters) {
		this.uCharacters = uCharacters;
	}

	public String getNumbers() {
		return numbers;
	}

	public void setNumbers(String numbers) {
		this.numbers = numbers;
	}

	public String getSigns() {
		return signs;
	}

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

	public void setNumOfLCharacters(int i) {
		numOfLCharacters = i;
	}

	public void setNumOfDigits(int i) {
		numOfDigits = i;
	}

	public void setNumOfSigns(int i) {
		numOfSigns = i;
	}

	public void setNumOfUCharacters(int i) {
		numOfUCharacters = i;
	}

}
