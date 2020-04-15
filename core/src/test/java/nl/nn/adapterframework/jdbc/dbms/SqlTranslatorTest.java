package nl.nn.adapterframework.jdbc.dbms;

import edu.emory.mathcs.backport.java.util.Arrays;
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


	@Parameterized.Parameters(name = "{index} - {0} -> {1}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][]{
				{"oracle", "mssql", "SELECT COUNT(*) FROM IBISSTORE", "SELECT COUNT(*) FROM IBISSTORE"}
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