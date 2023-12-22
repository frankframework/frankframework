/*
   Copyright 2020 WeAreFrank!

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
package org.frankframework.configuration;

import org.apache.logging.log4j.Logger;

import org.frankframework.util.LogUtil;

public class IbisContextRunnable implements Runnable {
	private static final int IBIS_INIT_EXCEPTION_TIMEOUT = 60*1000;
	private final Logger log = LogUtil.getLogger(this);
	private IbisContext ibisContext;

	public IbisContextRunnable(IbisContext ibisContext) {
		this.ibisContext = ibisContext;
	}

	@Override
	public void run() {
		try {
			log.trace("Starting IbisContextRunnable thread...");
			Thread.sleep(IBIS_INIT_EXCEPTION_TIMEOUT);
			if(ibisContext.getIbisManager() != null) {
				log.debug("Tried to initialize the ibisContext but has already been initialized, cancelling...");
			}
			else {
				ibisContext.init();
			}
		} catch (InterruptedException e) {
			log.warn("Interrupted IbisContextRunnable");
		}
	}

	public void setIbisContext(IbisContext ibisContext) {
		this.ibisContext = ibisContext;
	}
}
