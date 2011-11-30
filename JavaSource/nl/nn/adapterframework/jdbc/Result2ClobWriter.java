/*
 * $Log: Result2ClobWriter.java,v $
 * Revision 1.5  2011-11-30 13:51:43  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:49  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.3  2011/04/13 08:39:36  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Blob and Clob support using DbmsSupport
 *
 * Revision 1.2  2007/09/24 14:58:54  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * support for parameters
 *
 * Revision 1.1  2007/08/03 08:43:30  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first versions of Jdbc result writers
 *
 */
package nl.nn.adapterframework.jdbc;

import java.io.Writer;
import java.sql.ResultSet;

import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.jdbc.dbms.IDbmsSupport;


/**
 * {@link nl.nn.adapterframework.batch.IResulthandler Resulthandler} that writes the transformed record to a CLOB.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.jdbc.Result2ClobWriter</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>Name of the resulthandler</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setQuery(String) query}</td><td>the SQL query text</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDatasourceName(String) datasourceName}</td><td>can be configured from JmsRealm, too</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setClobColumn(int) clobColumn}</td><td>column that contains the clob to be updated</td><td>1</td></tr>
 * <tr><td>{@link #setDefault(boolean) default}</td><td>If true, this resulthandler is the default for all RecordHandlingFlow that do not have a handler specified</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPrefix(String) prefix}</td><td><i>Deprecated</i> Prefix that has to be written before record, if the record is in another block than the previous record</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSuffix(String) suffix}</td><td><i>Deprecated</i> Suffix that has to be written after the record, if the record is in another block than the next record</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setOnOpenDocument(String) onOpenDocument}</td><td>String that is written before any data of results is written</td><td>&lt;document name=&quot;#name#&quot;&gt;</td></tr>
 * <tr><td>{@link #setOnCloseDocument(String) onCloseDocument}</td><td>String that is written after all data of results is written</td><td>&lt;/document&gt;</td></tr>
 * <tr><td>{@link #setOnOpenBlock(String) onOpenBlock}</td><td>String that is written before the start of each logical block, as defined in the flow</td><td>&lt;#name#&gt;</td></tr>
 * <tr><td>{@link #setOnCloseBlock(String) onCloseBlock}</td><td>String that is written after the end of each logical block, as defined in the flow</td><td>&lt;/#name#&gt;</td></tr>
 * <tr><td>{@link #setBlockNamePattern(String) blockNamePattern}</td><td>String that is replaced by name of block or name of stream in above strings</td><td>#name#</td></tr>
 * </table>
 * <p/>
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.parameters.Parameter param}</td><td>any parameters defined on the resultHandler will be applied to the SQL statement</td></tr>
 * </table>
 * <p/>
 * 
 * @author  Gerrit van Brakel
 * @since   4.7
 * @version Id
 */
public class Result2ClobWriter extends Result2LobWriterBase {
	
	protected Object getLobHandle(IDbmsSupport dbmsSupport, ResultSet rs) throws SenderException {
		try {
			return dbmsSupport.getClobUpdateHandle(rs, querySender.getClobColumn());
		} catch (Exception e) {
			throw new SenderException(e);
		}
	}
	protected void   updateLob(IDbmsSupport dbmsSupport, Object lobHandle, ResultSet rs) throws SenderException {
		try {
			dbmsSupport.updateClob(rs, querySender.getClobColumn(), lobHandle);
		} catch (Exception e) {
			throw new SenderException(e);
		}
	}

	protected Writer getWriter(IDbmsSupport dbmsSupport, Object lobHandle, ResultSet rs) throws SenderException {
		try {
			return dbmsSupport.getClobWriter(rs,querySender.getClobColumn(), lobHandle);
		} catch (Exception e) {
			throw new SenderException(e);
		}
	}

	public void setClobColumn(int column) {
		querySender.setClobColumn(column);
	}
}
