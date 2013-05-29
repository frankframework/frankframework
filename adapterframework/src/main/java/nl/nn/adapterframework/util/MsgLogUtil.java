/*
   Copyright 2013 Nationale-Nederlanden

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
package nl.nn.adapterframework.util;

/**
 * Utility functions for message logging.
 * 
 * @author  Peter Leeuwenburgh
 * @version $Id$
 */

public class MsgLogUtil {
	public static final String MSGLOG_LEVEL_BY_DEFAULT_KEY = "msg.log.level.default";

	public static final String MSGLOG_LEVEL_NONE_STR = "None";
	public static final String MSGLOG_LEVEL_TERSE_STR = "Terse";
	public static final String MSGLOG_LEVEL_BASIC_STR = "Basic";
	public static final String MSGLOG_LEVEL_FULL_STR = "Full";

	public static final int MSGLOG_LEVEL_NONE = 0;
	public static final int MSGLOG_LEVEL_TERSE = 1;
	public static final int MSGLOG_LEVEL_BASIC = 2;
	public static final int MSGLOG_LEVEL_FULL = 3;

	private static int msgLogLevelByDefault = -1;

	public static final String msgLogLevels[] =
		{
			MSGLOG_LEVEL_NONE_STR,
			MSGLOG_LEVEL_TERSE_STR,
			MSGLOG_LEVEL_BASIC_STR,
			MSGLOG_LEVEL_FULL_STR };

	public static final int msgLogLevelNums[] =
		{
			MSGLOG_LEVEL_NONE,
			MSGLOG_LEVEL_TERSE,
			MSGLOG_LEVEL_BASIC,
			MSGLOG_LEVEL_FULL };

	public static synchronized int getMsgLogLevelByDefault() {
		if (msgLogLevelByDefault<0) {
			String msgLogLevelByDefaultString=AppConstants.getInstance().getString(MSGLOG_LEVEL_BY_DEFAULT_KEY, MSGLOG_LEVEL_NONE_STR);
			msgLogLevelByDefault = getMsgLogLevelNum(msgLogLevelByDefaultString);
		}
		return msgLogLevelByDefault;
	}

	public static int getMsgLogLevelNum(String msgLogLevel) {
		int i = msgLogLevels.length - 1;
		while (i >= 0 && !msgLogLevels[i].equalsIgnoreCase(msgLogLevel))
			i--; // try next
		if (i >= 0) {
			return msgLogLevelNums[i];
		} else {
			return i;
		}
	}

	public static String getMsgLogLevelString(int msgLogLevel) {
		int i = msgLogLevelNums.length - 1;
		while (i >= 0 && msgLogLevelNums[i] != msgLogLevel)
			i--; // try next
		if (i >= 0) {
			return msgLogLevels[i];
		} else {
			return "UnknownMsgLogLevel:" + msgLogLevel;
		}
	}
}
