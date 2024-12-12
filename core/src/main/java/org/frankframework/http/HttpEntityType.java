/*
   Copyright 2024 WeAreFrank!

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
package org.frankframework.http;

public enum HttpEntityType {
	/**
	 * The input message is sent unchanged as character data, like text, XML or JSON, with possibly parameter data appended.
	 * When there are no parameters to be appended, the output of this option is the same as {@link #BINARY}.
	 */
	RAW, // text/html;charset=UTF8

	/**
	 * The input message is sent unchanged as binary or character data. The mimetype and character set on the HTTP entity
	 * will determine how the client interprets the data.
	 */
	BINARY, //application/octet-stream

//		SWA("Soap with Attachments"), // text/xml
	/** Yields a x-www-form-urlencoded form entity */
	URLENCODED,
	/** Yields a multipart/form-data form entity */
	FORMDATA,
	/** Yields a MTOM multipart/related form entity */
	MTOM
}
