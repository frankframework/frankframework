/*
   Copyright 2025 WeAreFrank

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

/**
 * Represents a field from the message browser item.
 * @param fieldName Configured database table column name.
 * @param property The property that represents this field in the Message objects retrieved by the FF! Console.
 * @param displayName Column name shown in the table in the FF! Console.
 * @param type Type of data used to display dates or other data in a human readable format, e.g. "string" or "date".
 */
public record MessageBrowserField(String fieldName, String property, String displayName, String type) { }
