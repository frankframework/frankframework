package org.frankframework.mailsenders;

import org.frankframework.core.ISenderWithParameters;

org.frankframework.core.ISenderWithParameters;

public interface IMailSender extends ISenderWithParameters {

	@Deprecated
	public void setSmtpAuthAlias(String smtpAuthAlias);

	@Deprecated
	public void setSmtpUserid(String smtpUserId);

	@Deprecated
	public void setSmtpPassword(String smtpPassword);

	public void setDefaultSubject(String string);

}
