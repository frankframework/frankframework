package nl.nn.adapterframework.extensions.javascript;

import nl.nn.adapterframework.core.IbisException;

public class JavascriptException extends IbisException {

	private static final long serialVersionUID = 5556131300669949855L;

	public JavascriptException(String msg) {
		super(msg);
	}

	public JavascriptException(Throwable e) {
		super(e);
	}

	public JavascriptException(String msg, Throwable th) {
		super(msg,th);
	}
}
