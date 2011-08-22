/*
 * $Log: SizeStatisticsKeeper.java,v $
 * Revision 1.1  2011-08-22 14:31:32  L190409
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
