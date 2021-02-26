/*
   Copyright 2020 WeAreFrank!

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
package nl.nn.adapterframework.pipes;

import java.io.IOException;
import java.io.Writer;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.IBlockEnabledSender;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * IteratingPipe that has Strings as items.
 * 
 * @author Gerrit van Brakel
 */
public class StringIteratorPipe extends IteratingPipe<String> {

	private int blockSize=0;
	private int startPosition=-1;
	private int endPosition=-1;
	private boolean combineBlocks=true;

	private String blockPrefix="<block>";
	private String blockSuffix="</block>";
	private String linePrefix="";
	private String lineSuffix="";

	private boolean processInBlocksBySize=false;
	private boolean processInBlocksByKey=false;
	private boolean escapeXml=false;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		processInBlocksBySize = getBlockSize()>0;
		processInBlocksByKey = getStartPosition()>=0 && getEndPosition()>getStartPosition();
		if ((processInBlocksBySize || processInBlocksByKey) && !isCombineBlocks() && !(getSender() instanceof IBlockEnabledSender)) {
			ConfigurationWarnings.add(this, log, "configured to process in blocks, but combineBlocks=false, and sender is not Block Enabled. There will be no block behaviour effectively");
		}
	}

	
	@Override
	protected IteratingPipe<String>.ItemCallback createItemCallBack(IPipeLineSession session, ISender sender, Writer writer) {
		return new ItemCallback(session, sender, writer) {

			private int itemCounter=0;
			private int totalItems=0;
			private boolean blockOpen=false;
			private StringBuffer items = new StringBuffer();
			private String previousKey=null;
			private boolean processingInBlocks=false;
			
			@Override
			public void endIterating() throws SenderException, TimeOutException, IOException {
				endBlock();
				super.endIterating();
			}
			@Override
			public void startBlock() throws SenderException, TimeOutException, IOException {
				blockOpen=true;
				processingInBlocks=true;
				if (isCombineBlocks()) {
					items.append(getBlockPrefix());
				} else {
					super.startBlock();
				}
			}
			@Override
			public boolean endBlock() throws SenderException, TimeOutException, IOException {
				if (blockOpen) {
					if (isCombineBlocks()) {
						items.append(getBlockSuffix());
						super.startBlock();
						boolean result=false;
						try {
							result = super.handleItem(items.toString());
							blockOpen=false; // must not be set before super.handleItem(), because that calls open if blockSize>0 and it has not seen items yet
						} finally {
							result &= super.endBlock();
						}
						items.setLength(0);
						itemCounter=0;
						return result;
					} else {
						blockOpen=false;
						return super.endBlock();
					}
				}
				blockOpen=false;
				return true;
			}
			
			@Override
			public boolean handleItem(String item) throws SenderException, TimeOutException, IOException {
				if (processInBlocksBySize && itemCounter==0) {
					startBlock();
				} 
				if (processInBlocksByKey) {
					String key = getKey(item);
					if (!key.equals(previousKey)) { 
						if (previousKey!=null && !endBlock()) {
							return false;
						}
						startBlock();
						previousKey=key;
					}
				}
				String itemInEnvelope = getLinePrefix()+(isEscapeXml()?XmlUtils.encodeChars(item):item)+getLineSuffix();
				boolean result = true;
				if (processingInBlocks && isCombineBlocks()) {
					items.append(itemInEnvelope);
				} else {
					result = super.handleItem(itemInEnvelope);
				}
				if (processInBlocksBySize && ++itemCounter>=getBlockSize()) {
					result &= endBlock();
					itemCounter=0;
				}
				if (getMaxItems()>0 && ++totalItems>=getMaxItems()) {
					log.debug(getLogPrefix(session)+"count ["+totalItems+"] reached maxItems ["+getMaxItems()+"], stopping loop");
					return false;
				}
				return result;
			}
		};
	}

	protected String getKey(String item) {
		if (getEndPosition() >= item.length()) {
			return item.substring(getStartPosition());
		} else {
			return item.substring(getStartPosition(), getEndPosition());
		}
	}

	@Override
	@IbisDoc({"1", "Controls multiline behaviour. If set to a value greater than 0, it specifies the number of rows send in a block to the sender.", "0 (one line at a time, no prefix of suffix)"})
	public void setBlockSize(int i) {
		blockSize = i;
	}
	@Override
	public int getBlockSize() {
		return blockSize;
	}

	@IbisDoc({"2", "If <code>startPosition &gt;= 0</code>, this field contains the start position of the key in the current record (first character is 0); " + 
			"A sequence of lines with the same key is put in one block and send to the sender. Cannot be used in combination with blockSize.", "-1"})
	public void setStartPosition(int i) {
		startPosition = i;
	}
	public int getStartPosition() {
		return startPosition;
	}

	@IbisDoc({"3", "If <code>endPosition &gt;= startPosition</code>, this field contains the end position of the key in the current record", "-1"})
	public void setEndPosition(int i) {
		endPosition = i;
	}
	public int getEndPosition() {
		return endPosition;
	}

	@IbisDoc({"4", "If <code>true</code>, all items in a block are sent at once. If set false, items are sent individually, potentially leveraging block enabled sending capabilities of the sender", "true"})
	public void setCombineBlocks(boolean combineBlocks) {
		this.combineBlocks = combineBlocks;
	}
	public boolean isCombineBlocks() {
		return combineBlocks;
	}



	
	@IbisDoc({"5", "If <code>combineBlocks = true</code>, this string is inserted at the start of each block. Requires <code>blockSize</code> or <code>startPosition</code> and <code>endPosition</code> to be set too.", "&lt;block&gt;"})
	public void setBlockPrefix(String string) {
		blockPrefix = string;
	}
	public String getBlockPrefix() {
		return blockPrefix;
	}

	@IbisDoc({"6", "If <code>combineBlocks = true</code>, this string is inserted at the end of the set of lines. Requires <code>blockSize</code> or <code>startPosition</code> and <code>endPosition</code> to be set too.", "&lt;/block&gt;"})
	public void setBlockSuffix(String string) {
		blockSuffix = string;
	}
	public String getBlockSuffix() {
		return blockSuffix;
	}

	@IbisDoc({"7", "This string is inserted at the start of each item", ""})
	public void setLinePrefix(String string) {
		linePrefix = string;
	}
	public String getLinePrefix() {
		return linePrefix;
	}

	@IbisDoc({"8", "This string is appended at the end of each item", ""})
	public void setLineSuffix(String string) {
		lineSuffix = string;
	}
	public String getLineSuffix() {
		return lineSuffix;
	}

	@IbisDoc({"9", "Escape XML characters in each item", "false"})
	public void setEscapeXml(boolean escapeXml) {
		this.escapeXml = escapeXml;
	}
	public boolean isEscapeXml() {
		return escapeXml;
	}

}
