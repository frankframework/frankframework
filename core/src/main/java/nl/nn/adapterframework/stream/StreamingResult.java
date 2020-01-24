/*
   Copyright 2020 Integration Partners B.V.

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
package nl.nn.adapterframework.stream;

import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunResult;

public class StreamingResult extends PipeRunResult {

	private boolean resultHasBeenStreamed;
	
	public StreamingResult(boolean resultHasBeenStreamed) {
		super();
		this.resultHasBeenStreamed=resultHasBeenStreamed;
	}

	public StreamingResult(PipeForward forward, Object result, boolean resultHasBeenStreamed) {
		super(forward, result);
		this.resultHasBeenStreamed=resultHasBeenStreamed;
	}

	public boolean isResultHasBeenStreamed() {
		return resultHasBeenStreamed;
	}

//	public void setResultHasBeenStreamed(boolean resultHasBeenStreamed) {
//		this.resultHasBeenStreamed = resultHasBeenStreamed;
//	}

}
