/*
 * $Log: JdbcUtil.java,v $
 * Revision 1.17  2008-08-27 16:23:44  europe\L190409
 * added columnExists
 *
 * Revision 1.16  2008/06/19 15:14:14  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added inputstream and outputstream methods for blobs
 *
 * Revision 1.15  2007/09/12 09:27:36  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added warning in fullClose()
 *
 * Revision 1.14  2007/09/05 13:06:47  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * avoid NPE when putting null BLOBs and CLOBs
 *
 * Revision 1.13  2007/07/26 16:25:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added fullClose()
 *
 * Revision 1.12  2007/07/19 15:14:11  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * handle charsets of BLOB and CLOB streams correctly
 *
 * Revision 1.11  2007/02/12 14:12:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import nl.nn.adapterframework.jdbc.JdbcException;

import org.apache.log4j.Logger;

/**
 * Database-oriented utility functions.
 * 
 * @author  Gerrit van Brakel
 * @since   4.1
 * @version Id
 */
public class JdbcUtil {
	public static final String version = "$RCSfile: JdbcUtil.java,v $ $Revision: 1.17 $ $Date: 2008-08-27 16:23:44 $";
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
	
	public static boolean columnExists(Connection conn, String tableName, String columnName) throws SQLException {
		PreparedStatement stmt = null;
		try {
			String query = "SELECT count(" + columnName + ") FROM " + tableName;
			stmt = conn.prepareStatement(query);

			ResultSet rs = null;
			try {
				rs = stmt.executeQuery();
				return true;
			} catch (SQLException e) {
				return false;
			} finally {
				if (rs != null) {
					rs.close();
				}
			}
		} catch (SQLException e) {
			return false;
		} finally {
			if (stmt != null) {
				stmt.close();
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

	public static InputStream getBlobInputStream(ResultSet rs, int columnIndex, boolean blobIsCompressed) throws SQLException, JdbcException {
		InputStream input = getBlobInputStream(rs,columnIndex);
		if (blobIsCompressed) {
			return new InflaterInputStream(input);
		} else {
			return input;
		}
	}

	public static Reader getBlobReader(final ResultSet rs, int columnIndex, String charset, boolean blobIsCompressed) throws IOException, JdbcException, SQLException {
		Reader result;
		InputStream input = getBlobInputStream(rs,columnIndex);
		if (charset==null) {
			charset = Misc.DEFAULT_INPUT_STREAM_ENCODING;
		}
		if (blobIsCompressed) {
			result = new InputStreamReader(new InflaterInputStream(input), charset);
		} else {
			result = new InputStreamReader(input, charset);
		}
		return result;
	}

	public static String getBlobAsString(final ResultSet rs, int columnIndex, String charset, boolean xmlEncode, boolean blobIsCompressed) throws IOException, JdbcException, SQLException {
		return Misc.readerToString(getBlobReader(rs,columnIndex,charset,blobIsCompressed),null,xmlEncode);
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

	public static OutputStream getBlobOutputStream(final ResultSet rs, int columnIndex, boolean compressBlob) throws IOException, JdbcException, SQLException {
		OutputStream result;
		OutputStream out = getBlobUpdateOutputStream(rs, columnIndex);
		if (compressBlob) {
			result = new DeflaterOutputStream(out);
		} else {
			result = out;
		}
		return result;	
	}

	public static Writer getBlobWriter(final ResultSet rs, int columnIndex, String charset, boolean compressBlob) throws IOException, JdbcException, SQLException {
		Writer result;
		OutputStream out = getBlobUpdateOutputStream(rs, columnIndex);
		if (charset==null) {
			charset = Misc.DEFAULT_INPUT_STREAM_ENCODING;
		}
		if (compressBlob) {
			result = new BufferedWriter(new OutputStreamWriter(new DeflaterOutputStream(out),charset));
		} else {
			result = new BufferedWriter(new OutputStreamWriter(out,charset));
		}
		return result;	
	}

	public static void putStringAsBlob(final ResultSet rs, int columnIndex, String content, String charset, boolean compressBlob) throws IOException, JdbcException, SQLException {
		if (content!=null) {
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
		} else {
			log.warn("content to store in blob was null");
		}
	}
	

	public static InputStream getClobInputStream(ResultSet rs, int columnIndex) throws SQLException, JdbcException {
		Clob clob = rs.getClob(columnIndex);
		if (clob==null) {
			throw new JdbcException("no clob found in column ["+columnIndex+"]");
		}
		return clob.getAsciiStream();
	}

	public static Reader getClobReader(ResultSet rs, int columnIndex) throws SQLException, JdbcException {
		Clob clob = rs.getClob(columnIndex);
		if (clob==null) {
			throw new JdbcException("no clob found in column ["+columnIndex+"]");
		}
		return clob.getCharacterStream();
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

	public static Writer getClobWriter(ResultSet rs, int columnIndex) throws SQLException, JdbcException {
		Clob clob = rs.getClob(columnIndex);
		if (clob==null) {
			throw new JdbcException("no clob found in column ["+columnIndex+"]");
		}
		return clob.setCharacterStream(1L);
	}

	public static String getClobAsString(final ResultSet rs, int columnIndex, boolean xmlEncode) throws IOException, JdbcException, SQLException {
		Reader reader = getClobReader(rs,columnIndex);
		return Misc.readerToString(reader, null, xmlEncode);
	}

	public static void putStringAsClob(final ResultSet rs, int columnIndex, String content) throws IOException, JdbcException, SQLException {
		if (content!=null) {
			Writer writer = getClobWriter(rs, columnIndex);
			writer.write(content);
			writer.close();
		} else {
			log.warn("content to store in blob was null");
		}
	}

	public static void fullClose(ResultSet rs) {
		Statement statement=null;
		Connection connection=null;
		
		if (rs==null) {
			log.warn("resultset to close was null");
			return;		
		}
		try {
			statement = rs.getStatement();
			connection = statement.getConnection();
		} catch (SQLException e) {
			log.warn("Could not obtain statement or connection from resultset",e);
		} finally {
			try {
				rs.close();
			} catch (SQLException e) {
				log.warn("Could not close resultset", e);
			} finally {
				if (statement!=null) {
					try {
						statement.close();
					} catch (SQLException e) {
						log.warn("Could not close statement", e);
					} finally {
						if (connection!=null) {
							try {
								connection.close();
							} catch (SQLException e) {
								log.warn("Could not close connection", e);
							}
						}
					}
				}
			}
		}
	}

	public static void fullClose(Statement statement) {
		Connection connection=null;
				
		try {
			connection = statement.getConnection();
		} catch (SQLException e) {
			log.warn("Could not obtain connection from statement",e);
		} finally {
			try {
				statement.close();
			} catch (SQLException e) {
				log.warn("Could not close statement", e);
			} finally {
				if (connection!=null) {
					try {
						connection.close();
					} catch (SQLException e) {
						log.warn("Could not close connection", e);
					}
				}
			}
		}
	}

}
