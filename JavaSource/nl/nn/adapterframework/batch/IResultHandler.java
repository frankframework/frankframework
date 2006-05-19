/*
 * $Log: IResultHandler.java,v $
 * Revision 1.4  2006-05-19 09:28:36  europe\m00i745
 * Restore java files from batch package after unwanted deletion.
 *
 * Revision 1.2  2005/10/31 14:38:03  John Dekker <john.dekker@ibissource.org>
 * Add . in javadoc
 *
 * Revision 1.1  2005/10/11 13:00:20  John Dekker <john.dekker@ibissource.org>
 * New ibis file related elements, such as DirectoryListener, MoveFilePie and 
 * BatchFileTransformerPipe
 *
 */
package nl.nn.adapterframework.batch;

import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.PipeLineSession;

/**
 * Interface for handling a transformed record.
 * 
 * @author John Dekker
 */
public interface IResultHandler extends INamedObject {
	/**
	 * @param inputFilename name of the original file containing the untransformed record
	 * @param recordKey key of the record (describes the record type)
	 * @param result transformed record
	 * @throws Exception
	 */
	void handleResult(PipeLineSession session, String inputFilename, String recordKey, Object result) throws Exception;
	
	/**
	 * Called when all records in the original file are handled
	 * @param inputFilename name of the original file containing the untransformed record
	 * @return the name or names of the output files
	 * @throws Exception
	 */
	Object finalizeResult(PipeLineSession session, String inputFilename, boolean error) throws Exception;

	/**
	 * @param inputFilename name of the original file containing the untransformed record
	 * @param mustPrefix boolean indicates if the prefix must be written
	 * @param hasPreviousRecord boolean indicates if a previous record has been written, in case a suffix has to be written first
	 * @throws Exception
	 */
	void writePrefix(PipeLineSession session, String inputFilename, boolean mustPrefix, boolean hasPreviousRecord) throws Exception;
	
	/**
	 * @param inputFilename name of the original file containing the untransformed record
	 * @throws Exception
	 */
	void writeSuffix(PipeLineSession session, String inputFilename) throws Exception;
	
	/**
	 * @return true if this resulthandler should be used for all flows if no resulthandler is specified for that flow 
	 */
	boolean isDefault();
	void setDefault(boolean isDefault);
}
