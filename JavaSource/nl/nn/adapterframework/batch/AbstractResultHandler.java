/*
 * $Log: AbstractResultHandler.java,v $
 * Revision 1.5  2006-05-19 09:28:36  europe\m00i745
 * Restore java files from batch package after unwanted deletion.
 *
 * Revision 1.3  2005/10/31 14:38:02  John Dekker <john.dekker@ibissource.org>
 * Add . in javadoc
 *
 * Revision 1.2  2005/10/31 07:27:58  John Dekker <john.dekker@ibissource.org>
 * Resolves bug for writing suffix
 *
 * Revision 1.1  2005/10/11 13:00:21  John Dekker <john.dekker@ibissource.org>
 * New ibis file related elements, such as DirectoryListener, MoveFilePie and 
 * BatchFileTransformerPipe
 *
 */
package nl.nn.adapterframework.batch;

import org.apache.commons.lang.StringUtils;


/**
 * Abstract class for resulthandlers (handler that handles the transformed record).
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.ibis4fundation.transformation.AbstractResultHandler</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>Name of the resulthandler</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPrefix(String) prefix}</td><td>Prefix that has to be written before record, if the record is in another block than the previous record</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSuffix(String) suffix}</td><td>Suffix that has to be written after the record, if the record is in another block than the next record</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * 
 * @author: John Dekker
 */
public abstract class AbstractResultHandler implements IResultHandler {
	private String prefix;
	private String suffix;
	private String name;
	
	protected AbstractResultHandler() {
	}

	public String getName() {
		return name;
	}

	public void setName(String string) {
		name = string;
	}

	public String getPrefix() {
		return prefix;
	}

	public String getSuffix() {
		return suffix;
	}

	public void setPrefix(String string) {
		prefix = string;
	}

	public void setSuffix(String string) {
		suffix = string;
	}

	protected String[] prefix(boolean mustPrefix, boolean hasPreviousRecord) {
		if (! mustPrefix || StringUtils.isEmpty(prefix)) {
			return null;
		}
		if (! hasPreviousRecord) {
			return new String[] { prefix }; 
		}
		else {
			return new String[] { suffix, prefix };
		}
	}
}
