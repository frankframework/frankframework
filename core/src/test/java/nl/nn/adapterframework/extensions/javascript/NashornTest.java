/*
   Copyright 2020 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.extensions.javascript;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.util.flow.ResultHandler;

public class NashornTest {
	private Nashorn engine;

	@Before
	public void setUp() throws JavascriptException {
		engine = new Nashorn();
		engine.startRuntime();
	}

	@Test
	public void testGetEngine() {
		Assert.assertNotNull(engine.getEngine());
	}

	@Test
	public void testResultHandler() throws Exception {
		ResultHandler resultHandler = new ResultHandler();
		engine.setResultHandler(resultHandler);

		engine.executeScript("result('somerandomtext');");
		Assert.assertEquals("somerandomtext", resultHandler.waitFor());

		engine.executeScript("error('somerandomtext22');");
		try {
			resultHandler.waitFor();
			Assert.fail();
		} catch (Exception e) {
			Assert.assertEquals("somerandomtext22", e.getMessage());
		}
	}

	@Test
	public void testPromise() throws Exception {
		String script =
				"var promise = new Promise(function(resolve, reject) { \n" +
						"    resolve('Success') \n" +
						"}) \n" +
						"  \n" +
						"promise \n" +
						"    .then(function(successMessage) { \n" +
						"        result(successMessage); \n" +
						"    }) \n" +
						"    .catch(function(errorMessage) { \n" +
						"        error(errorMessage); \n" +
						"    }); ";

		ResultHandler resultHandler = new ResultHandler();
		engine.setResultHandler(resultHandler);
		engine.executeScript(script);
		Assert.assertEquals("Success", resultHandler.waitFor());
	}

	@Test
	public void testExecuteFunction() throws JavascriptException {
		String script = "function plusTwo(x) { return x + 2;}";
		engine.executeScript(script);
		Double out = (Double) engine.executeFunction("plusTwo", 5);
		Assert.assertEquals(7.0, out, 0.0);
	}

	@Test
	public void testExecuteFunctionNoParam() throws JavascriptException {
		String script = "function plusTwo(x) { return x + 2;}";
		engine.executeScript(script);
		Double out = (Double) engine.executeFunction("plusTwo");
		Assert.assertTrue(out.isNaN());
	}

	@Test(expected = JavascriptException.class)
	public void testExecuteFunctionUnknownFunc() throws JavascriptException {
		engine.executeFunction("plusTwo");
	}
}
