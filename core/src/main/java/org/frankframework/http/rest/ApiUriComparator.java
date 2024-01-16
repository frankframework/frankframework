/*
Copyright 2017 Integration Partners B.V.

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
package org.frankframework.http.rest;

import java.util.Comparator;

public class ApiUriComparator implements Comparator<String> {

	@Override
	public int compare(String uri1, String uri2) {
		String uri1c = uri1.replace("*", "");
		String uri2c = uri2.replace("*", "");

		return uri1c.compareTo(uri2c);
	}
}
