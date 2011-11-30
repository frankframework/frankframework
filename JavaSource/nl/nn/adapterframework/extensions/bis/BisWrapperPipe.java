/*
 * $Log: BisWrapperPipe.java,v $
 * Revision 1.8  2011-11-30 13:52:00  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.2  2011/10/26 12:51:14  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * deprecated
 *
 * Revision 1.1  2011/10/19 14:49:54  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.6  2011/09/22 08:54:07  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * corrected javadoc
 *
 * Revision 1.5  2011/09/22 08:45:22  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added attributes removeOutputNamespaces, omitResult and addOutputNamespace for the purpose of the migration from IFSA to TIBCO
 *
 * Revision 1.4  2011/09/16 13:30:21  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added check on soap body not empty in case of unwrap
 *
 * Revision 1.3  2011/09/16 13:11:05  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * - renamed attributes xpathExpression and namespaceDefs to inputXPath and inputNamespaceDefs
 * - added attributes outputRoot and outputNamespace
 *
 * Revision 1.2  2011/09/15 10:19:42  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * corrected javadoc
 *
 * Revision 1.1  2011/09/14 14:13:50  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * first version
 *
 *
 */
package nl.nn.adapterframework.extensions.bis;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.soap.ExtendedSoapWrapperPipe;

/**
 * Pipe to wrap or unwrap a message conformable to the BIS (Business Integration Services) standard.
 *
 * <p><b>Configuration </b><i>(where deviating from ExtendedSoapWrapperPipe)</i><b>:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setBisMessageHeaderInSoapBody(boolean) bisMessageHeaderInSoapBody}</td><td>{@link ExtendedSoapWrapperPipe#setMessageHeaderInSoapBody(boolean) messageHeaderInSoapBody}</td><td><code>false</code></td></tr>
 * <tr><td>{@link #setBisMessageHeaderSessionKey(String) bisMessageHeaderSessionKey}</td><td>{@link ExtendedSoapWrapperPipe#setMessageHeaderSessionKey(String) messageHeaderSessionKey}</td><td>bisMessageHeader</td></tr>
 * <tr><td>{@link #setBisMessageHeaderNamespace(String) bisMessageHeaderNamespace}</td><td>{@link ExtendedSoapWrapperPipe#setMessageHeaderNamespace(String) messageHeaderNamespace}</td><td>http://www.ing.com/CSP/XSD/General/Message_2</td></tr>
 * <tr><td>{@link #setBisConversationIdSessionKey(String) bisConversationIdSessionKey}</td><td>{@link ExtendedSoapWrapperPipe#setMessageHeaderConversationIdSessionKey(String) messageHeaderConversationIdSessionKey}</td><td>bisConversationId</td></tr>
 * <tr><td>{@link #setBisExternalRefToMessageIdSessionKey(String) bisExternalRefToMessageIdSessionKey}</td><td>{@link ExtendedSoapWrapperPipe#setMessageHeaderExternalRefToMessageIdSessionKey(String) messageHeaderExternalRefToMessageIdSessionKey}</td><td>bisExternalRefToMessageId</td></tr>
 * <tr><td>{@link #setBisResultInPayload(boolean) bisResultInPayload}</td><td>{@link ExtendedSoapWrapperPipe#setResultInPayload(boolean) resultInPayload}</td><td><code>true</code></td></tr>
 * <tr><td>{@link #setBisResultNamespace(String) bisResultNamespace}</td><td>{@link ExtendedSoapWrapperPipe#setResultNamespace(String) resultNamespace}</td><td>http://www.ing.com/CSP/XSD/General/Message_2</td></tr>
 * <tr><td>{@link #setBisErrorCodeSessionKey(String) bisErrorCodeSessionKey}</td><td>{@link ExtendedSoapWrapperPipe#setResultErrorCodeSessionKey(String) resultErrorCodeSessionKey}</td><td>bisErrorCode</td></tr>
 * <tr><td>{@link #setBisErrorTextSessionKey(String) bisErrorTextSessionKey}</td><td>{@link ExtendedSoapWrapperPipe#setResultErrorTextSessionKey(String) resultErrorTextSessionKey}</td><td>bisErrorText</td></tr>
 * <tr><td>{@link #setBisErrorReasonSessionKey(String) bisErrorReasonSessionKey}</td><td>{@link ExtendedSoapWrapperPipe#setResultErrorReasonSessionKey(String) resultErrorReasonSessionKey}</td><td>bisErrorReason</td></tr>
 * <tr><td>{@link #setBisServiceName(String) bisServiceName}</td><td>{@link ExtendedSoapWrapperPipe#setResultServiceName(String) resultServiceName}</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setBisActionName(String) bisActionName}</td><td>{@link ExtendedSoapWrapperPipe#setResultActionName(String) resultActionName}</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setOutputRoot(String) outputRoot}</td><td>{@link ExtendedSoapWrapperPipe#setOutputRootOnError(String) outputRootOnError}</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setOutputNamespace(String) outputNamespace}</td><td>when outputRoot is not empty, {@link ExtendedSoapWrapperPipe#setOutputNamespaceOnError(String) outputNamespaceOnError}</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setAddOutputNamespace(boolean) addOutputNamespace}</td><td>when <code>true</code>, {@link ExtendedSoapWrapperPipe#setOutputNamespace(String) outputNamespace}=outputNamespace</td><td>false</td></tr>
 * </table></p>
 * @version Id
 * @author Peter Leeuwenburgh
 * @deprecated Please replace with nl.nn.adapterframework.soap.ExtendedSoapWrapperPipe
 */
public class BisWrapperPipe extends ExtendedSoapWrapperPipe {
	private String bisMessageHeaderSessionKey = "bisMessageHeader";
	private String bisMessageHeaderNamespace = "http://www.ing.com/CSP/XSD/General/Message_2";
	private String bisConversationIdSessionKey = "bisConversationId";
	private String bisExternalRefToMessageIdSessionKey = "bisExternalRefToMessageId";
	private String bisResultNamespace = "http://www.ing.com/CSP/XSD/General/Message_2";
	private String bisErrorCodeSessionKey = "bisErrorCode";
	private String bisErrorTextSessionKey = "bisErrorText";
	private String bisErrorReasonSessionKey = "bisErrorReason";
	
	private String bisOutputNamespace;
	private boolean addOutputNamespace = false;

	public void configure() throws ConfigurationException {
		ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
		String msg = getLogPrefix(null)+"The class ["+getClass().getName()+"] has been deprecated. Please change to ["+getClass().getSuperclass().getName()+"]";
		configWarnings.add(log, msg);

		if (StringUtils.isNotEmpty(bisOutputNamespace)) {
			if (StringUtils.isNotEmpty(super.getOutputRootOnError())) {
				super.setOutputNamespaceOnError(bisOutputNamespace);
			}
			if (isAddOutputNamespace()) {
				super.setOutputNamespace(bisOutputNamespace);
			}
		}
		super.configure();
	}

	public String getMessageHeaderSessionKey() {
		return bisMessageHeaderSessionKey;
	}

	public String getMessageHeaderNamespace() {
		return bisMessageHeaderNamespace;
	}

	public String getMessageHeaderConversationIdSessionKey() {
		return bisConversationIdSessionKey;
	}

	public String getMessageHeaderExternalRefToMessageIdSessionKey() {
		return bisExternalRefToMessageIdSessionKey;
	}

	public String getResultNamespace() {
		return bisResultNamespace;
	}

	public String getResultErrorCodeSessionKey() {
		return bisErrorCodeSessionKey;
	}

	public String getResultErrorTextSessionKey() {
		return bisErrorTextSessionKey;
	}

	public String getResultErrorReasonSessionKey() {
		return bisErrorReasonSessionKey;
	}

	public void setBisMessageHeaderInSoapBody(boolean b) {
		super.setMessageHeaderInSoapBody(b);
	}

	public void setBisMessageHeaderSessionKey(String bisMessageHeaderSessionKey) {
		super.setMessageHeaderSessionKey(bisMessageHeaderSessionKey);
	}

	public void setBisMessageHeaderNamespace(String bisMessageHeaderNamespace) {
		super.setMessageHeaderNamespace(bisMessageHeaderNamespace);
	}

	public void setBisResultInPayload(boolean b) {
		super.setResultInPayload(b);
	}

	public void setBisResultNamespace(String bisResultNamespace) {
		super.setResultNamespace(bisResultNamespace);
	}

	public void setBisConversationIdSessionKey(String bisConversationIdSessionKey) {
		super.setMessageHeaderConversationIdSessionKey(bisConversationIdSessionKey);
	}

	public void setBisExternalRefToMessageIdSessionKey(String bisExternalRefToMessageIdSessionKey) {
		super.setMessageHeaderExternalRefToMessageIdSessionKey(bisExternalRefToMessageIdSessionKey);
	}

	public void setBisErrorCodeSessionKey(String bisErrorCodeSessionKey) {
		super.setResultErrorCodeSessionKey(bisErrorCodeSessionKey);
	}

	public void setBisErrorTextSessionKey(String bisErrorTextSessionKey) {
		super.setResultErrorTextSessionKey(bisErrorTextSessionKey);
	}

	public void setBisErrorReasonSessionKey(String bisErrorReasonSessionKey) {
		super.setResultErrorReasonSessionKey(bisErrorReasonSessionKey);
	}

	public void setBisServiceName(String bisServiceName) {
		super.setResultServiceName(bisServiceName);
	}

	public void setBisActionName(String bisActionName) {
		super.setResultActionName(bisActionName);
	}

	public void setOutputRoot(String outputRoot) {
		super.setOutputRootOnError(outputRoot);
	}

	public void setOutputNamespace(String outputNamespace) {
		bisOutputNamespace = outputNamespace;
	}

	public void setAddOutputNamespace(boolean b) {
		addOutputNamespace = b;
	}

	public boolean isAddOutputNamespace() {
		return addOutputNamespace;
	}
}