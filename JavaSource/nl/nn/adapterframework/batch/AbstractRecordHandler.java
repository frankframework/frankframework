/*
 * $Log: AbstractRecordHandler.java,v $
 * Revision 1.5  2007-05-03 11:29:43  europe\L190409
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

import org.apache.log4j.Logger;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.util.LogUtil;

/**
 * Abstract class that contains functionality for parsing the field values from a 
 * record (line). Fields in the record are either seperated with a seperator or have
 * a fixed position in the line.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.batch.AbstractRecordHandler</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the RecordHandler</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setInputFields(String) inputFields}</td><td>Comma seperated specification of fieldlengths</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setInputSeperator(String) inputSeperator}</td><td>Seperator that seperated the fields in the record</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFieldsDifferConditionForPrefix(String) inputFields}</td><td>Comma seperated numbers of those fields that are compared with the previous record to determine if a prefix must be written</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * 
 * @author: John Dekker
 */
public abstract class AbstractRecordHandler implements IRecordHandler {
	protected Logger log = LogUtil.getLogger(this);

	private String name;
	private List inputFields; 
	private String inputSeperator;
	private List seperatorWhenFieldsDiffer;
	
	protected AbstractRecordHandler() {
		this.inputFields = new LinkedList();
		this.seperatorWhenFieldsDiffer = new LinkedList();
	}

	public void configure() throws ConfigurationException {
		//nothing to do		
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

	public void setInputFields(String fieldLengths) {
		StringTokenizer st = new StringTokenizer(fieldLengths, ",");
		while (st.hasMoreTokens()) {
			String token = st.nextToken().trim();
			addInputField(Integer.parseInt(token));
		}
	}

	protected int getNumberOfInputFields() {
		return inputFields.size();
	}
	
	public ArrayList parse(PipeLineSession session, String record) {
		if (inputFields.size() > 0) {
			return parseUsingInputFields(record);
		}
		else if (inputSeperator != null) {
			return parseUsingSeperator(record);
		}
		else {
			ArrayList result = new ArrayList();
			result.add(record);
			return result;
		}
	}
	
	private ArrayList parseUsingInputFields(String record) {
		ArrayList result = new ArrayList();

		int recordLength = record.length(); 
		int curPos = 0;
		for (Iterator fieldIt = inputFields.iterator(); fieldIt.hasNext();) {
			InputField field = (InputField) fieldIt.next();
			int endPos = curPos + field.length; 
			
			if (curPos >= recordLength) {
				result.add("");
			}
			else if (endPos >= recordLength) {
				result.add(record.substring(curPos));
			}
			else {
				result.add(record.substring(curPos, endPos));
			}
			
			curPos = endPos;
		}
		
		return result;
	}
	
	public boolean mustPrefix(PipeLineSession session, boolean equalRecordTypes, ArrayList prevRecord, ArrayList curRecord) {
		if (! equalRecordTypes) {
			return true;
		}
			
		if (getSeperatorWhenFieldsDiffer().size() > 0) {
			if (prevRecord == null) {
				return true;
			}
			for (Iterator fieldNdxIt = seperatorWhenFieldsDiffer.iterator(); fieldNdxIt.hasNext();) {
				int ndx = ((Integer)fieldNdxIt.next()).intValue();
				if (! prevRecord.get(ndx-1).equals(curRecord.get(ndx-1))) {
					return true;
				}
			}
		}
		return false;
	}
	
	private ArrayList parseUsingSeperator(String record) {
		ArrayList result = new ArrayList();
		
		int endNdx = -1;
		do {
			int startNdx = endNdx + 1;
			endNdx = record.indexOf(inputSeperator, startNdx);
			if (endNdx == -1) {
				result.add(record.substring(startNdx));
			}
			else {
				result.add(record.substring(startNdx, endNdx));
			}
		}
		while(endNdx != -1);
		
		return result;
	}

	protected class InputField {
		private int length;
		
		InputField(int length) {
			this.length = length;
		}
	}
	public String getName() {
		return name;
	}

	public void setName(String string) {
		name = string;
	}

	public String getInputSeperator() {
		return inputSeperator;
	}

	public void setInputSeperator(String string) {
		inputSeperator = string;
	}

	public List getSeperatorWhenFieldsDiffer() {
		return seperatorWhenFieldsDiffer;
	}

	public void setSeperatorWhenFieldsDiffer(List list) {
		seperatorWhenFieldsDiffer = list;
	}

	public void setFieldsDifferConditionForPrefix(String fieldNrs) {
		StringTokenizer st = new StringTokenizer(fieldNrs, ",");
		while (st.hasMoreTokens()) {
			String token = st.nextToken().trim();
			seperatorWhenFieldsDiffer.add(new Integer(token));
		}
	}

}
