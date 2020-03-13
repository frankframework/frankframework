package nl.nn.adapterframework.extensions.log4j;

import nl.nn.adapterframework.util.Misc;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * This is a wrapper for Log4j2 layouts.
 * It enables us to:
 * limit log message length,
 * apply global masking,
 * and apply local (thread-wise) masking.
 *
 * @author Murat Kaan Meral
 */
public abstract class IbisMaskingLayout extends AbstractStringLayout {
	private static int maxLength = -1;
	private static String moreMessage = " ...(%d more characters)";
	private static ThreadLocal<Set<String>> threadLocalReplace = new ThreadLocal<>();
	private static Set<String> globalReplace = new HashSet<>();

	protected AbstractStringLayout layout;

	protected IbisMaskingLayout(Charset charset) {
		super(charset);
	}

	@Override
	public String toSerializable(LogEvent event) {
		String result = layout.toSerializable(event);
		if(StringUtils.isNotEmpty(result)) {
			result = Misc.hideAll(result, globalReplace);
			result = Misc.hideAll(result, threadLocalReplace.get());
		}
		int length = result.length();
		if (maxLength > 0 && length > maxLength) {
			int diff = length - maxLength;
			result = result.substring(0, maxLength) + " " + String.format(moreMessage, diff) + "\r\n";
		}

		return result;
	}

	public static void setMaxLength (int maxLength) {
		IbisMaskingLayout.maxLength = maxLength;
	}

	public static int getMaxLength () {
		return maxLength;
	}

	public static String getMoreMessageString() {
		return moreMessage;
	}

	public static void setMoreMessageString(String moreMessage) {
		IbisMaskingLayout.moreMessage = moreMessage;
	}

	public static void addToGlobalReplace (String regex) {
		globalReplace.add(regex);
	}

	public static void removeFromGlobalReplace (String regex) {
		globalReplace.remove(regex);
	}

	public static Set<String> getGlobalReplace () {
		return globalReplace;
	}

	public static void cleanGlobalReplace () {
		globalReplace = new HashSet<>();
	}

	public static void addToThreadLocalReplace (Collection<String> collection) {
		if(threadLocalReplace.get() == null)
			cleanThreadLocalReplace();
		threadLocalReplace.get().addAll(collection);
	}

	public static void addToThreadLocalReplace (String regex) {
		if(threadLocalReplace.get() == null)
			cleanThreadLocalReplace();
		threadLocalReplace.get().add(regex);
	}

	public static void removeFromThreadLocalReplace (String regex) {
		threadLocalReplace.get().remove(regex);
	}

	public static Set<String> getThreadLocalReplace () {
		return threadLocalReplace.get();
	}

	public static void cleanThreadLocalReplace () {
		threadLocalReplace.set(new HashSet<>());
	}
}
