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
package nl.nn.adapterframework.processors;

import java.sql.SQLException;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.util.Locker;

/**
 * @author Jaco de Groot
 */
public class LockerPipeLineProcessor extends PipeLineProcessorBase {

	public PipeLineResult processPipeLine(PipeLine pipeLine, String messageId, String message, IPipeLineSession pipeLineSession, String firstPipe) throws PipeRunException {
		PipeLineResult pipeLineResult;
		Locker locker = pipeLine.getLocker();
		String objectId = null;
		if (locker != null) {
			try {
				objectId = locker.lock();
			} catch (Exception e) {
				boolean isUniqueConstraintViolation = false;
				if (e instanceof SQLException) {
					SQLException sqle = (SQLException) e;
					isUniqueConstraintViolation = locker.getDbmsSupport().isUniqueConstraintViolation(sqle);
				}
				if (isUniqueConstraintViolation) {
					String msg = "error while setting lock: " + e.getMessage();
					log.info(msg);
				} else {
					throw new PipeRunException(null, "error while setting lock", e);
				}
			}
			if (objectId != null) {
				try {
					pipeLineResult = pipeLineProcessor.processPipeLine(pipeLine, messageId, message, pipeLineSession, firstPipe);
				} finally {
					try {
						locker.unlock(objectId);
					} catch (Exception e) {
						//throw new PipeRunException(null, "error while removing lock", e);
						String msg = "error while removing lock: " + e.getMessage();
						log.warn(msg);
					}
				}
			} else {
				pipeLineResult = new PipeLineResult();
				pipeLineResult.setState("success");
			}
		} else {
			pipeLineResult = pipeLineProcessor.processPipeLine(pipeLine, messageId, message, pipeLineSession, firstPipe);
		}
		return pipeLineResult;
	}
}
