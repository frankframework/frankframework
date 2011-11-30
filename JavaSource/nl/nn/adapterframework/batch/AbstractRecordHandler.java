/*
 * $Log: AbstractRecordHandler.java,v $
 * Revision 1.17  2011-11-30 13:51:56  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:48  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.15  2010/01/27 13:48:12  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added getRecordType()
 * modified isNewRecordType, to better detect what is promised by its name
 *
 * Revision 1.14  2008/12/30 17:01:13  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added configuration warnings facility (in Show configurationStatus)
 *
 * Revision 1.13  2008/07/14 17:52:50  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * return whole record when no fields or separator specified
 *
 * Revision 1.12  2008/02/19 09:23:48  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.11  2008/02/15 16:04:22  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added trim attribute
 *
 * Revision 1.10  2007/10/08 13:28:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed ArrayList to List where possible
 *
 * Revision 1.9  2007/09/24 14:55:32  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * support for parameters
 *
 * Revision 1.8  2007/09/10 11:11:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * renamed mustPrefix() to isNewRecordType()
 * renamed attribute 'separatorWhenFieldsDiffer' to 'recordIdentifyingFields'
 *
 * Revision 1.7  2007/08/03 08:24:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.6  2007/07/26 16:02:37  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed seperator into separator
 *
 * Revision 1.5  2007/05/03 11:29:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * add methods configure(), open() and close()
 *
 * Revision 1.4  2006/05/19 09:01:49  Peter Eijgermans <peter.eijgermans@ibissource.org>
 * Restore java files from batch package after unwanted deletion.
 *
 * Revision 1.2  2005/10/13 14:09:41  John Dekker <john.dekker@ibissource.org>
 * StringTokenizer did not return token if two delimeters follow eachother directly.
 *
 * Revision 1.1  2005/10/11 13:00:21  John Dekker <john.dekker@ibissource.org>
 * New ibis file related elements, such as DirectoryListener, MoveFilePie and 
 * BatchFileTransformerPipe
 *
 */
package nl.nn.adapterframework.batch;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.IWithParameters;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Abstract class that contains functionality for parsing the field values from a 
 * record (line). Fields in the record are either separated with a separator or have
 * a fixed position in the line.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.batch.AbstractRecordHandler</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the RecordHandler</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setInputFields(String) inputFields}</td><td>Comma separated specification of fieldlengths. If neither this attribute nor <code>inputSeparator</code> is specified then the entire record is parsed</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setInputSeparator(String) inputSeparator}</td><td>Separator that separated the fields in the input record. If neither this attribute nor <code>inputFields</code> is specified then the entire record is parsed</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTrim(boolean) trim}</td><td>when set <code>true</code>, trailing spaces are removed from each field</td><td>false</td></tr>
 * <tr><td>{@link #setRecordIdentifyingFields(String) recordIdentifyingFields}</td><td>Comma separated list of numbers of those fields that are compared with the previous record to determine if a prefix must be written. If any of these fields is not equal in both records, the record types are assumed to be different</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * 
 * @author  John Dekker
 * @version Id
 */
public abstract class AbstractRecordHandler implements IRecordHandler, IWithParameters {
	protected Logger log = LogUtil.getLogger(this);

	private String name;
	private String inputSeparator;
	private boolean trim=false;
	
	private List inputFields=new LinkedList(); 
	private List recordIdentifyingFields=new LinkedList();
	
	protected ParameterList paramList = null;

	public void configure() throws ConfigurationException {
		if (paramList!=null) {
			paramList.configure();
		}
		if (inputFields.size()>0 && StringUtils.isNotEmpty(getInputSeparator())) {
			throw new ConfigurationException(ClassUtils.nameOf(this)+" ["+getName()+"] inputFields and inputSeparator cannot be specified both");
		}
	}
	
	public void open() throws SenderException {
		//nothing to do		
	}
	public void close() throws SenderException {
		//nothing to do		
	}

	public void addInputField(int length) {
		inputFields.add(new InputField(length));
	}
	
	public void registerChild(InputfieldsPart part) {
		setInputFields(part.getValue());
	}


	protected int getNumberOfInputFields() {
		return inputFields.size();
	}
	
	public List parse(PipeLineSession session, String record) {
		if (inputFields.size() > 0) {
			return parseUsingInputFields(record);
		}
		else if (inputSeparator != null) {
			return parseUsingSeparator(record);
		}
		else {
			List result = new ArrayList();
			result.add(record);
			return result;
		}
	}
	
	private List parseUsingInputFields(String record) {
		List result = new ArrayList();

		int recordLength = record.length(); 
		int curPos = 0;
		for (Iterator fieldIt = inputFields.iterator(); fieldIt.hasNext();) {
			InputField field = (InputField) fieldIt.next();
			int endPos = curPos + field.length; 
			
			String item;
			if (curPos >= recordLength) {
				item="";
			}
			else if (endPos >= recordLength) {
				item=record.substring(curPos);
			}
			else {
				item=record.substring(curPos, endPos);
			}
			if (isTrim()) {
				result.add(item.trim());
			} else {
				result.add(item);
			}
			
			curPos = endPos;
		}
		
		return result;
	}

	private List parseUsingSeparator(String record) {
		List result = new ArrayList();
		
		int endNdx = -1;
		do {
			int startNdx = endNdx + 1;
			endNdx = record.indexOf(inputSeparator, startNdx);
			String item;
			if (endNdx == -1) {
				item=record.substring(startNdx);
			}
			else {
				item=record.substring(startNdx, endNdx);
			}
			if (isTrim()) {
				result.add(item.trim());
			} else {
				result.add(item);
			}
		}
		while(endNdx != -1);
		
		return result;
	}
	
	public String getRecordType(List record) {
		String result=null;
		
		for (Iterator it = recordIdentifyingFields.iterator(); it.hasNext();) {
			int i = ((Integer)it.next()).intValue();
			Object field=record.get(i-1);
			String fieldValue=field==null?"":field.toString();
			if (result==null) {
				result=fieldValue;
			} else {
				result+="_"+fieldValue;
			}
		}
		return result;
	}
	
	public boolean isNewRecordType(PipeLineSession session, boolean equalRecordHandlers, List prevRecord, List curRecord) {
		if (getRecordIdentifyingFieldList().size() == 0) {
			log.debug("isNewRecordType(): no RecordIdentifyingFields specified, so returning false");
			return false;
		}
		if (! equalRecordHandlers) {
			log.debug("isNewRecordType(): equalRecordTypes ["+equalRecordHandlers+"], so returning true");
			return true;
		}
			
		if (prevRecord == null) {
			log.debug("isNewRecordType(): no previous record, so returning true");
			return true;
		}
		for (Iterator it = recordIdentifyingFields.iterator(); it.hasNext();) {
			int i = ((Integer)it.next()).intValue();
			Object prevField=prevRecord.get(i-1);
			Object curField=curRecord.get(i-1);
			if (! prevField.equals(curField)) {
				log.debug("isNewRecordType(): fields ["+i+"] different previous value ["+prevField+"] current value ["+curField+"], so returning true");
				return true;
			}
		}
		return false;
	}
	

	protected class InputField {
		private int length;
		
		InputField(int length) {
			this.length = length;
		}
	}

	/*
	 * this method returns a List, and therefore cannot be called 'getRecordIdentifyingFields', 
	 * because then setRecordIdentifyingFields is not found as a setter.
	 */  
	public List getRecordIdentifyingFieldList() {
		return recordIdentifyingFields;
	}

	public void setRecordIdentifyingFields(String fieldNrs) {
		StringTokenizer st = new StringTokenizer(fieldNrs, ",");
		while (st.hasMoreTokens()) {
			String token = st.nextToken().trim();
			// log.debug("setRecordIdentifyingFields() found identifiying field ["+token+"]");
			recordIdentifyingFields.add(new Integer(token));
		}
		if (recordIdentifyingFields.size()==0) {
			log.warn("setRecordIdentifyingFields(): value ["+fieldNrs+"] did result in an empty list of tokens");
		}
	}
	public void setFieldsDifferConditionForPrefix(String fieldNrs) {
		ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
		String msg = ClassUtils.nameOf(this) +"["+getName()+"]: the attribute 'fieldsDifferConditionForPrefix' has been renamed 'recordIdentifyingFields' since version 4.7";
		configWarnings.add(log, msg);
		setRecordIdentifyingFields(fieldNrs);
	}


	public void addParameter(Parameter p) {
		if (paramList==null) {
			paramList=new ParameterList();
		}
		paramList.add(p);
	}


	public void setName(String string) {
		name = string;
	}
	public String getName() {
		return name;
	}


	public void setInputFields(String fieldLengths) {
		StringTokenizer st = new StringTokenizer(fieldLengths, ",");
		while (st.hasMoreTokens()) {
			String token = st.nextToken().trim();
			addInputField(Integer.parseInt(token));
		}
	}


	/**
	 * @deprecated typo has been fixed: please use 'inputSeparator' instead of 'inputSeperator'
	 */
	public void setInputSeperator(String string) {
		ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
		String msg = ClassUtils.nameOf(this) +"["+getName()+"]: typo has been fixed: please use 'inputSeparator' instead of 'inputSeperator'";
		configWarnings.add(log, msg);
		setInputSeparator(string);
	}
	public void setInputSeparator(String string) {
		inputSeparator = string;
	}
	public String getInputSeparator() {
		return inputSeparator;
	}

	public void setTrim(boolean b) {
		trim = b;
	}
	public boolean isTrim() {
		return trim;
	}

}
