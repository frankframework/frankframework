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
	 * <p>
	 *     This function is useful to disambiguate method-overloads when an array of mixed arguments should be passed all as type {@link Supplier}
	 *     to a function that takes a number of {@link Object} parameters, such as Log4J log methods (in particular the
	 *     methods where an exception is passed as last argument).
	 *     For example:
	 *     <code>
	 *         log.error("{} Error with message id [{}]", supplier(this::getLogPrefix), supply(messageId), e);
	 *     </code>
	 * </p>
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
	 * <p>
	 *     This function is useful to disambiguate method-overloads when an array of mixed arguments should be passed all as type {@link Supplier}
	 *     to a function that takes a number of {@link Object} parameters, such as Log4J log methods (in particular the
	 *     methods where an exception is passed as last argument).
	 * </p>
	 * <p>
	 *     For example:
	 *     <code>
	 *         log.error("{} Error with message id [{}]", supplier(this::getLogPrefix), supply(messageId), e);
	 *     </code>
	 *     This can also be useful when for instance a no-arguments function should be passed to a JUnit arguments
	 *     supplier for a parameterized unit test:
	 *     <code>
	 *	public static Stream<Arguments> transactionManagers() {
	 * 		return Stream.of(
	 * 			Arguments.of(supplier(ReceiverTest::buildNarayanaTransactionManager)),
	 * 			Arguments.of(supplier(ReceiverTest::buildBtmTransactionManager))
	 * 		);
	 *        }
	 *     </code>
	 * </p>
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
	 * <p>
	 *     This function is useful when a single-argument function needs to be passed to a method
	 *     where the compiler cannot determine correct argument types due to method overloads of
	 *     methods that take generic object parameters, such as JUnit {@code Arguments.of()}.
	 * </p>
	 * <p>For Example:</p>
	 * <code>
	 *  public static Stream<Arguments> transactionManagers() {
	 * 		return Stream.of(
	 * 			Arguments.of(function(ReceiverTest::buildNarayanaTransactionManager)),
	 * 			Arguments.of(function(ReceiverTest::buildBtmTransactionManager))
	 * 		);
	 *  }
	 * </code>
	 *
	 * @param f Lambda or function reference that takes a single parameter and returns a value.
	 * @return Same lambda or function reference but now type verified by the compiler.
	 * @param <T> Type of the argument to the function parameter.
	 * @param <R> Return type of the function parameter.
	 */
	public static <T,R> Function<T,R> function(Function<T, R> f) {
		return f;
	}


	private FunctionalUtil() {
		// No-op
	}
}
