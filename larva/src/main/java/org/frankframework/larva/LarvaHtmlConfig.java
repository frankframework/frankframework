package org.frankframework.larva;

import jakarta.servlet.http.HttpServletRequest;

import lombok.Getter;
import lombok.Setter;

public class LarvaHtmlConfig extends LarvaConfig {
	public static final String REQUEST_PARAM_LOG_LEVEL = "loglevel";
	public static final String REQUEST_PARAM_AUTO_SCROLL = "autoscroll";
	public static final String REQUEST_PARAM_MULTI_THREADED = "multithreaded";
	public static final String REQUEST_PARAM_SCENARIOS_ROOT_DIR = "scenariosrootdirectory";
	public static final String REQUEST_PARAM_EXECUTE = "execute";
	public static final String REQUEST_PARAM_TIMEOUT = "timeout";
	public static final String REQUEST_PARAM_WAIT_BEFORE_CLEANUP = "waitbeforecleanup";


	private @Getter @Setter boolean autoScroll = true;
	private @Getter @Setter boolean useHtmlBuffer = false;
	private @Getter @Setter boolean useLogBuffer = true;
	private @Getter @Setter String execute;

	public LarvaHtmlConfig(HttpServletRequest request) {
		String paramLogLevel = request.getParameter(REQUEST_PARAM_LOG_LEVEL);
		String paramAutoScroll = request.getParameter(REQUEST_PARAM_AUTO_SCROLL);
		String paramMultiThreaded = request.getParameter(REQUEST_PARAM_MULTI_THREADED);
		String paramWaitBeforeCleanUp = request.getParameter(REQUEST_PARAM_WAIT_BEFORE_CLEANUP);
		String paramTimeout = request.getParameter(REQUEST_PARAM_TIMEOUT);

		String paramExecute = request.getParameter(REQUEST_PARAM_EXECUTE);
		String paramScenariosRootDirectory = request.getParameter(REQUEST_PARAM_SCENARIOS_ROOT_DIR);

		setLogLevel(LarvaLogLevel.parse(paramLogLevel, LarvaLogLevel.WRONG_PIPELINE_MESSAGES));
		setAutoScroll(paramAutoScroll != null || paramLogLevel == null);
		setMultiThreaded(paramMultiThreaded != null || paramLogLevel == null);
		if (paramWaitBeforeCleanUp != null) {
			setWaitBeforeCleanup(Integer.parseInt(paramWaitBeforeCleanUp));
		}
		if (paramTimeout != null) {
			setTimeout(Integer.parseInt(paramTimeout));
		}
		setExecute(paramExecute);
		setActiveScenariosDirectory(paramScenariosRootDirectory);
	}
}
