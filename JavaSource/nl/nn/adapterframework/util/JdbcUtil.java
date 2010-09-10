/*
 * $Log: JdbcUtil.java,v $
 * Revision 1.25  2010-09-10 11:42:44  L190409
 * improved error  handling for tableExists()
 *
 * Revision 1.24  2010/07/12 12:25:38  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved debug message
 *
 * Revision 1.23  2010/02/11 14:22:50  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added several methods for checking IBISSTORE
 *
 * Revision 1.22  2009/11/17 09:04:12  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * blobSmartGet: fixed bug for MessageLog blobs
 *
 * Revision 1.21  2009/08/04 11:31:30  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * support for CLOBs and BLOBs by name
 * additional applyParameters and displayParameters methods
 *
 * Revision 1.20  2009/06/03 14:16:37  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * fixed bug that caused a loop in QuerySender when getBlobSmart=true
 *
 * Revision 1.19  2009/03/03 14:34:41  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added putByteArrayAsBlob()
 *
 * Revision 1.18  2008/10/20 13:02:26  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * also show not compressed blobs and not serialized blobs
 *
 * Revision 1.17  2008/08/27 16:23:44  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
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
import java.util.zip.DataFormatException;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import nl.nn.adapterframework.core.IMessageWrapper;
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
	public static final String version = "$RCSfile: JdbcUtil.java,v $ $Revision: 1.25 $ $Date: 2010-09-10 11:42:44 $";

	public final static int DATABASE_GENERIC=0;
	public final static int DATABASE_ORACLE=1;

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
			String query=null;
			try {
				query="select count(*) from "+tableName;
				log.debug("create statement to check for existence of ["+tableName+"] using query ["+query+"]");
				stmt = conn.prepareStatement(query);
				log.debug("execute statement");
				ResultSet rs = stmt.executeQuery();
				log.debug("statement executed");
				rs.close();
				return true;
			} catch (SQLException e) {
				if (log.isDebugEnabled()) log.debug("exception checking for existence of ["+tableName+"] using query ["+query+"]", e);
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
		String query=null;
		try {
			query = "SELECT count(" + columnName + ") FROM " + tableName;
			stmt = conn.prepareStatement(query);

			ResultSet rs = null;
			try {
				rs = stmt.executeQuery();
				return true;
			} catch (SQLException e) {
				if (log.isDebugEnabled()) log.debug("exception checking for existence of column ["+columnName+"] in table ["+tableName+"] executing query ["+query+"]", e);
				return false;
			} finally {
				if (rs != null) {
					rs.close();
				}
			}
		} catch (SQLException e) {
			log.warn("exception checking for existence of column ["+columnName+"] in table ["+tableName+"] preparing query ["+query+"]", e);
			return false;
		} finally {
			if (stmt != null) {
				stmt.close();
			}
		}
	}

	public static boolean isTablePresent(Connection conn, int databaseType, String schemaOwner, String tableName) {
		if (databaseType==DATABASE_ORACLE) {
			String query="select count(*) from all_tables where owner='"+schemaOwner.toUpperCase()+"' and table_name='"+tableName.toUpperCase()+"'";
			try {
				if (JdbcUtil.executeIntQuery(conn, query)>=1) {
					return true;
				} else {
					return false;
				}
			} catch (Exception e) {
				log.warn("could not determine presence of table ["+tableName+"]",e);
				return false;
			}
		} else {
			log.warn("could not determine presence of table ["+tableName+"] (not an Oracle database)");
			return true;
		}
	}

	public static boolean isIndexPresent(Connection conn, int databaseType, String schemaOwner, String tableName, String indexName) {
		if (databaseType==DATABASE_ORACLE) {
			String query="select count(*) from all_indexes where owner='"+schemaOwner.toUpperCase()+"' and table_name='"+tableName.toUpperCase()+"' and index_name='"+indexName.toUpperCase()+"'";
			try {
				if (JdbcUtil.executeIntQuery(conn, query)>=1) {
					return true;
				} else {
					return false;
				}
			} catch (Exception e) {
				log.warn("could not determine presence of index ["+indexName+"] on table ["+tableName+"]",e);
				return false;
			}
		} else {
			log.warn("could not determine presence of index ["+indexName+"] on table ["+tableName+"] (not an Oracle database)");
			return true;
		}
	}

	public static boolean isSequencePresent(Connection conn, int databaseType, String schemaOwner, String sequenceName) {
		if (databaseType==DATABASE_ORACLE) {
			String query="select count(*) from all_sequences where sequence_owner='"+schemaOwner.toUpperCase()+"' and sequence_name='"+sequenceName.toUpperCase()+"'";
			try {
				if (JdbcUtil.executeIntQuery(conn, query)>=1) {
					return true;
				} else {
					return false;
				}
			} catch (Exception e) {
				log.warn("could not determine presence of sequence ["+sequenceName+"]",e);
				return false;
			}
		} else {
			log.warn("could not determine presence of sequence ["+sequenceName+"] (not an Oracle database)");
			return true;
		}
	}

	public static boolean isTableColumnPresent(Connection conn, int databaseType, String schemaOwner, String tableName, String columnName) {
		if (databaseType==DATABASE_ORACLE) {
			String query="select count(*) from all_tab_columns where owner='"+schemaOwner.toUpperCase()+"' and table_name='"+tableName.toUpperCase()+"' and column_name=?";
			try {
				if (JdbcUtil.executeIntQuery(conn, query, columnName.toUpperCase())>=1) {
					return true;	
				} else {
					return false;
				}
			} catch (Exception e) {
				log.warn("could not determine correct presence of column ["+columnName+"] of table ["+tableName+"]",e);
				return false;
			}
		} else {
			log.warn("could not determine correct presence of column ["+columnName+"] of table ["+tableName+"] (not an Oracle database)");
			return true;
		}
	}

	public static boolean isIndexColumnPresent(Connection conn, int databaseType, String schemaOwner, String tableName, String indexName, String columnName) {
		if (databaseType==DATABASE_ORACLE) {
			String query="select count(*) from all_ind_columns where index_owner='"+schemaOwner.toUpperCase()+"' and table_name='"+tableName.toUpperCase()+"' and index_name='"+indexName.toUpperCase()+"' and column_name=?";
			try {
				if (JdbcUtil.executeIntQuery(conn, query, columnName.toUpperCase())>=1) {
					return true;
				} else {
					return false;
				}
			} catch (Exception e) {
				log.warn("could not determine correct presence of column ["+columnName+"] of index ["+indexName+"] on table ["+tableName+"]",e);
				return false;
			}
		} else {
			log.warn("could not determine correct presence of column ["+columnName+"] of index ["+indexName+"] on table ["+tableName+"] (not an Oracle database)");
			return true;
		}
	}

	public static int getIndexColumnPosition(Connection conn, int databaseType, String schemaOwner, String tableName, String indexName, String columnName) {
		if (databaseType==DATABASE_ORACLE) {
			String query="select column_position from all_ind_columns where index_owner='"+schemaOwner.toUpperCase()+"' and table_name='"+tableName.toUpperCase()+"' and index_name='"+indexName.toUpperCase()+"' and column_name=?";
			try {
				return JdbcUtil.executeIntQuery(conn, query, columnName.toUpperCase());
			} catch (Exception e) {
				log.warn("could not determine correct presence of column ["+columnName+"] of index ["+indexName+"] on table ["+tableName+"]",e);
				return -1;
			}
		} else {
			log.warn("could not determine correct presence of column ["+columnName+"] of index ["+indexName+"] on table ["+tableName+"] (not an Oracle database)");
			return -1;
		}
	}

	public static int getDatabaseType(Connection conn) throws SQLException {
		DatabaseMetaData md=conn.getMetaData();
		String product=md.getDatabaseProductName();
		if ("Oracle".equals(product)) {
			log.debug("Setting databasetype to ORACLE");
			return DATABASE_ORACLE;
		} else {
			log.debug("Setting databasetype to GENERIC");
			return DATABASE_GENERIC;
		}
	}

	public static String getSchemaOwner(Connection conn, int databaseType) throws SQLException, JdbcException  {
		if (databaseType==DATABASE_ORACLE) {
			String query="SELECT SYS_CONTEXT('USERENV','CURRENT_SCHEMA') FROM DUAL";
			return executeStringQuery(conn, query);
		} else {
			log.warn("could not determine current schema (not an Oracle database)");
			return "";
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
		return getBlobInputStream(rs.getBlob(columnIndex),columnIndex+"");
	}
	public static InputStream getBlobInputStream(ResultSet rs, String columnName) throws SQLException, JdbcException {
		return getBlobInputStream(rs.getBlob(columnName),columnName);
	}
	public static InputStream getBlobInputStream(Blob blob, String column) throws SQLException, JdbcException {
		if (blob==null) {
			throw new JdbcException("no blob found in column ["+column+"]");
		}
		return blob.getBinaryStream();
	}

	public static InputStream getBlobInputStream(ResultSet rs, int columnIndex, boolean blobIsCompressed) throws SQLException, JdbcException {
		return getBlobInputStream(rs.getBlob(columnIndex),columnIndex+"",blobIsCompressed);
	}
	public static InputStream getBlobInputStream(ResultSet rs, String columnName, boolean blobIsCompressed) throws SQLException, JdbcException {
		return getBlobInputStream(rs.getBlob(columnName),columnName,blobIsCompressed);
	}
	public static InputStream getBlobInputStream(Blob blob, String column, boolean blobIsCompressed) throws SQLException, JdbcException {
		InputStream input = getBlobInputStream(blob,column);
		if (blobIsCompressed) {
			return new InflaterInputStream(input);
		} else {
			return input;
		}
	}

	public static Reader getBlobReader(final ResultSet rs, int columnIndex, String charset, boolean blobIsCompressed) throws IOException, JdbcException, SQLException {
		return getBlobReader(rs.getBlob(columnIndex),columnIndex+"",charset,blobIsCompressed);
	}
	public static Reader getBlobReader(final ResultSet rs, String columnName, String charset, boolean blobIsCompressed) throws IOException, JdbcException, SQLException {
		return getBlobReader(rs.getBlob(columnName),columnName,charset,blobIsCompressed);
	}
	public static Reader getBlobReader(Blob blob, String column, String charset, boolean blobIsCompressed) throws IOException, JdbcException, SQLException {
		Reader result;
		InputStream input = getBlobInputStream(blob,column);
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
		return getBlobAsString(rs, columnIndex, charset, xmlEncode, blobIsCompressed, false);
	}

	public static String getBlobAsString(final ResultSet rs, int columnIndex, String charset, boolean xmlEncode, boolean blobIsCompressed, boolean blobSmartGet) throws IOException, JdbcException, SQLException {
		return getBlobAsString(rs.getBlob(columnIndex),columnIndex+"",charset, xmlEncode, blobIsCompressed, blobSmartGet);
	}
	public static String getBlobAsString(final ResultSet rs, String columnName, String charset, boolean xmlEncode, boolean blobIsCompressed, boolean blobSmartGet) throws IOException, JdbcException, SQLException {
		return getBlobAsString(rs.getBlob(columnName),columnName,charset, xmlEncode, blobIsCompressed, blobSmartGet);
	}
	public static String getBlobAsString(Blob blob, String column, String charset, boolean xmlEncode, boolean blobIsCompressed, boolean blobSmartGet) throws IOException, JdbcException, SQLException {
		if (blobSmartGet) {
			if (blob==null) {
				log.debug("no blob found in column ["+column+"]");
				return null;
			}
			int bl = (int)blob.length();

			InputStream is = blob.getBinaryStream();
			byte[] buf = new byte[bl];
			int bl1 = is.read(buf);

			Inflater decompressor = new Inflater();
			decompressor.setInput(buf);
			ByteArrayOutputStream bos = new ByteArrayOutputStream(buf.length);
			byte[] bufDecomp = new byte[1024];
			boolean decompresOK = true;
			while (!decompressor.finished()) {
				try {
					int count = decompressor.inflate(bufDecomp);
					if (count==0) {
						break;
					}
					bos.write(bufDecomp, 0, count);
				} catch (DataFormatException e) {
					log.debug("message in column ["+column+"] is not compressed");
					decompresOK = false;
					break;
				}
			}
			bos.close();
			if (decompresOK)
				buf = bos.toByteArray(); 

			Object result = null;
			ObjectInputStream ois = null;
			boolean objectOK = true;
			try {
				ByteArrayInputStream bis = new ByteArrayInputStream(buf);
				ois = new ObjectInputStream(bis);
				result = ois.readObject();
			} catch (Exception e) {
				log.debug("message in column ["+column+"] is probably not a serialized object: "+e.getClass().getName());
				objectOK=false;
			}
			if (ois!=null)
				ois.close();
		
			String rawMessage;
			if (objectOK) {
				if (result instanceof IMessageWrapper) {
					rawMessage = ((IMessageWrapper)result).getText();
				} else {
					rawMessage = (String)result;
				}
			} else {
				rawMessage = new String(buf,charset);
			}

			String message = XmlUtils.encodeCdataString(rawMessage);
			return message;
		} else {
			return Misc.readerToString(getBlobReader(blob,column,charset,blobIsCompressed),null,xmlEncode);
		}
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

	public static void putByteArrayAsBlob(final ResultSet rs, int columnIndex, byte content[], boolean compressBlob) throws IOException, JdbcException, SQLException {
		if (content!=null) {
			OutputStream out = getBlobUpdateOutputStream(rs, columnIndex);
			if (compressBlob) {
				DeflaterOutputStream dos = new DeflaterOutputStream(out);
				dos.write(content);
				dos.close();
			} else {
				out.write(content);
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
	public static Reader getClobReader(ResultSet rs, String columnName) throws SQLException, JdbcException {
		Clob clob = rs.getClob(columnName);
		if (clob==null) {
			throw new JdbcException("no clob found in column ["+columnName+"]");
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
	public static String getClobAsString(final ResultSet rs, String columnName, boolean xmlEncode) throws IOException, JdbcException, SQLException {
		Reader reader = getClobReader(rs,columnName);
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


	private static String displayParameters(String param1, String param2) {
		return (param1==null?"":(" param1 ["+param1+"]"+(param2==null?"":(" param2 ["+param2+"]"))));
	}
	private static String displayParameters(int param1, String param2, String param3) {
		return " param1 ["+param1+"]"+(param2==null?"":(" param2 ["+param2+"]"+(param3==null?"":(" param3 ["+param3+"]"))));
	}
	private static String displayParameters(int param1, int param2, String param3, String param4) {
		return " param1 ["+param1+"] param2 ["+param2+"]"+(param3==null?"":(" param3 ["+param3+"]"+(param4==null?"":(" param4 ["+param4+"]"))));
	}
	private static String displayParameters(int param1, int param2, int param3, String param4, String param5) {
		return " param1 ["+param1+"] param2 ["+param2+"] param3 ["+param3+"]"+(param4==null?"":(" param4 ["+param4+"]"+(param5==null?"":(" param5 ["+param5+"]"))));
	}
	

	private static void applyParameters(PreparedStatement stmt, String param1, String param2) throws SQLException {
		if (param1!=null) {
			//if (log.isDebugEnabled()) log.debug("set"+displayParameters(param1,param2));
			stmt.setString(1,param1);
			if (param2!=null) {
				stmt.setString(2,param2);
			}
		}
	}
	private static void applyParameters(PreparedStatement stmt, int param1, String param2, String param3) throws SQLException {
		//if (log.isDebugEnabled()) log.debug("set"+displayParameters(param1,param2,param3));
		stmt.setInt(1,param1);
		if (param2!=null) {
			stmt.setString(2,param2);
			if (param3!=null) {
				stmt.setString(3,param3);
			}
		}
	}
	private static void applyParameters(PreparedStatement stmt, int param1, int param2, String param3, String param4) throws SQLException {
		// if (log.isDebugEnabled()) log.debug("set"+displayParameters(param1,param2,param3,param4));
		stmt.setInt(1,param1);
		stmt.setInt(2,param2);
		if (param3!=null) {
			stmt.setString(3,param3);
			if (param4!=null) {
				stmt.setString(4,param4);
			}
		}
	}
	private static void applyParameters(PreparedStatement stmt, int param1, int param2, int param3, String param4, String param5) throws SQLException {
		//if (log.isDebugEnabled()) log.debug("set"+displayParameters(param1,param2,param3,param4,param5));
		stmt.setInt(1,param1);
		stmt.setInt(2,param2);
		stmt.setInt(3,param3);
		if (param4!=null) {
			stmt.setString(4,param4);
			if (param5!=null) {
				stmt.setString(5,param5);
			}
		}
	}

	/**
	 * exectues query that returns a string. Returns null if no results are found. 
	 */
	public static String executeStringQuery(Connection connection, String query) throws JdbcException {
		PreparedStatement stmt = null;

		try {
			if (log.isDebugEnabled()) log.debug("prepare and execute query ["+query+"]");
			stmt = connection.prepareStatement(query);
			ResultSet rs = stmt.executeQuery();
			try {
				if (!rs.next()) {
					return null;
				}
				return rs.getString(1);
			} finally {
				rs.close();
			}
		} catch (Exception e) {
			throw new JdbcException("could not obtain value using query ["+query+"]",e);
		} finally {
			if (stmt!=null) {
				try {
					stmt.close();
				} catch (Exception e) {
					throw new JdbcException("could not close statement of query ["+query+"]",e);
				}
			}
		}
	}


	public static int executeIntQuery(Connection connection, String query) throws JdbcException {
		return executeIntQuery(connection,query,null,null);
	}
	public static int executeIntQuery(Connection connection, String query, String param) throws JdbcException {
		return executeIntQuery(connection,query,param,null);
	}

	/**
	 * exectues query that returns an integer. Returns -1 if no results are found. 
	 */
	public static int executeIntQuery(Connection connection, String query, String param1, String param2) throws JdbcException {
		PreparedStatement stmt = null;

		try {
			if (log.isDebugEnabled()) log.debug("prepare and execute query ["+query+"]"+displayParameters(param1,param2));
			stmt = connection.prepareStatement(query);
			applyParameters(stmt,param1,param2);
			ResultSet rs = stmt.executeQuery();
			try {
				if (!rs.next()) {
					return -1;
				}
				return rs.getInt(1);
			} finally {
				rs.close();
			}
		} catch (Exception e) {
			throw new JdbcException("could not obtain value using query ["+query+"]"+displayParameters(param1,param2),e);
		} finally {
			if (stmt!=null) {
				try {
					stmt.close();
				} catch (Exception e) {
					throw new JdbcException("could not close statement of query ["+query+"]"+displayParameters(param1,param2),e);
				}
			}
		}
	}

	public static int executeIntQuery(Connection connection, String query, int param) throws JdbcException {
		return executeIntQuery(connection,query,param,null,null);
	}
	public static int executeIntQuery(Connection connection, String query, int param1, String param2) throws JdbcException {
		return executeIntQuery(connection,query,param1,param2,null);
	}
	
	public static int executeIntQuery(Connection connection, String query, int param1, String param2, String param3) throws JdbcException {
		PreparedStatement stmt = null;

		try {
			if (log.isDebugEnabled()) log.debug("prepare and execute query ["+query+"]"+displayParameters(param1,param2,param3));
			stmt = connection.prepareStatement(query);
			applyParameters(stmt,param1,param2,param3);
			ResultSet rs = stmt.executeQuery();
			try {
				if (!rs.next()) {
					return -1;
				}
				return rs.getInt(1);
			} finally {
				rs.close();
			}
		} catch (Exception e) {
			throw new JdbcException("could not obtain value using query ["+query+"]"+displayParameters(param1,param2,param3),e);
		} finally {
			if (stmt!=null) {
				try {
					stmt.close();
				} catch (Exception e) {
					throw new JdbcException("could not close statement of query ["+query+"]"+displayParameters(param1,param2,param3),e);
				}
			}
		}
	}


	public static int executeIntQuery(Connection connection, String query, int param1, int param2) throws JdbcException {
		return executeIntQuery(connection,query,param1,param2,null,null);
	}
	public static int executeIntQuery(Connection connection, String query, int param1, int param2, String param3) throws JdbcException {
		return executeIntQuery(connection,query,param1,param2,param3,null);
	}
	
	public static int executeIntQuery(Connection connection, String query, int param1, int param2, String param3, String param4) throws JdbcException {
		PreparedStatement stmt = null;

		try {
			if (log.isDebugEnabled()) log.debug("prepare and execute query ["+query+"]"+displayParameters(param1,param2,param3,param4));
			stmt = connection.prepareStatement(query);
			applyParameters(stmt,param1,param2,param3,param4);
			ResultSet rs = stmt.executeQuery();
			try {
				if (!rs.next()) {
					return -1;
				}
				return rs.getInt(1);
			} finally {
				rs.close();
			}
		} catch (Exception e) {
			throw new JdbcException("could not obtain value using query ["+query+"]"+displayParameters(param1,param2,param3,param4),e);
		} finally {
			if (stmt!=null) {
				try {
					stmt.close();
				} catch (Exception e) {
					throw new JdbcException("could not close statement of query ["+query+"]"+displayParameters(param1,param2,param3,param4),e);
				}
			}
		}
	}


	public static void executeStatement(Connection connection, String query) throws JdbcException {
		executeStatement(connection,query,null,null);
	}
	public static void executeStatement(Connection connection, String query, String param) throws JdbcException {
		executeStatement(connection,query,param,null);
	}
	
	public static void executeStatement(Connection connection, String query, String param1, String param2) throws JdbcException {
		PreparedStatement stmt = null;

		try {
			if (log.isDebugEnabled()) log.debug("prepare and execute query ["+query+"]"+displayParameters(param1,param2));
			stmt = connection.prepareStatement(query);
			applyParameters(stmt,param1,param2);
			stmt.execute();
		} catch (Exception e) {
			throw new JdbcException("could not execute query ["+query+"]"+displayParameters(param1,param2),e);
		} finally {
			if (stmt!=null) {
				try {
					stmt.close();
				} catch (Exception e) {
					throw new JdbcException("could not close statement for query ["+query+"]"+displayParameters(param1,param2),e);
				}
			}
		}
	}

	public static void executeStatement(Connection connection, String query, int param) throws JdbcException {
		executeStatement(connection,query,param,null,null);
	}
	public static void executeStatement(Connection connection, String query, int param1, String param2) throws JdbcException {
		executeStatement(connection,query,param1,param2,null);
	}
	
	public static void executeStatement(Connection connection, String query, int param1, String param2, String param3) throws JdbcException {
		PreparedStatement stmt = null;

		try {
			if (log.isDebugEnabled()) log.debug("prepare and execute query ["+query+"]"+displayParameters(param1,param2,param3));
			stmt = connection.prepareStatement(query);
			applyParameters(stmt,param1,param2,param3);
			stmt.execute();
		} catch (Exception e) {
			throw new JdbcException("could not execute query ["+query+"]"+displayParameters(param1,param2,param3),e);
		} finally {
			if (stmt!=null) {
				try {
					stmt.close();
				} catch (Exception e) {
					throw new JdbcException("could not close statement for query ["+query+"]"+displayParameters(param1,param2,param3),e);
				}
			}
		}
	}

	public static void executeStatement(Connection connection, String query, int param1, int param2, String param3, String param4) throws JdbcException {
		PreparedStatement stmt = null;

		try {
			if (log.isDebugEnabled()) log.debug("prepare and execute query ["+query+"]"+displayParameters(param1,param2,param3,param4));
			stmt = connection.prepareStatement(query);
			applyParameters(stmt,param1,param2,param3,param4);
			stmt.execute();
		} catch (Exception e) {
			throw new JdbcException("could not execute query ["+query+"]"+displayParameters(param1,param2,param3,param4),e);
		} finally {
			if (stmt!=null) {
				try {
					stmt.close();
				} catch (Exception e) {
					throw new JdbcException("could not close statement for query ["+query+"]"+displayParameters(param1,param2,param3,param4),e);
				}
			}
		}
	}

	public static void executeStatement(Connection connection, String query, int param1, int param2, int param3, String param4, String param5) throws JdbcException {
		PreparedStatement stmt = null;

		try {
			if (log.isDebugEnabled()) log.debug("prepare and execute query ["+query+"]"+displayParameters(param1,param2,param3,param4,param5));
			stmt = connection.prepareStatement(query);
			applyParameters(stmt,param1,param2,param3,param4,param5);
			stmt.execute();
		} catch (Exception e) {
			throw new JdbcException("could not execute query ["+query+"]"+displayParameters(param1,param2,param3,param4,param5),e);
		} finally {
			if (stmt!=null) {
				try {
					stmt.close();
				} catch (Exception e) {
					throw new JdbcException("could not close statement for query ["+query+"]"+displayParameters(param1,param2,param3,param4,param5),e);
				}
			}
		}
	}


}
