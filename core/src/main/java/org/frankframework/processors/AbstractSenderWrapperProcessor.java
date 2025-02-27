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
package org.frankframework.processors;

import org.apache.logging.log4j.Logger;

import org.frankframework.util.LogUtil;

/**
 * Baseclass for SenderWrapperProcessor.
 *
 * @author  Gerrit van Brakel
 * @since   4.11
 */
public abstract class AbstractSenderWrapperProcessor implements SenderWrapperProcessor {
	protected Logger log = LogUtil.getLogger(this);

	protected SenderWrapperProcessor senderWrapperProcessor;

	public void setSenderWrapperProcessor(SenderWrapperProcessor senderWrapperProcessor) {
		this.senderWrapperProcessor = senderWrapperProcessor;
	}

}
