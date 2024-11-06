/*
   Copyright 2020-2023 WeAreFrank!

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
package org.frankframework.pipes;

import java.io.IOException;
import java.io.Writer;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.core.IBlockEnabledSender;
import org.frankframework.core.ISender;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.TimeoutException;
import org.frankframework.util.XmlEncodingUtils;

/**
 * IteratingPipe that has Strings as items.
 *
 * @author Gerrit van Brakel
 */
public abstract class StringIteratorPipe extends IteratingPipe<String> {

	private @Getter int stringIteratorPipeBlockSize=0;
	private @Getter int startPosition=-1;
	private @Getter int endPosition=-1;
	private @Getter boolean combineBlocks=true;

	private @Getter String blockPrefix="<block>";
	private @Getter String blockSuffix="</block>";
	private @Getter String linePrefix="";
	private @Getter String lineSuffix="";

	private boolean processInBlocksBySize=false;
	private boolean processInBlocksByKey=false;
	private @Getter boolean escapeXml=false;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		processInBlocksBySize = getStringIteratorPipeBlockSize()>0;
		processInBlocksByKey = getStartPosition()>=0 && getEndPosition()>getStartPosition();
		if ((processInBlocksBySize || processInBlocksByKey) && !isCombineBlocks() && !(getSender() instanceof IBlockEnabledSender)) {
			ConfigurationWarnings.add(this, log, "configured to process in blocks, but combineBlocks=false, and sender is not Block Enabled. There will be no block behaviour effectively");
		}
		if (getStringIteratorPipeBlockSize()>0) {
			super.setBlockSize(isCombineBlocks() ? 1 : getStringIteratorPipeBlockSize());
		}
	}

	@Override
	protected IteratingPipe<String>.ItemCallback createItemCallBack(PipeLineSession session, ISender sender, Writer writer) {
		return new ItemCallback(session, sender, writer) {

			private int itemCounter=0;
			private int totalItems=0;
			private final StringBuilder items = new StringBuilder();
			private String previousKey=null;
			private boolean processingInBlocks=false;

			@Override
			public void endIterating() throws SenderException, IOException, TimeoutException {
				finalizeBlock();
				super.endIterating();
			}
			@Override
			public void startBlock() throws SenderException, TimeoutException {
				processingInBlocks=true;
				super.startBlock();
				if (isCombineBlocks()) {
					items.append(getBlockPrefix());
				}
			}

			private StopReason finalizeBlock() throws SenderException, TimeoutException, IOException {
				if (processingInBlocks && isCombineBlocks() && itemCounter>0) {
					itemCounter=0;
					items.append(getBlockSuffix());
					StopReason stopReason = super.handleItem(items.toString());
					items.setLength(0);
					return stopReason;
				}
				return null;
			}

			@Override
			public StopReason handleItem(String item) throws SenderException, TimeoutException, IOException {
				if (processInBlocksBySize && itemCounter==0) {
					startBlock();
				}
				if (processInBlocksByKey) {
					String key = getKey(item);
					if (!key.equals(previousKey)) {
						StopReason stopReason = finalizeBlock();
						if(stopReason != null) {
							return stopReason;
						}
						startBlock();
						previousKey=key;
					}
				}
				String itemInEnvelope = getLinePrefix()+(isEscapeXml() ? XmlEncodingUtils.encodeChars(item) : item)+getLineSuffix();
				StopReason result = null;
				if (processingInBlocks && isCombineBlocks()) {
					items.append(itemInEnvelope);
					++itemCounter;
					if (processInBlocksBySize && itemCounter>=getStringIteratorPipeBlockSize()) {
						finalizeBlock();
					}
				} else {
					result = super.handleItem(itemInEnvelope);
				}
				if (getMaxItems()>0 && ++totalItems>=getMaxItems()) {
					log.debug("count [{}] reached maxItems [{}], stopping loop", totalItems, getMaxItems());
					return StopReason.MAX_ITEMS_REACHED;
				}
				return result;
			}
		};
	}

	protected String getKey(String item) {
		if (getEndPosition() >= item.length()) {
			return item.substring(getStartPosition());
		}
		return item.substring(getStartPosition(), getEndPosition());
	}

	/**
	 * Controls multiline behaviour. If set to a value greater than 0, it specifies the number of rows send in a block to the sender.
	 * @ff.default 0 (one line at a time, no prefix of suffix)
	 */
	@Override
	public void setBlockSize(int i) {
		stringIteratorPipeBlockSize = i;
	}

	/**
	 * If <code>startPosition &gt;= 0</code>, this field contains the start position of the key in the current record (first character is 0);
	 * A sequence of lines with the same key is put in one block and send to the sender. Cannot be used in combination with blockSize.
	 * @ff.default -1
	 */
	public void setStartPosition(int i) {
		startPosition = i;
	}

	/**
	 * If <code>endPosition &gt;= startPosition</code>, this field contains the end position of the key in the current record
	 * @ff.default -1
	 */
	public void setEndPosition(int i) {
		endPosition = i;
	}

	/**
	 * If <code>true</code>, all items in a block are sent at once. If set false, items are sent individually, potentially leveraging block enabled sending capabilities of the sender
	 * @ff.default true
	 */
	public void setCombineBlocks(boolean combineBlocks) {
		this.combineBlocks = combineBlocks;
	}

	/**
	 * If <code>combineBlocks = true</code>, this string is inserted at the start of each block. Requires <code>blockSize</code> or <code>startPosition</code> and <code>endPosition</code> to be set too.
	 * @ff.default &lt;block&gt;
	 */
	public void setBlockPrefix(String string) {
		blockPrefix = string;
	}

	/**
	 * If <code>combineBlocks = true</code>, this string is inserted at the end of the set of lines. Requires <code>blockSize</code> or <code>startPosition</code> and <code>endPosition</code> to be set too.
	 * @ff.default &lt;/block&gt;
	 */
	public void setBlockSuffix(String string) {
		blockSuffix = string;
	}

	/** This string is inserted at the start of each item */
	public void setLinePrefix(String string) {
		linePrefix = string;
	}

	/** This string is appended at the end of each item */
	public void setLineSuffix(String string) {
		lineSuffix = string;
	}

	/**
	 * Escape XML characters in each item
	 * @ff.default false
	 */
	public void setEscapeXml(boolean escapeXml) {
		this.escapeXml = escapeXml;
	}

}
