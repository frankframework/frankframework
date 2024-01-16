/*
   Copyright 2022 WeAreFrank!

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
package org.frankframework.statistics;

public interface IBasics<S> extends ItemList {
	public static final int NUM_BASIC_ITEMS=6;

	public S takeSnapshot();

	public void addValue(long value);
	public void checkMinMax(long value);


	public long getCount();
	public long getIntervalCount(S mark);
	public long getIntervalMin(S mark);
	public long getIntervalMax(S mark);
	public void updateIntervalMinMax(S mark, long value);

	public long getMax();

	public long getMin();

	public long getSum();
	public long getSumOfSquares();

	public long getIntervalSum(S mark);
	public long getIntervalSumOfSquares(S mark);

	public double getAverage();
	public double getIntervalAverage(S mark);

	public double getVariance();
	public double getIntervalVariance(S mark);

	public double getStdDev();

}
