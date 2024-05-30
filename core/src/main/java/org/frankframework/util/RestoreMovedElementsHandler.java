package org.frankframework.util;

import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.frankframework.core.PipeLineSession;

@Log4j2
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class RestoreMovedElementsHandler {

	private static final String ME_START = "{sessionKey:";
	private static final String ME_END = "}";

	/**
	 * Restore moved elements from pipelineSession.
	 *
	 * @param inputString The input string to restore.
	 * @param pipeLineSession The session of the pipeline execution.
	 * @return The input string with moved elements restored.
	 */
	public static String process(String inputString, PipeLineSession pipeLineSession) {
		if (inputString == null || pipeLineSession == null) {
			return inputString;
		}
		int startPos = inputString.indexOf(ME_START);
		if (startPos == -1) {
			return inputString;
		}
		char[] inputChars = inputString.toCharArray();
		int copyFrom = 0;
		StringBuilder buffer = new StringBuilder();
		while (startPos != -1) {
			buffer.append(inputChars, copyFrom, startPos - copyFrom);
			int nextStartPos = inputString.indexOf(ME_START, startPos + ME_START.length());
			if (nextStartPos == -1) {
				nextStartPos = inputString.length();
			}
			int endPos = inputString.indexOf(ME_END, startPos + ME_START.length());
			if (endPos == -1 || endPos > nextStartPos) {
				log.warn("Found a start delimiter without an end delimiter while restoring from compacted result at position [{}] in [{}]", startPos, inputString);
				buffer.append(inputChars, startPos, nextStartPos - startPos);
				copyFrom = nextStartPos;
			} else {
				String movedElementSessionKey = inputString.substring(startPos + ME_START.length(),endPos);
				if (pipeLineSession.containsKey(movedElementSessionKey)) {
					String movedElementValue = pipeLineSession.getString(movedElementSessionKey);
					buffer.append(movedElementValue);
					copyFrom = endPos + ME_END.length();
				} else {
					log.warn("Did not find sessionKey [{}] while restoring from compacted result", movedElementSessionKey);
					buffer.append(inputChars, startPos, nextStartPos - startPos);
					copyFrom = nextStartPos;
				}
			}
			startPos = inputString.indexOf(ME_START, copyFrom);
		}
		buffer.append(inputChars, copyFrom, inputChars.length - copyFrom);
		return buffer.toString();
	}
}
