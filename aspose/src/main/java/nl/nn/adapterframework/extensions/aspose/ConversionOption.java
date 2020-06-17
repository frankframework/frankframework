/*
   Copyright 2019 Integration Partners

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
package nl.nn.adapterframework.extensions.aspose;

public enum ConversionOption {
    SINGLEPDF(0), SEPERATEPDF(1);
    // Written as "SEPERATEPDF" instead of "SEPARATEPDF" because Aspose libary expects "SEPERATEPDF"
	//TODO should be SEPARATEPDF, aspose espects a 0 or 1 !!!

	private final int value;

	private ConversionOption(int value) {
		this.value = value;
	}

	public int getValue() {
		return this.value;
	}
}
