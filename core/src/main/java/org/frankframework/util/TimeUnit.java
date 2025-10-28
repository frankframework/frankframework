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
package org.frankframework.util;

import java.time.temporal.ChronoUnit;

/**
 * Unit of time for fields in the Configuration that allow specifying the unit of time explicitly.
 */
public enum TimeUnit {
	SECONDS, MINUTES, HOURS, DAYS, WEEKS, MONTHS, YEARS;

	public ChronoUnit toChronoUnit(){
		return ChronoUnit.valueOf(this.name());
	}
}
