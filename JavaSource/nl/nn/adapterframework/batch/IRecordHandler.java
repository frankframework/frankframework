/*
 * $Log: IRecordHandler.java,v $
 * Revision 1.1  2005-10-11 13:00:22  europe\m00f531
 * New ibis file related elements, such as DirectoryListener, MoveFilePie and 
 * BatchFileTransformerPipe
 *
 */
package nl.nn.adapterframework.batch;

import java.util.ArrayList;

import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.PipeLineSession;

/**
 * Interface for transforming a record (= structured ASCII line) 
 * 
 * @author John Dekker
 */
public interface IRecordHandler extends INamedObject {
	/**
	 * @param record to be transformed
	 * @return ArrayList with String values for each inputfield
	 * @throws Exception
	 */
	ArrayList parse(PipeLineSession session, String record) throws Exception;

	/**
	 * @param parsedRecord
	 * @return transformed result
	 * @throws Exception
	 */	
	Object handleRecord(PipeLineSession session, ArrayList parsedRecord) throws Exception;
	
	/**
	 * @param equalRecordTypes flag indicates if the previous record was of same type as the current
	 * @param prevRecord values of the input fields of the previous record
	 * @param curRecord values of the input fields of the current record
	 * @return boolean that indicates whether a prefix must be added to the transformed result
	 * @throws Exception
	 */
	boolean mustPrefix(PipeLineSession session, boolean equalRecordTypes, ArrayList prevRecord, ArrayList curRecord) throws Exception;
	
}
