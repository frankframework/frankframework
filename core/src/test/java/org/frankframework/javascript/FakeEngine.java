package org.frankframework.javascript;

import org.frankframework.core.ISender;
import org.frankframework.core.PipeLineSession;
import org.frankframework.util.flow.ResultHandler;
import org.frankframework.util.flow.graphviz.GraphvizEngineTest;

/**
 * Javascript Engine to do tests with, without dependencies to real engines.
 * Used in {@link GraphvizEngineTest#testJavascriptEngineIsClosedProperly}
 */
@SuppressWarnings("unused")
public class FakeEngine implements JavascriptEngine<String> {

	@Override
	public void setGlobalAlias(String alias) {
	}

	@Override
	public void startRuntime() {
	}

	@Override
	public void executeScript(String script) {
	}

	@Override
	public Object executeFunction(String name, Object... parameters) {
		return null;
	}

	@Override
	public void closeRuntime() {
	}

	@Override
	public String getEngine() {
		return "FakeEngine";
	}

	@Override
	public void registerCallback(final ISender sender, final PipeLineSession session) {
	}

	@Override
	public void setResultHandler(ResultHandler resultHandler) {
	}
}
