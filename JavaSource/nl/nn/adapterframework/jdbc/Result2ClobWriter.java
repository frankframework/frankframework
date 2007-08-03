/*
 * $Log: Result2ClobWriter.java,v $
 * Revision 1.1  2007-08-03 08:43:30  europe\L190409
 * first versions of Jdbc result writers
 *
 */
package nl.nn.adapterframework.jdbc;

import java.io.Writer;
import java.sql.ResultSet;

import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.util.JdbcUtil;


/**
 * {@link nl.nn.adapterframework.batch.IResulthandler Resulthandler} that writes the transformed record to a CLOB.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.jdbc.Result2ClobWriter</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDefaultResultHandler(boolean) default}</td><td>If true, this resulthandler is the default for all RecordHandlingFlow that do not have a handler specified</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setQuery(String) query}</td><td>the SQL query text</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDatasourceName(String) datasourceName}</td><td>can be configured from JmsRealm, too</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setClobColumn(int) clobColumn}</td><td>column that contains the clob to be updated</td><td>1</td></tr>
 * </table>
 * </p>
 * 
 * @author  Gerrit van Brakel
 * @since   4.7
 * @version Id
 */
public class Result2ClobWriter extends Result2LobWriterBase {
	public static final String version = "$RCSfile: Result2ClobWriter.java,v $  $Revision: 1.1 $ $Date: 2007-08-03 08:43:30 $";
	
	protected Writer getWriter(ResultSet rs) throws SenderException {
		try {
			return JdbcUtil.getClobWriter(rs,querySender.getClobColumn());
		} catch (Exception e) {
			throw new SenderException(e);
		}
	}

	public void setClobColumn(int column) {
		querySender.setClobColumn(column);
	}
}
