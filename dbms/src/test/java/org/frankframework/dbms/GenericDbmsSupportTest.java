package org.frankframework.dbms;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;


public class GenericDbmsSupportTest {

	@Test
	public void testSplitQuery() {
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
