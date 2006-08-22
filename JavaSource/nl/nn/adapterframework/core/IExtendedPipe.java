/*
 * $Log: IExtendedPipe.java,v $
 * Revision 1.2  2006-08-22 12:51:00  europe\L190409
 * added preserveInput attribute
 *
 * Revision 1.1  2005/09/08 15:52:18  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved extra functionality to IExtendedPipe
 *
 */
package nl.nn.adapterframework.core;

/**
 * extra attributes to do logging and use sessionvariables.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setDurationThreshold(long) durationThreshold}</td><td>if durationThreshold >=0 and the duration (in milliseconds) of the message processing exceeded the value specified the message is logged informatory</td><td>-1</td></tr>
 * <tr><td>{@link #setGetInputFromSessionKey(String) getInputFromSessionKey}</td><td>when set, input is taken from this session key, instead of regular input</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStoreResultInSessionKey(String) storeResultInSessionKey}</td><td>when set, the result is stored under this session key</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * 
 * @author  Gerrit van Brakel
 * @since   4.3
 * @version Id
 */
public interface IExtendedPipe extends IPipe {

	/**
	 * Sets a threshold for the duration of message execution; 
	 * If the threshold is exceeded, the message is logged to be analyzed.
	 */
	public void setDurationThreshold(long maxDuration) ;
	public long getDurationThreshold();



	public void setGetInputFromSessionKey(String string);
	public String getGetInputFromSessionKey();

	public void setStoreResultInSessionKey(String string);
	public String getStoreResultInSessionKey();

	public void setPreserveInput(boolean preserveInput);
	public boolean isPreserveInput();

}
