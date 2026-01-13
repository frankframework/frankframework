/*
   Copyright 2024-2026 WeAreFrank!

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

import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
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

	@NonNull
	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		return owner.doPipe(message, session, true, null);
	}

	@Override
	public @NonNull PipeRunResult validate(Message message, PipeLineSession session, String messageRoot) throws PipeRunException {
		return owner.doPipe(message, session, true, messageRoot);
	}

	@Override
	public int getMaxThreads() {
		return 0;
	}

	@Override
	public List<PipeForward> getRegisteredForwards() {
		// Only here because PipeLine configuration calls it, and interface demands it. There will never be any forwards owned or used by the wrapper.
		return owner.getRegisteredForwards();
	}

	@Override
	@Nullable
	public PipeForward findForward(String forward) {
		// Should not usually be called for validators, but might be called from error-handling code paths. Should then use exception forwards from original validator.
		return owner.findForward(forward);
	}

	@Override
	public void addForward(PipeForward forward) {
		// No-op, only here because PipeLine configuration calls it and interface demands it. PipeLine will always call it only to add the SUCCESS-Forward and that is already added on the owner too, and only that is actually used.
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
	public void setApplicationContext(@NonNull ApplicationContext applicationContext) {
		// Can ignore this as it's not set through Spring
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

	@SuppressWarnings("deprecation")
	@Override
	public void setEmptyInputReplacement(String string) {
		owner.setEmptyInputReplacement(string);
	}

	@Override
	public void setDefaultValue(String string) {
		owner.setDefaultValue(string);
	}

	@Override
	public String getDefaultValue() {
		return owner.getDefaultValue();
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

	@Override
	public boolean skipPipe(Message input, PipeLineSession session) throws PipeRunException {
		return owner.skipPipe(input, session);
	}

	@Override
	public void setSkipOnEmptyInput(boolean b) {
		owner.setSkipOnEmptyInput(b);
	}

	@Override
	public void setIfParam(String string) {
		owner.setIfParam(string);
	}

	@Override
	public void setIfValue(String string) {
		owner.setIfValue(string);
	}

	@Override
	public void setOnlyIfSessionKey(String onlyIfSessionKey) {
		owner.setOnlyIfSessionKey(onlyIfSessionKey);
	}

	@Override
	public void setOnlyIfValue(String onlyIfValue) {
		owner.setOnlyIfValue(onlyIfValue);
	}

	@Override
	public void setUnlessSessionKey(String unlessSessionKey) {
		owner.setUnlessSessionKey(unlessSessionKey);
	}

	@Override
	public void setUnlessValue(String unlessValue) {
		owner.setUnlessValue(unlessValue);
	}

	@Override
	public boolean isSkipOnEmptyInput() {
		return owner.isSkipOnEmptyInput();
	}

	@Override
	public String getIfParam() {
		return owner.getIfParam();
	}

	@Override
	public String getIfValue() {
		return owner.getIfValue();
	}

	@Override
	public String getOnlyIfSessionKey() {
		return owner.getOnlyIfSessionKey();
	}

	@Override
	public String getOnlyIfValue() {
		return owner.getOnlyIfValue();
	}

	@Override
	public String getUnlessSessionKey() {
		return owner.getUnlessSessionKey();
	}

	@Override
	public String getUnlessValue() {
		return owner.getUnlessValue();
	}
}
