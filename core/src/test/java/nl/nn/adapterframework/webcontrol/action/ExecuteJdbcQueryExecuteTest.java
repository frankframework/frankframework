package nl.nn.adapterframework.webcontrol.action;

import org.junit.Before;
import org.junit.Test;

public class ExecuteJdbcQueryExecuteTest {
	
	@Before
	public void setup() {
		ExecuteJdbcQueryExecute executor = new ExecuteJdbcQueryExecute();
		executor.getResult("jdbc", "queryType", "resultType", "query");
	}
}