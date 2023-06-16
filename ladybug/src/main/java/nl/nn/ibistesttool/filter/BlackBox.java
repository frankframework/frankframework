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
package nl.nn.ibistesttool.filter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import nl.nn.testtool.Checkpoint;

/**
 * Only show senders that communicate with another system
 * 
 * @author Jaco de Groot
 */
public class BlackBox extends GrayBox {
	private Set<String> SENDERS_TO_HIDE = new HashSet<>(Arrays.asList(
			// Also in stub4testtool.xsl
			"nl.nn.adapterframework.jdbc.ResultSet2FileSender",
			"nl.nn.adapterframework.jdbc.DirectQuerySender",
			"nl.nn.adapterframework.jdbc.FixedQuerySender",
			"nl.nn.adapterframework.jdbc.XmlQuerySender",
			"nl.nn.adapterframework.senders.DelaySender",
			"nl.nn.adapterframework.senders.EchoSender",
			"nl.nn.adapterframework.senders.IbisLocalSender",
			"nl.nn.adapterframework.senders.LogSender",
			"nl.nn.adapterframework.senders.ParallelSenders",
			"nl.nn.adapterframework.senders.SenderSeries",
			"nl.nn.adapterframework.senders.SenderWrapper",
			"nl.nn.adapterframework.senders.XsltSender",
			"nl.nn.adapterframework.senders.FixedResultSender",
			"nl.nn.adapterframework.senders.JavascriptSender",
			"nl.nn.adapterframework.jdbc.MessageStoreSender",
			"nl.nn.adapterframework.senders.ReloadSender",
			"nl.nn.adapterframework.compression.ZipWriterSender",
			"nl.nn.adapterframework.senders.LocalFileSystemSender",
			// Not in stub4testtool.xsl
			"nl.nn.adapterframework.senders.XmlValidatorSender"));

	@Override
	protected boolean isSender(Checkpoint checkpoint) {
		if (checkpoint.getName() != null && checkpoint.getName().startsWith("Sender ")
				&& !SENDERS_TO_HIDE.contains(checkpoint.getSourceClassName())) {
			return true;
		}
		return false;
	}

	@Override
	protected boolean isSenderOrPipeline(Checkpoint checkpoint) {
		return isSender(checkpoint);
	}

}
