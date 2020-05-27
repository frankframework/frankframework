package nl.nn.adapterframework.extensions.javascript;

import jdk.nashorn.api.scripting.NashornScriptEngine;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.extensions.graphviz.ResultHandler;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.LogUtil;
import org.apache.logging.log4j.Logger;

import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

public class Nashorn implements JavascriptEngine<NashornScriptEngine> {

	private Logger log = LogUtil.getLogger(this);
	private NashornScriptEngine engine;
	private String alias;
	private static final ScheduledExecutorService globalScheduledThreadPool = Executors.newScheduledThreadPool(20);

	@Override
	public void setScriptAlias(String alias) {
		this.alias = alias;
	}

	@Override
	public void startRuntime() {
		ScriptEngineManager engineManager = new ScriptEngineManager();
		engine = (NashornScriptEngine) engineManager.getEngineByName("nashorn");

		ScriptContext scriptContext = engine.getContext();
		scriptContext.setAttribute("__NASHORN_POLYFILL_TIMER__", globalScheduledThreadPool, ScriptContext.ENGINE_SCOPE);
		engine.setContext(scriptContext);

		executeScript("var " + alias + " = this;");
		URL polyfill = getClass().getClassLoader().getResource("js/nashorn-polyfill/build/nashorn-polyfill.js");
		executeScript("load('" + polyfill + "')");
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
