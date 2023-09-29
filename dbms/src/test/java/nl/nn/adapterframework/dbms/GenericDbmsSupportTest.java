package nl.nn.adapterframework.dbms;

import static org.junit.Assert.assertEquals;

import java.sql.SQLException;
import java.util.List;

import org.junit.Test;

public class GenericDbmsSupportTest {

	@Test
	public void testSplitQuery() throws JdbcException, SQLException {
		String query1 = "select count(*) from ibisstore;";
		String query2 = "delete from temp where tvarchar='t;st';";
		String query3 = "update temp set tvarchar='new' where tvarchar2='old';";
		String query4 = "ooo BEGIN BEGIN ooo; IF (ooo) THEN ooo; END IF; ooo; IF (ooo) THEN ooo; END IF; END;END;";
		List<String> result = (new GenericDbmsSupport()).splitQuery(query1 + query2 + query3 + query4);
		assertEquals(4, result.size());
		assertEquals(query1, result.get(0));
		assertEquals(query2, result.get(1));
		assertEquals(query3, result.get(2));
		assertEquals(query4, result.get(3));
	}

}
