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
/*
 * $Log: InputStreamReaderFactory.java,v $
 * Revision 1.4  2012-06-01 10:52:48  m00f069
 * Created IPipeLineSession (making it easier to write a debugger around it)
 *
 * Revision 1.3  2011/11/30 13:51:56  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:47  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2010/05/03 17:02:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * default implementation of IInputstreamReaderFactory
 *
 */
package nl.nn.adapterframework.batch;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.SenderException;

/**
 * Basic InputStreamReaderFactory.
 * 
 * @author  Gerrit van Brakel
 * @since   4.11
 * @version $Id$
 */
public class InputStreamReaderFactory implements IInputStreamReaderFactory {

	public void configure() throws ConfigurationException {
	}

	public Reader getReader(InputStream inputstream, String charset, String streamId, IPipeLineSession session) throws SenderException {
		try {
			return new InputStreamReader(inputstream, charset);
		} catch (UnsupportedEncodingException e) {
			throw new SenderException("cannot use charset ["+charset+"] to read stream ["+streamId+"]",e);
		}
	}


}
