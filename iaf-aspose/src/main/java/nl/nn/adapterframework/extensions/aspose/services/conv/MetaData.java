/**
 * 
 */
package nl.nn.adapterframework.extensions.aspose.services.conv;

import java.util.Objects;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

/**
/**
 * Immutable class with meta data of for each type of document.
 * 
 * @author <a href="mailto:gerard_van_der_hoorn@deltalloyd.nl">Gerard van der Hoorn</a> (d937275)
 */
//@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property="type")
//@JsonSubTypes({
//	@JsonSubTypes.Type(value=MetaData.class, name="MetaData"),
//	@JsonSubTypes.Type(value=MailMetaData.class, name="MailMetaData")
//})
public class MetaData {

	private Integer numberOfPages;

	public MetaData() {
	}

	public MetaData(Integer numberOfPages) {
		this.numberOfPages = numberOfPages;
	}

	public Integer getNumberOfPages() {
		return numberOfPages;
	}

	public void setNumberOfPages(Integer numberOfPages) {
		this.numberOfPages = numberOfPages;
	}

	@Override
	public int hashCode() {
		return Objects.hash(getNumberOfPages());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof MetaData) {
			MetaData other = (MetaData) obj;
			return Objects.equals(getNumberOfPages(), other.getNumberOfPages());
		} else {
			return false;
		}
	}

	/**
	 * Every derived class so implement this class.
	 * @return
	 */
	protected ToStringHelper toStringHelper() {
		return MoreObjects.toStringHelper(this).add("numberOfPages", getNumberOfPages());
	}

	@Override
	public final String toString() {
		return toStringHelper().toString();
	}
}