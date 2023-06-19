/*
   Copyright 2023 WeAreFrank!

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
package nl.nn.adapterframework.jta.btm;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import bitronix.tm.journal.DiskJournal;
import bitronix.tm.journal.JournalRecord;
import bitronix.tm.utils.Uid;
import nl.nn.adapterframework.util.AppConstants;

/**
 * This class exists to ensure the DiskJournal is accessible when updating the TransactionLog
 * Since it's not possible to change the current transaction, the most we can do is attempt to
 * retry the log/force actions and hope we don't break the tx-log even further.
 * 
 * @see <a href="https://github.com/ibissource/iaf/issues/4615">IbisSource issue</a>
 * @see <a href="https://github.com/bitronix/btm/issues/45">BTM issue</a>
 * 
 * @author Niels Meijer
 */
public class BtmDiskJournal extends DiskJournal {
	private Logger log = LogManager.getLogger(BtmDiskJournal.class);
	private static final String LOG_ERR_MSG = "cannot write log, disk logger is not open";
	private static final String FORCE_ERR_MSG = "cannot force log writing, disk logger is not open";
	private static final String COLLECT_ERR_MSG = "cannot collect dangling records, disk logger is not open";
	private static final AtomicInteger ERROR_COUNT = new AtomicInteger(0);
	private static final int MAX_ERROR_COUNT = AppConstants.getInstance().getInt("transactionmanager.btm.journal.maxRetries", 500);
	private static AtomicLong lastRecovery = new AtomicLong(0);

	@Override
	public void log(int status, Uid gtrid, Set<String> uniqueNames) throws IOException {
		try {
			super.log(status, gtrid, uniqueNames);
		} catch (IOException e) {
			if(e instanceof ClosedChannelException || LOG_ERR_MSG.equals(e.getMessage())) {
				recover(e);

				super.log(status, gtrid, uniqueNames);
			} else {
				throw e;
			}
		}
	}

	@Override
	public void force() throws IOException {
		try {
			super.force();
		} catch (IOException e) {
			if(e instanceof ClosedChannelException || FORCE_ERR_MSG.equals(e.getMessage())) {
				recover(e);

				super.force();
			} else {
				throw e;
			}
		}
	}

	@Override
	public Map<Uid, JournalRecord> collectDanglingRecords() throws IOException {
		try {
			return super.collectDanglingRecords();
		} catch (IOException e) {
			if(e instanceof ClosedChannelException || COLLECT_ERR_MSG.equals(e.getMessage())) {
				recover(e);

				return super.collectDanglingRecords();
			} else {
				throw e;
			}
		}
	}

	private void recover(IOException e) throws IOException {
		Long now = Instant.now().getEpochSecond();
		Long last = lastRecovery.getAndSet(now);
		if(now - last > 3600) {
			log.debug("resetting FileChannel exception count");
			ERROR_COUNT.set(0);
		}

		int errorCount = ERROR_COUNT.incrementAndGet();

		close();
		if(errorCount > MAX_ERROR_COUNT) {
			log.warn("FileChannel exception but too many retries, aborting");
			throw e;
		}

		log.error("FileChannel exception, attempt [{}] to recover DiskJournal", errorCount, e);
		open();
	}
}
