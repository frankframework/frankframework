package nl.nn.adapterframework.extensions.javascript;

import jdk.nashorn.api.scripting.NashornScriptEngine;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.extensions.graphviz.ResultHandler;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.LogUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;

import javax.script.Invocable;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.function.Consumer;

public class Nashorn implements JavascriptEngine<NashornScriptEngine> {

	private Logger log = LogUtil.getLogger(this);
	private NashornScriptEngine engine;
	private String alias;

	@Override
	public void setScriptAlias(String alias) {
		if (StringUtils.isEmpty(alias))
			return;
		if (engine == null) {
			this.alias = alias;
		} else {
			executeScript(alias  + " = this;");
		}
	}

	@Override
	public void startRuntime() {
		ScriptEngineManager engineManager = new ScriptEngineManager();
		engine = (NashornScriptEngine) engineManager.getEngineByName("nashorn");

		if (StringUtils.isNotEmpty(alias))
			executeScript(alias + " = this;");

		executeScript("load('classpath:net/arnx/nashorn/lib/promise.js')");
	}

	@Override
	public void executeScript(String script) {
		try {
			engine.eval(script);
		} catch (NullPointerException | ScriptException e) {
			log.error("Error executing the script[" + script + "]", e);
		}
	}

	@Override
	public Object executeFunction(String name, Object... parameters) {
		try {
			return ((Invocable) engine).invokeFunction(name, parameters);
		} catch (ScriptException | NoSuchMethodException e) {
			log.error("Error executing function [" + name + "]", e);
		}
		return null;
	}

	@Override
	public void closeRuntime() {
	}

	@Override
	public NashornScriptEngine getEngine() {
		return engine;
	}

	@Override
	public void registerCallback(ISender sender, IPipeLineSession session) {
		CallbackInterface<String, String> method = (param) -> {
			try {
				Message msg = Message.asMessage(param);
				return sender.sendMessage(msg, session).asString();
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		};
		getEngine().put(sender.getName(), method);
	}

	@Override
	public void setResultHandler(ResultHandler resultHandler) {
		getEngine().put("result", (Consumer<String>) resultHandler::setResult);
		getEngine().put("error", (Consumer<String>) resultHandler::setError);
	}

	@FunctionalInterface
	interface CallbackInterface<T, R> {
		public R sendMessage(T b);
	}
}
