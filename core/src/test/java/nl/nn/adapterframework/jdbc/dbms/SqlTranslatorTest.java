package nl.nn.adapterframework.jdbc.dbms;

import edu.emory.mathcs.backport.java.util.Arrays;
import nl.nn.adapterframework.util.LogUtil;
import org.apache.log4j.Level;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class SqlTranslatorTest {
	private String source, target, query, expected;

	public SqlTranslatorTest(String source, String target, String query, String expected) {
		this.source = source;
		this.target = target;
		this.query = query;
		this.expected = expected;
	}

	@Parameterized.Parameters(name = "{index} - {0} -> {1} [{3}]")
	public static Collection<Object[]> data() {
		LogUtil.getRootLogger().setLevel(Level.ALL);
		return Arrays.asList(new Object[][]{
				{"oracle", "mssql", "SELECT COUNT(*) FROM IBISSTORE", "SELECT COUNT(*) FROM IBISSTORE"},
				{"oracle", "oracle", "SELECT COUNT(*) FROM IBISSTORE", "SELECT COUNT(*) FROM IBISSTORE"},
				{"oracle", null, null, "java.lang.IllegalArgumentException"},
				{"not-a-db", "mssql", null, "java.lang.IllegalArgumentException"},
				{"", "mssql", null, "java.lang.IllegalArgumentException"},
				{"oracle", "mssql", "INSERT INTO IBISTEMP (tkey,tblob1) VALUES (SEQ_IBISTEMP.NEXTVAL,EMPTY_BLOB());", "INSERT INTO IBISTEMP (tkey,tblob1) VALUES ((NEXT VALUE FOR SEQ_IBISTEMP),0x);"},
				{"oracle", "mssql", "SELECT SEQ_IBISTEMP.NEXTVAL FROM DuaL", "SELECT (NEXT VALUE FOR SEQ_IBISTEMP)"},
		});
	}

	@Test
	public void test() throws Throwable {
		try {
			SqlTranslator translator = new SqlTranslator(source, target);
			String out = translator.translate(query);

			System.out.println("IN : " + query);
			System.out.println("OUT: " + out);
			assertEquals(expected, out);
		} catch (Throwable t) {
			if (checkExceptionClass(t, expected)) {
				Assert.assertTrue(true);
			} else {
				throw t;
			}
		}
	}



	/**
	 * Recursively check if the exception thrown is equal to the exception expected.
	 * @param t Throwable to be checked.
	 * @param c Class to be checked
	 * @return True if one of the causes of the exception is the given class, false otherwise.
	 * @throws Throwable Input t when class is not found.
	 */
	private boolean checkExceptionClass(Throwable t, String c) throws Throwable {
		try {
			return checkExceptionClass(t, Class.forName(c));
		} catch (ClassNotFoundException e) {
			if (c.equalsIgnoreCase("success"))
				return false;
			throw t;
		}
	}

	/**
	 * Recursively check if the exception thrown is equal to the exception expected.
	 * @param t Throwable to be checked.
	 * @param c Class to be checked
	 * @return True if one of the causes of the exception is the given class, false otherwise.
	 */
	private boolean checkExceptionClass(Throwable t, Class c) {
		if (c.isInstance(t)) {
			return true;
		} else if (t.getCause() != null) {
			return checkExceptionClass(t.getCause(), c);
		}
		return false;
	}
}