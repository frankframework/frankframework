/*
   Copyright 2018 Nationale-Nederlanden

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
package org.frankframework.ibistesttool.filter;

import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.Report;
import nl.nn.testtool.filter.CheckpointMatcher;

/**
 * The FtpFileRetrieverPipe should be converted to a Sender in the Ibis
 * AdapterFramework. Untill then this class can be used as an additional
 * checkpoint matcher in views that need to display ftp senders.
 *
 * @author Jaco de Groot
 */
public class FtpFileRetrieverPipe implements CheckpointMatcher {

	public boolean match(Report report, Checkpoint checkpoint) {
		if (
				checkpoint.getName() != null
				&&
				(
					checkpoint.getName().startsWith("Pipe ")
					// Also in stub4testtool.xsl
					&& "org.frankframework.ftp.FtpFileRetrieverPipe".equals(checkpoint.getSourceClassName())
					&&
					(
						checkpoint.getType() == Checkpoint.TYPE_STARTPOINT
						|| checkpoint.getType() == Checkpoint.TYPE_ENDPOINT
						|| checkpoint.getType() == Checkpoint.TYPE_ABORTPOINT
					)
				)
			) {
			return true;
		}
		return false;
	}

}
