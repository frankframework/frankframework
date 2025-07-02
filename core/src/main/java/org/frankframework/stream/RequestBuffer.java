/*
   Copyright 2025 WeAreFrank!

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
package org.frankframework.stream;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;

/**
 * An interface for a wrapper around an input that can be opened and read in different formats, such as InputStream or Reader, while preserving the original
 * input so that it can be re-read time and time again without having to fully consume any InputStream immediately.
 */
public interface RequestBuffer {

	InputStream asInputStream();
	Reader asReader() throws IOException;
	Reader asReader(Charset charset);

}
