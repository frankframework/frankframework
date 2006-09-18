/*
 * $Log: PasswordGeneratorPipe.java,v $
 * Revision 1.1  2006-09-18 13:08:42  europe\L190409
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
 * <tr><td>{@link #setUseSecureRandom(String) useSecureRandom}</td>  <td>Whether the SecureRandom algorithm is to be used (slower)</td><td>true</td></tr>
 * <tr><td>{@link #setNumOfLCharacters(int) numOfLCharacters}</td><td>The number of lowercase characters ('a'..'z') in the generated password</td><td>2</td></tr>
 * <tr><td>{@link #setNumOfUCharacters(int) numOfUCharacters}</td><td>The number of uppercase characters ('A'..'Z') in the generated password</td><td>2</td></tr>
 * <tr><td>{@link #setNumOfDigitss(int) numOfDigits}</td><td>The number of digits ('0'..'9') in the generated password</td><td>2</td></tr>
 * <tr><td>{@link #setNumOfSigns(int) numOfSigns}</td><td>The number of sign characters (one of ;:_%$#@!&gt;&lt;) in the generated password</td><td>2</td></tr>
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
	public static final String version = "$RCSfile: PasswordGeneratorPipe.java,v $ $Revision: 1.1 $ $Date: 2006-09-18 13:08:42 $";
	
	private static final String LCHARACTERS="abcdefghijklmnopqrstuvwxyz";
	private static final String UCHARACTERS="ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static final String NUMBERS="0123456789";
	private static final String SIGNS=";:_%$#@!><";
	
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
		resultSb.append(getRandomElementsOfString(LCHARACTERS, numOfLCharacters));
		resultSb.append(getRandomElementsOfString(UCHARACTERS, numOfUCharacters));
		resultSb.append(getRandomElementsOfString(SIGNS, numOfSigns));
		resultSb.append(getRandomElementsOfString(NUMBERS, numOfNumbers));
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
