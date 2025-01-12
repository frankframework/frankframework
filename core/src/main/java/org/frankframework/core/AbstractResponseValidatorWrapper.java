/*
   Copyright 2024-2025 WeAreFrank!

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
package org.frankframework.core;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.ApplicationContext;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.pipes.AbstractValidator;
import org.frankframework.stream.Message;
import org.frankframework.util.Locker;

/**
 * Wrapper for the response validator. It has its own name and forwards, but delegates the actual work to the original validator.
 * It overrides the stop and start method to prevent the original validator from being started and stopped.
 */
public abstract class AbstractResponseValidatorWrapper<V extends AbstractValidator> implements IValidator {

	private @Getter @Setter String name;
	private boolean started = false;

	private final Map<String, PipeForward> forwards = new HashMap<>();

	protected V owner;

	protected AbstractResponseValidatorWrapper(V owner) {
		super();
		this.owner = owner;
		name = "ResponseValidator of " + owner.getName();
	}

	@Override
	public void configure() throws ConfigurationException {
		// Do not configure, also do not (re)configure owner
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		return owner.doPipe(message, session, true, null);
	}

	@Override
	public PipeRunResult validate(Message message, PipeLineSession session, String messageRoot) throws PipeRunException {
		return owner.doPipe(message, session, true, messageRoot);
	}

	@Override
	public int getMaxThreads() {
		return 0;
	}

	@Override
	public Map<String, PipeForward> getForwards() {
		return forwards;
	}

	@Override
	public void addForward(PipeForward forward) {
		forwards.put(forward.getName(), forward);
	}

	@Override
	public void start() {
		started = true;
	}

	@Override
	public void stop() {
		started = false;
	}

	@Override
	public boolean isRunning() {
		return started;
	}

	@Override
	public ApplicationContext getApplicationContext() {
		return owner.getApplicationContext();
	}

	@Override
	public ClassLoader getConfigurationClassLoader() {
		return owner.getConfigurationClassLoader();
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		//Can ignore this as it's not set through Spring
	}

	@Override
	public boolean consumesSessionVariable(String sessionKey) {
		return owner.consumesSessionVariable(sessionKey);
	}

	@Override
	public void setPipeLine(PipeLine pipeline) {
		owner.setPipeLine(pipeline);
	}

	@Override
	public void setGetInputFromSessionKey(String string) {
		owner.setGetInputFromSessionKey(string);
	}

	@Override
	public String getGetInputFromSessionKey() {
		return owner.getGetInputFromSessionKey();
	}

	@Override
	public void setGetInputFromFixedValue(String string) {
		owner.setGetInputFromFixedValue(string);
	}

	@Override
	public String getGetInputFromFixedValue() {
		return owner.getGetInputFromFixedValue();
	}

	@Override
	public void setEmptyInputReplacement(String string) {
		owner.setEmptyInputReplacement(string);
	}

	@Override
	public String getEmptyInputReplacement() {
		return owner.getEmptyInputReplacement();
	}

	@Override
	public void setPreserveInput(boolean preserveInput) {
		owner.setPreserveInput(preserveInput);
	}

	@Override
	public boolean isPreserveInput() {
		return owner.isPreserveInput();
	}

	@Override
	public void setStoreResultInSessionKey(String string) {
		owner.setStoreResultInSessionKey(string);
	}

	@Override
	public String getStoreResultInSessionKey() {
		return owner.getStoreResultInSessionKey();
	}

	@Override
	public void setChompCharSize(String string) {
		owner.setChompCharSize(string);
	}

	@Override
	public String getChompCharSize() {
		return owner.getChompCharSize();
	}

	@Override
	public void setElementToMove(String string) {
		owner.setElementToMove(string);
	}

	@Override
	public String getElementToMove() {
		return owner.getElementToMove();
	}

	@Override
	public void setElementToMoveSessionKey(String string) {
		owner.setElementToMoveSessionKey(string);
	}

	@Override
	public String getElementToMoveSessionKey() {
		return owner.getElementToMoveSessionKey();
	}

	@Override
	public void setElementToMoveChain(String string) {
		owner.setElementToMoveChain(string);
	}

	@Override
	public String getElementToMoveChain() {
		return owner.getElementToMoveChain();
	}

	@Override
	public void setRemoveCompactMsgNamespaces(boolean b) {
		owner.setRemoveCompactMsgNamespaces(b);
	}

	@Override
	public boolean isRemoveCompactMsgNamespaces() {
		return owner.isRemoveCompactMsgNamespaces();
	}

	@Override
	public void setRestoreMovedElements(boolean restoreMovedElements) {
		owner.setRestoreMovedElements(restoreMovedElements);
	}

	@Override
	public boolean isRestoreMovedElements() {
		return owner.isRestoreMovedElements();
	}

	@Override
	public void setDurationThreshold(long maxDuration) {
		owner.setDurationThreshold(maxDuration);
	}

	@Override
	public long getDurationThreshold() {
		return owner.getDurationThreshold();
	}

	@Override
	public void setLocker(Locker locker) {
		owner.setLocker(locker);
	}

	@Override
	public Locker getLocker() {
		return owner.getLocker();
	}

	@Override
	public void setWriteToSecLog(boolean b) {
		owner.setWriteToSecLog(b);
	}

	@Override
	public boolean isWriteToSecLog() {
		return owner.isWriteToSecLog();
	}

	@Override
	public void setSecLogSessionKeys(String string) {
		owner.setSecLogSessionKeys(string);
	}

	@Override
	public String getSecLogSessionKeys() {
		return owner.getSecLogSessionKeys();
	}

	@Override
	public void registerEvent(String description) {
		owner.registerEvent(description);
	}

	@Override
	public void throwEvent(String event, Message eventMessage) {
		owner.throwEvent(event, eventMessage);
	}

	@Override
	public boolean sizeStatisticsEnabled() {
		return owner.sizeStatisticsEnabled();
	}

	@Override
	public void setHideRegex(String hideRegex) {
		owner.setHideRegex(hideRegex);
	}

	@Override
	public String getHideRegex() {
		return owner.getHideRegex();
	}

	@Override
	public void setLogIntermediaryResults(String string) {
		owner.setLogIntermediaryResults(string);
	}

	@Override
	public String getLogIntermediaryResults() {
		return owner.getLogIntermediaryResults();
	}
}
