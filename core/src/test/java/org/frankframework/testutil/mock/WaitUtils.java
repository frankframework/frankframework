package org.frankframework.testutil.mock;

import static org.awaitility.Awaitility.await;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;

import org.frankframework.core.IManagable;
import org.frankframework.util.LogUtil;
import org.frankframework.util.RunState;

public class WaitUtils {
	protected static final Logger LOG = LogUtil.getLogger(WaitUtils.class);

	public static void waitWhileInState(IManagable object, RunState... state) {
		Set<RunState> states = new HashSet<>();
		Collections.addAll(states, state);

		LOG.debug("Wait while runstate of [{}] (currently in [{}]) to change to not any of [{}]", object.getName(), object.getRunState(), states);
		await()
				.atMost(60, TimeUnit.SECONDS)
				.pollInterval(100, TimeUnit.MILLISECONDS)
				.until(()-> !states.contains(object.getRunState()));
	}

	public static void waitForState(IManagable object, RunState... state) {
		Set<RunState> states = new HashSet<>();
		Collections.addAll(states, state);

		LOG.debug("Wait for runstate of [{}] to go from [{}] to any of [{}]", object.getName(), object.getRunState(), states);
		await()
				.atMost(10, TimeUnit.SECONDS)
				.pollInterval(100, TimeUnit.MILLISECONDS)
				.until(() -> states.contains(object.getRunState()));
	}
}
