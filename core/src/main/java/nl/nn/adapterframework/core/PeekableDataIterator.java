/*
   Copyright 2021 WeAreFrank!

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
package nl.nn.adapterframework.core;
/**
 * Wrapper for IDataIterator, that allows to peek the object that will be returned by next().
 * 
 * @author Gerrit van Brakel
 */
public class PeekableDataIterator<I> implements IDataIterator<I> {

	private final IDataIterator<I> target;
	private I peeked = null;

	public PeekableDataIterator(IDataIterator<I> target) {
		super();
		this.target = target;
	}

	@Override
	public boolean hasNext() throws SenderException {
		return peeked!=null || target.hasNext();
	}

	@Override
	public I next() throws SenderException {
		if (peeked!=null) {
			I result = peeked;
			peeked = null;
			return result;
		}

		return target.next();
	}

	/**
	 * Returns object that will be returned by {@link #next()} if present, or null if not. Can be called multiple times.
	 */
	public I peek() throws SenderException {
		if (peeked==null && hasNext()) {
			peeked=target.next();
		}
		return peeked;
	}

	@Override
	public void close() throws SenderException {
		target.close();
	}

}
