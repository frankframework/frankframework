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
package org.frankframework.core;

import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.frankframework.doc.Label;

@Target(TYPE)
@Label(name = "TYPE")
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface DestinationType {

	Type value();

	enum Type {
		HTTP, MQTT, JVM, ADAPTER, CMIS, KAFKA, IDIN, JDBC, JMS, MONGODB, MAIL, SAP, FILE_SYSTEM;
	}
}
