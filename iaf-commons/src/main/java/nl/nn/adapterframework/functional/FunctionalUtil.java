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
package nl.nn.adapterframework.functional;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Static utility class with helper functions for functional programming and using lambdas.
 */
public class FunctionalUtil {

	/**
	 * Helper function to create a {@link Supplier} to supply the single constant argument value.
	 *
	 * @param value Value to be supplied. NB: This should be a constant, otherwise its value is instantly
	 *              computed instead of being delayed on-demand!
	 * @return {@link Supplier} that will return the {@code value} parameter.
	 * @param <T> Type of the value to be supplied.
	 */
	public static <T> Supplier<T> supply(T value) {
		return () -> value;
	}

	/**
	 * Helper function to cast parameter as a {@link Supplier} when the compiler cannot work it
	 * out by itself.
	 *
	 * @param s Supplier lambda or function reference.
	 * @return The same Supplier but now type verified by the compiler.
	 * @param <T> Return type of the {@link Supplier} function.
	 */
	public static <T> Supplier<T> supplier(Supplier<T> s) {
		return s;
	}

	/**
	 * Helper function to cast parameter as {@link Function} when the compiler cannot work it
	 * out by itself.
	 *
	 * @param f Lambda or function reference that takes a single parameter and has return type.
	 * @return Same lambda or function reference but now type verified by the compiler.
	 * @param <T> Type of the argument to the function parameter.
	 * @param <R> Return type of the function parameter.
	 */
	public static <T,R> Function<T,R> function(Function<T, R> f) {
		return f;
	}
}
