package nl.nn.adapterframework.extensions.javascript;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ISender;

public class Nashorn implements JavascriptEngine<ScriptEngine> {

	private ScriptEngine engine;
	private String alias;

	@Override
	public void setScriptAlias(String alias) {
		this.alias = alias;
	}

	@Override
	public void startRuntime() {
		ScriptEngineManager engineManager = new ScriptEngineManager();
		engine = engineManager.getEngineByName("nashorn");
	}

	@Override
	public void executeScript(String script) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Object executeFunction(String name, Object... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void closeRuntime() {
	}

	@Override
	public ScriptEngine getEngine() {
		return engine;
	}

	@Override
	public void registerCallback(ISender sender, IPipeLineSession session) {
		// TODO Auto-generated method stub
		
	}
}
