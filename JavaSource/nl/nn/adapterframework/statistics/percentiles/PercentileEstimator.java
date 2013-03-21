/*
   Copyright 2013 Nationale-Nederlanden

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
/*
 * $Log: PercentileEstimator.java,v $
 * Revision 1.3  2011-11-30 13:52:02  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:51  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2009/12/29 14:25:18  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved statistics to separate package
 *
 * Revision 1.4  2008/08/27 16:25:16  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added clear()
 *
 * Revision 1.3  2006/09/07 08:37:51  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added sample return facility
 *
 * Revision 1.2  2005/03/10 09:52:16  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * reworked percentile estimation
 *
 * Revision 1.1  2005/02/02 16:30:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modular percentile estimation
 *
 */
package nl.nn.adapterframework.statistics.percentiles;

import nl.nn.adapterframework.util.XmlBuilder;

/**
 * Interface to define estimators for percentiles.
 * 
 * @author Gerrit van Brakel
 * @version $Id$
 */
public interface PercentileEstimator {
	
	int getNumPercentiles();
	int getPercentage(int index);
	void addValue(long value, long count, long min, long max);
	double getPercentileEstimate(int index, long count, long min, long max);

	int getSampleCount(long count, long min, long max);
	XmlBuilder getSample(int index, long count, long min, long max);
	
	public void clear();

}
