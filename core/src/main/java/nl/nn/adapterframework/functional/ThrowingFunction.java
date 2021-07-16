package nl.nn.adapterframework.functional;

@FunctionalInterface
public interface ThrowingFunction<T, R, E extends Exception> {
	R apply(T param) throws E;
}
