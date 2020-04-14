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
 * It enables us to: </br>
 * - limit log message length</br>
 * - apply global masking</br>
 * - apply local (thread-wise) masking</br>
 *
 * @author Murat Kaan Meral
 */
public abstract class IbisMaskingLayout extends AbstractStringLayout {

	/**
	 * Max length for the log message to be displayed. -1 for unlimited.
	 */
	private static int maxLength = -1;

	/**
	 * Explanation to display, if number of characters exceed the max length.
	 * %d is for the number of extra characters.
	 */
	private static String moreMessage = " ...(%d more characters)";

	/**
	 * Set of regex strings to hide locally, meaning for specific threads/adapters.
	 * This is required to be set for each thread individually.
	 */
	private static ThreadLocal<Set<String>> threadLocalReplace = new ThreadLocal<>();

	/**
	 * Set of regex strings to hide globally, meaning for every thread/adapter.
	 */
	private static Set<String> globalReplace = new HashSet<>();

	protected AbstractStringLayout layout;

	protected IbisMaskingLayout(Charset charset) {
		super(charset);
	}

	/**
	 * Uses an abstract string layout (e.g. Pattern Layout or XML Layout)
	 * to serialize log events to log message. Masks those messages using
	 * global and local regex strings. Then shortens the message to max length,
	 * if necessary.
	 *
	 * @param event Event to be serialized.
	 * @return Serialized and masked event.
	 */
	@Override
	public String toSerializable(LogEvent event) {
		String result = layout.toSerializable(event);
		if (StringUtils.isNotEmpty(result)) {
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

	public static void setMaxLength(int maxLength) {
		IbisMaskingLayout.maxLength = maxLength;
	}

	public static int getMaxLength() {
		return maxLength;
	}

	public static String getMoreMessageString() {
		return moreMessage;
	}

	public static void setMoreMessageString(String moreMessage) {
		IbisMaskingLayout.moreMessage = moreMessage;
	}

	public static void addToGlobalReplace(String regex) {
		globalReplace.add(regex);
	}

	public static void removeFromGlobalReplace(String regex) {
		globalReplace.remove(regex);
	}

	public static Set<String> getGlobalReplace() {
		return globalReplace;
	}

	public static void cleanGlobalReplace() {
		globalReplace = new HashSet<>();
	}

	public static void addToThreadLocalReplace(Collection<String> collection) {
		if (threadLocalReplace.get() == null)
			createThreadLocalReplace();
		threadLocalReplace.get().addAll(collection);
	}

	/**
	 * Add regex to hide locally, meaning for specific threads/adapters.
	 * This used to be LogUtil.setThreadHideRegex(String hideRegex)
	 */
	public static void addToThreadLocalReplace(String regex) {
		if (threadLocalReplace.get() == null)
			createThreadLocalReplace();
		threadLocalReplace.get().add(regex);
	}

	/**
	 * Remove regex to hide locally, meaning for specific threads/adapters.
	 * When the last item is removed the Set will be removed as well.
	 */
	public static void removeFromThreadLocalReplace(String regex) {
		threadLocalReplace.get().remove(regex);

		if(threadLocalReplace.get().isEmpty())
			removeThreadLocalReplace();
	}

	/**
	 * Set of regex strings to hide locally, meaning for specific threads/adapters.
	 * Can return null when not used/initalized!
	 */
	public static Set<String> getThreadLocalReplace() {
		return threadLocalReplace.get();
	}

	private static void createThreadLocalReplace() {
		threadLocalReplace.set(new HashSet<>());
	}

	public static void removeThreadLocalReplace() {
		threadLocalReplace.remove();
	}
}
