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
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import bitronix.tm.journal.DiskJournal;
import bitronix.tm.journal.JournalRecord;
import bitronix.tm.utils.Uid;

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

	@Override
	public void log(int status, Uid gtrid, Set<String> uniqueNames) throws IOException {
		try {
			super.log(status, gtrid, uniqueNames);
		} catch (ClosedChannelException e) {
			recover(e);

			super.log(status, gtrid, uniqueNames);
		}
	}

	@Override
	public void force() throws IOException {
		try {
			super.force();
		} catch (ClosedChannelException e) {
			recover(e);

			super.force();
		}
	}

	@Override
	public Map<Uid, JournalRecord> collectDanglingRecords() throws IOException {
		try {
			return super.collectDanglingRecords();
		} catch (ClosedChannelException e) {
			recover(e);

			return super.collectDanglingRecords();
		}
	}

	private void recover(ClosedChannelException e) throws IOException {
		log.error("FileChannel unexpectectly closed, attempting to recover DiskJournal", e);

		close();
		open();
	}
}
