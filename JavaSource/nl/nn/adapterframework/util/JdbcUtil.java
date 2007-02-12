/*
 * $Log: JdbcUtil.java,v $
 * Revision 1.11  2007-02-12 14:12:03  europe\L190409
 * Logger from LogUtil
 *
 * Revision 1.10  2006/12/13 16:33:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added blobCharset attribute
 *
 * Revision 1.9  2005/12/29 15:34:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added support for clobs
 *
 * Revision 1.8  2005/10/19 11:37:48  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed cause from warning, due to ' unresolved compilation problems'
 *
 * Revision 1.7  2005/10/17 11:25:35  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added code to handel blobs and warnings
 *
 * Revision 1.6  2005/08/24 15:55:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added getBlobInputStream()
 *
 * Revision 1.5  2005/08/18 13:37:22  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected version String
 *
 * Revision 1.4  2005/08/18 13:36:09  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * rework using prepared statement
 * close() finally
 *
 * Revision 1.3  2004/03/26 10:42:42  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 * Revision 1.2  2004/03/25 13:36:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * table exists via count(*) rather then via metadata
 *
 * Revision 1.1  2004/03/23 17:16:14  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * initial version
 *
 */
package nl.nn.adapterframework.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import nl.nn.adapterframework.jdbc.JdbcException;

import org.apache.log4j.Logger;

/**
 * Database-oriented utility functions.
 * 
 * @version Id
 * @author  Gerrit van Brakel
 * @since   4.1
 */
public class JdbcUtil {
	public static final String version = "$RCSfile: JdbcUtil.java,v $ $Revision: 1.11 $ $Date: 2007-02-12 14:12:03 $";
	protected static Logger log = LogUtil.getLogger(JdbcUtil.class);
	
	private static final boolean useMetaData=false;
	/**
	 * @return true if tableName exists in database in this connection
	 */
	public static boolean tableExists(Connection conn, String tableName ) throws SQLException {
		
		PreparedStatement stmt = null;
		if (useMetaData) {
			DatabaseMetaData dbmeta = conn.getMetaData();
			ResultSet tableset = dbmeta.getTables(null, null, tableName, null);
			return !tableset.isAfterLast();
		} else {
			try {
				String query="select count(*) from "+tableName;
				log.debug("create statement to check for existence of ["+tableName+"] using query ["+query+"]");
				stmt = conn.prepareStatement(query);
				log.debug("execute statement");
				ResultSet rs = stmt.executeQuery();
				log.debug("statement executed");
				rs.close();
				return true;
			} catch (SQLException e) {
				return false;
			} finally {
				if (stmt!=null) {
					stmt.close();
				}
			}
		}
	}

	public static String warningsToString(SQLWarning warnings) {
		XmlBuilder warningsElem = warningsToXmlBuilder(warnings);
		if (warningsElem!=null) {
			return warningsElem.toXML();
		}
		return null;
	}

	public static void warningsToXml(SQLWarning warnings, XmlBuilder parent) {
		XmlBuilder warningsElem=warningsToXmlBuilder(warnings);
		if (warningsElem!=null) {
			parent.addSubElement(warningsElem);	
		}
	}
				
	public static XmlBuilder warningsToXmlBuilder(SQLWarning warnings) {	
		if (warnings!=null) {
			XmlBuilder warningsElem = new XmlBuilder("warnings");
			while (warnings!=null) {
				XmlBuilder warningElem = new XmlBuilder("warning"); 
				warningElem.addAttribute("errorCode",""+warnings.getErrorCode());
				warningElem.addAttribute("sqlState",""+warnings.getSQLState());
				String message=warnings.getMessage();
				
				// getCause() geeft unresolvedCompilationProblem (bij Peter Leeuwenburgh?)
 				Throwable cause=warnings.getCause();
				if (cause!=null) {
					warningElem.addAttribute("cause",cause.getClass().getName());
					if (message==null) {
						message=cause.getMessage();
					} else {
						message=message+": "+cause.getMessage();
					}
				}
				
				warningElem.addAttribute("message",message);
				warningsElem.addSubElement(warningElem);
				warnings=warnings.getNextWarning();
			}
			return warningsElem;
		}
		return null;
	}

	
	public static InputStream getBlobInputStream(ResultSet rs, int columnIndex) throws SQLException, JdbcException {
		Blob blob = rs.getBlob(columnIndex);
		if (blob==null) {
			throw new JdbcException("no blob found in column ["+columnIndex+"]");
		}
		return blob.getBinaryStream();
	}

	/**
	 * retrieves an outputstream to a blob column from an updatable resultset.
	 */
	public static OutputStream getBlobUpdateOutputStream(ResultSet rs, int columnIndex) throws SQLException, JdbcException {
		Blob blob = rs.getBlob(columnIndex);
		if (blob==null) {
			throw new JdbcException("no blob found in column ["+columnIndex+"]");
		}
		return blob.setBinaryStream(1L);
	}

	public static String getBlobAsString(final ResultSet rs, int columnIndex, String charset, boolean xmlEncode, boolean blobIsCompressed) throws IOException, JdbcException, SQLException {
		InputStream input = getBlobInputStream(rs,columnIndex);
		String result;
		if (charset==null) {
			charset = Misc.DEFAULT_INPUT_STREAM_ENCODING;
		}
		if (blobIsCompressed) {
			result = Misc.streamToString(new InflaterInputStream(input), null, charset, xmlEncode);
		} else {
			result = Misc.streamToString(input, null, charset, xmlEncode);
		}
		return result;
	}

	public static void putStringAsBlob(final ResultSet rs, int columnIndex, String content, String charset, boolean compressBlob) throws IOException, JdbcException, SQLException {
		OutputStream out = getBlobUpdateOutputStream(rs, columnIndex);
		if (charset==null) {
			charset = Misc.DEFAULT_INPUT_STREAM_ENCODING;
		}
		if (compressBlob) {
			DeflaterOutputStream dos = new DeflaterOutputStream(out);
			dos.write(content.getBytes(charset));
			dos.close();
		} else {
			out.write(content.getBytes(charset));
		}
		out.close();
	}

	public static InputStream getClobInputStream(ResultSet rs, int columnIndex) throws SQLException, JdbcException {
		Clob clob = rs.getClob(columnIndex);
		if (clob==null) {
			throw new JdbcException("no clob found in column ["+columnIndex+"]");
		}
		return clob.getAsciiStream();
	}

	/**
	 * retrieves an outputstream to a clob column from an updatable resultset.
	 */
	public static OutputStream getClobUpdateOutputStream(ResultSet rs, int columnIndex) throws SQLException, JdbcException {
		Clob clob = rs.getClob(columnIndex);
		if (clob==null) {
			throw new JdbcException("no clob found in column ["+columnIndex+"]");
		}
		return clob.setAsciiStream(1L);
	}

//TODO should maybe set encoding to "US-ASCII"
	public static String getClobAsString(final ResultSet rs, int columnIndex, boolean xmlEncode) throws IOException, JdbcException, SQLException {
		InputStream input = getClobInputStream(rs,columnIndex);
		return Misc.streamToString(input, null, xmlEncode);
	}

//	TODO should maybe set encoding to "US-ASCII"
	public static void putStringAsClob(final ResultSet rs, int columnIndex, String content) throws IOException, JdbcException, SQLException {
		OutputStream out = getClobUpdateOutputStream(rs, columnIndex);
		out.write(content.getBytes());
		out.close();
	}
        

}
