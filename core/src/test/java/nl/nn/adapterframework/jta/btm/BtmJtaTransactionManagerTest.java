package nl.nn.adapterframework.jta.btm;

import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.TransactionManagerServices;
import nl.nn.adapterframework.jta.StatusRecordingTransactionManagerImplementationTestBase;
import nl.nn.adapterframework.testutil.TransactionManagerType;

public class BtmJtaTransactionManagerTest extends StatusRecordingTransactionManagerImplementationTestBase<BtmJtaTransactionManager,BitronixTransactionManager> {

	@Override
	protected BtmJtaTransactionManager createTransactionManager() {
		return new BtmJtaTransactionManager();
	}

	@Override
	protected String getTMUID(BitronixTransactionManager tm) {
		return TransactionManagerServices.getConfiguration().getServerId();
	}


	@BeforeClass
	public static void ensureBTMisNotActive() {
		if(TransactionManagerServices.isTransactionManagerRunning()) {
			TransactionManagerType.BTM.closeConfigurationContext();
		}
		if(TransactionManagerServices.isTransactionManagerRunning()) {
			fail("unable to shutdown BTM TransactionManager");
		}
	}

	@AfterClass
	public static void validateNoTXIsActive() {
		if(TransactionManagerServices.isTransactionManagerRunning()) {
			fail("TransactionManager still running");
		}
	}

	@Override
	protected BtmJtaTransactionManager setupTransactionManager() {
		TransactionManagerServices.getConfiguration().setLogPart1Filename(folder.getRoot()+"/btm-1.tlog");
		TransactionManagerServices.getConfiguration().setLogPart2Filename(folder.getRoot()+"/btm-2.tlog");
		return super.setupTransactionManager();
	}


}
