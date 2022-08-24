package nl.nn.adapterframework.jta.narayana;

import java.util.Properties;

import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;

import com.arjuna.ats.arjuna.common.arjPropertyManager;
import com.arjuna.ats.arjuna.coordinator.TransactionReaper;
import com.arjuna.ats.arjuna.coordinator.TxControl;
import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionManagerImple;

import nl.nn.adapterframework.jta.StatusRecordingTransactionManagerImplementationTestBase;
import nl.nn.adapterframework.testutil.TransactionManagerType;

public class NarayanaJtaTransactionManagerTest extends StatusRecordingTransactionManagerImplementationTestBase<NarayanaJtaTransactionManager,TransactionManagerImple,UserTransaction>{

	@Override
	protected NarayanaJtaTransactionManager createTransactionManager() {
		return new NarayanaJtaTransactionManager();
	}

	@Override
	protected String getTMUID(TransactionManagerImple tm) {
		return arjPropertyManager.getCoreEnvironmentBean().getNodeIdentifier();
	}


	@BeforeClass
	public static void ensureNarayanaisNotActive() {
//		if(TransactionManagerServices.isTransactionManagerRunning()) {
			TransactionManagerType.NARAYANA.closeConfigurationContext();
//		}
//		if(TransactionManagerServices.isTransactionManagerRunning()) {
//			fail("unable to shutdown NARAYANA TransactionManager");
//		}
		TxControl.disable(true);
		TransactionReaper.terminate(false);
	}

	@AfterClass
	public static void validateNoTXIsActive() {
//		if(TransactionManagerServices.isTransactionManagerRunning()) {
//			fail("TransactionManager still running");
//		}
	}

	@Override
	protected NarayanaJtaTransactionManager setupTransactionManager() {
		NarayanaJtaTransactionManager result = super.setupTransactionManager();
		Properties props = new Properties();
		props.setProperty("JDBCEnvironmentBean.isolationLevel", "2" );
		props.setProperty("ObjectStoreEnvironmentBean.objectStoreDir", folder.getRoot().toString());
		props.setProperty("ObjectStoreEnvironmentBean.stateStore.objectStoreDir", folder.getRoot().toString());
		props.setProperty("ObjectStoreEnvironmentBean.communicationStore.objectStoreDir", folder.getRoot().toString());

		NarayanaConfigurationBean config = new NarayanaConfigurationBean();
		config.setProperties(props);
		config.afterPropertiesSet();
		return result;
	}

	@Ignore("Narayana does not keep a freshly opened transaction as PENDING")
	@Override
	public void testShutdownWithPendingTransactions() throws NotSupportedException, SystemException {
		//super.testShutdownWithPendingTransactions();
	}

}
