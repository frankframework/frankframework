/*
 * $Log: Y01ErrorMessageFormatter.java,v $
 * Revision 1.4  2004-03-30 07:30:00  L190409
 * updated javadoc
 *
 */
package nl.nn.adapterframework.errormessageformatters;

import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.DateUtils;
/**
 * ErrorMessageFormatter for JUICE, introduced with the Y01-project.
 * 
 * @deprecated Please note that the information returned by this ErrorMessageFormatter is not very 
 * informative. Consider using one of {@Link ErrorMessageFormatter} or {@Link XslErrorMessageFormatter}
 * 
 * @version Id
 * @author Johan Verrips IOS
 */
public class Y01ErrorMessageFormatter extends ErrorMessageFormatter {
	public static final String version="$Id: Y01ErrorMessageFormatter.java,v 1.4 2004-03-30 07:30:00 L190409 Exp $";
	
	public String format(
	    String message,
	    Throwable t,
	    INamedObject location,
	    String originalMessage,
	    String messageId,
	    long receivedTime) {
		String result= "<ServiceResponse>\n" +
	            "   <ResponseEnvelope>\n" +
	            "       <serviceType>ING_RES1006</serviceType>\n" +
	            "       <messageId>" +messageId+   "</messageId>\n" +
	            "       <from>"+AppConstants.getInstance().getProperty("application.name")+
	            				" "+
	            				AppConstants.getInstance().getProperty("application.version")+
	            				"</from>\n" +
	            "       <to>JUICE</to>\n" +
	            "       <timeStamp>" + DateUtils.getIsoTimeStamp() + "</timeStamp>\n" +
	            "       <ResponseStatus>\n" +
	            "           <statusCode>999</statusCode>\n" +
	            "           <statusType>SYSTEM</statusType>\n" +
	            "           <statusText>" + message + "</statusText>\n" +
	            "       </ResponseStatus>\n" +
	            "   </ResponseEnvelope>\n" +
	            "   <Body>\n" +location.getName()+
	            "   </Body>\n" +
	            "</ServiceResponse>\n";
	
	
	
		return result;
	}
}
