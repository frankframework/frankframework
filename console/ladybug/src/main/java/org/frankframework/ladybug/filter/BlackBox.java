/*
   Copyright 2023 WeAreFrank!, 2018 Nationale-Nederlanden

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
package org.frankframework.ladybug.filter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import nl.nn.testtool.Checkpoint;

/**
 * Only show senders that communicate with another system
 *
 * @author Jaco de Groot
 */
public class BlackBox extends AbstractBox {
	private static final Set<String> SENDERS_TO_HIDE = new HashSet<>(Arrays.asList(
			// Also in stub4testtool.xsl
			"org.frankframework.jdbc.ResultSet2FileSender",
			"org.frankframework.jdbc.DirectQuerySender",
			"org.frankframework.jdbc.FixedQuerySender",
			"org.frankframework.jdbc.XmlQuerySender",
			"org.frankframework.senders.DelaySender",
			"org.frankframework.senders.EchoSender",
			"org.frankframework.senders.IbisLocalSender",
			"org.frankframework.senders.LogSender",
			"org.frankframework.senders.ParallelSenders",
			"org.frankframework.senders.SenderSeries",
			"org.frankframework.senders.SenderWrapper",
			"org.frankframework.senders.XsltSender",
			"org.frankframework.senders.FixedResultSender",
			"org.frankframework.senders.JavascriptSender",
			"org.frankframework.jdbc.MessageStoreSender",
			"org.frankframework.senders.ReloadSender",
			"org.frankframework.compression.ZipWriterSender",
			"org.frankframework.senders.LocalFileSystemSender",
			// Not in stub4testtool.xsl
			"org.frankframework.senders.XmlValidatorSender"));

	@Override
	protected boolean isSender(Checkpoint checkpoint) {
		return super.isSender(checkpoint) && !SENDERS_TO_HIDE.contains(checkpoint.getSourceClassName());
	}

	@Override
	protected boolean isSenderOrPipeline(Checkpoint checkpoint) {
		return isSender(checkpoint);
	}

}
