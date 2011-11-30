/*
 * $Log: SizeStatisticsKeeper.java,v $
 * Revision 1.3  2011-11-30 13:51:48  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:52  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2011/08/22 14:31:32  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * support for size statistics
 *
 */
package nl.nn.adapterframework.statistics;

/**
 * @author  Gerrit van Brakel
 * @since  
 * @version Id
 */
public class SizeStatisticsKeeper extends StatisticsKeeper {

	private static final String statConfigKey="Statistics.size.boundaries";
    public static final String DEFAULT_BOUNDARY_LIST="10000,100000,1000000";

    public SizeStatisticsKeeper(String name) {
		super(name,BigBasics.class, statConfigKey, DEFAULT_BOUNDARY_LIST);
	}

	public String getUnits() {
		return "B";
	}

}
