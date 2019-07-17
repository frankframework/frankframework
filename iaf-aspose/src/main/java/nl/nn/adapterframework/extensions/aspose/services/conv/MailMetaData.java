/**
 * 
 */
package nl.nn.adapterframework.extensions.aspose.services.conv;

import java.util.Date;
import java.util.Objects;

import com.google.common.base.MoreObjects.ToStringHelper;

import nl.nn.adapterframework.extensions.aspose.services.util.ConvertUtil;

/**
 * Immutable class with meta data of a mail.
 * 
 * @author <a href="mailto:gerard_van_der_hoorn@deltalloyd.nl">Gerard van der
 *         Hoorn</a> (d937275)
 */
public class MailMetaData extends MetaData {

	private String from;

	private Date sentTimestamp;

	private String recipient;

	private String subject;

	public MailMetaData() {
	}

	public MailMetaData(Integer numberOfPages, String from, Date sentTimestamp, String recipient, String subject) {
		super(numberOfPages);
		this.from = from;
		setSentTimestamp(sentTimestamp);
		this.recipient = recipient;
		this.subject = subject;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	// @JsonSerialize(using = JsonDateSerializer.class)
	public Date getSentTimestamp() {
		return sentTimestamp;
	}

	/**
	 * Set timestamp. Clear milliseconds so values can be checked without the
	 * millisecond.
	 * 
	 * @param sentTimestamp
	 */
	// @JsonDeserialize(using = JsonDateDeserializer.class)
	public void setSentTimestamp(Date sentTimestamp) {
		this.sentTimestamp = sentTimestamp;
		// if (sentTimestamp == null) {
		// this.sentTimestamp = null;
		// } else {
		// this.sentTimestamp = new Date((sentTimestamp.getTime() / 1000L) * 1000L);
		// }
	}

	public String getRecipient() {
		return recipient;
	}

	public void setRecipient(String recipient) {
		this.recipient = recipient;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), getFrom(), getSentTimestamp(), getRecipient(), getSubject());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof MailMetaData) {
			MailMetaData other = (MailMetaData) obj;
			return super.equals(other) && Objects.equals(getFrom(), other.getFrom())
					&& Objects.equals(getSentTimestamp(), other.getSentTimestamp())
					&& Objects.equals(getRecipient(), other.getRecipient())
					&& Objects.equals(getSubject(), other.getSubject());
		} else {
			return false;
		}
	}

	@Override
	protected ToStringHelper toStringHelper() {
		return super.toStringHelper().add("from", getFrom())
				.add("sentTimestamp", ConvertUtil.convertTimestampToStr(getSentTimestamp()))
				.add("recipient", getRecipient()).add("subject", getSubject());
	}
}
