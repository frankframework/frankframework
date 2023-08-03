package nl.nn.adapterframework.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * This static class contains static methods that act as stored procedures for the H2 database, so there can be
 * unit test coverage without Docker running databases.
 */
@SuppressWarnings("unused")
public class H2TestProcedures {

	static final String INSERT_SQL = "INSERT INTO SP_TESTDATA(TMESSAGE, TCHAR) VALUES (?, ?)";
	static final String SELECT_BY_CONTENT_SQL = "SELECT * FROM SP_TESTDATA WHERE TMESSAGE = ? ORDER BY TKEY";

	public static void insertMessage(Connection conn, String message, char status) throws SQLException {
		try (PreparedStatement pstmt = conn.prepareStatement(INSERT_SQL)) {
			pstmt.setString(1, message);
			pstmt.setObject(2, status);
			pstmt.executeUpdate();
		}
	}

	public static ResultSet selectByContent(Connection conn, String message) throws SQLException {
		PreparedStatement pstmt = conn.prepareStatement(SELECT_BY_CONTENT_SQL);
		pstmt.setString(1, message);
		return pstmt.executeQuery();
	}
}
