package nl.nn.adapterframework.testutil.junit;

import static org.junit.platform.commons.util.AnnotationUtils.findAnnotatedFields;
import static org.junit.platform.commons.util.ReflectionUtils.makeAccessible;

import java.lang.reflect.Field;
import java.util.function.Predicate;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.platform.commons.util.ExceptionUtils;
import org.junit.platform.commons.util.ReflectionUtils;

public class JUnitFileServerExtension implements BeforeAllCallback, BeforeEachCallback {

	static final Namespace NAMESPACE = Namespace.create(LocalFileSystemMock.class);
	static final String FS_INSTANCE = "FS_INSTANCE";

	/**
	 * Perform field injection for non-private, {@code static} fields (i.e.,
	 * class fields) of type {@link LocalFileServer} that are annotated
	 * with {@link LocalFileSystemMock @MockFileSystem}.
	 */
	@Override
	public void beforeAll(ExtensionContext context) {
		injectStaticFields(context, context.getRequiredTestClass());
	}

	/**
	 * Perform field injection for non-private, non-static fields (i.e.,
	 * instance fields) of type {@link LocalFileServer} that are annotated
	 * with {@link LocalFileSystemMock @MockFileSystem}.
	 */
	@Override
	public void beforeEach(ExtensionContext context) {
		context.getRequiredTestInstances()
			.getAllInstances()
			.forEach(instance -> injectInstanceFields(context, instance));
	}

	private void injectStaticFields(ExtensionContext context, Class<?> testClass) {
		injectFields(context, null, testClass, ReflectionUtils::isStatic);
	}

	private void injectInstanceFields(ExtensionContext context, Object instance) {
		if(getFileSystemBuilder(context) != null) {
			injectFields(context, instance, instance.getClass(), ReflectionUtils::isNotStatic);
		}
	}

	private void injectFields(ExtensionContext context, Object testInstance, Class<?> testClass, Predicate<Field> predicate) {
		findAnnotatedFields(testClass, LocalFileSystemMock.class, predicate).forEach(field -> {
			assertNonFinalField(field);
			assertSupportedType(field.getType());
			LocalFileSystemMock mfs = field.getAnnotation(LocalFileSystemMock.class);

			try {
				String name = testClass.getSimpleName();
				LocalFileServer fsb = new LocalFileServer(name, mfs.username(), mfs.password());
				context.getRoot().getStore(NAMESPACE).put(FS_INSTANCE, fsb);
				makeAccessible(field).set(testInstance, fsb);
			}
			catch (Throwable t) {
				ExceptionUtils.throwAsUncheckedException(t);
			}
		});
	}

	private void assertNonFinalField(Field field) {
		if (ReflectionUtils.isFinal(field)) {
			throw new ExtensionConfigurationException("@MockFileSystem field [" + field + "] must not be declared as final.");
		}
	}

	private void assertSupportedType(Class<?> type) {
		if(!LocalFileServer.class.isAssignableFrom(type)) {
			throw new ExtensionConfigurationException("Can only resolve @MockFileSystem field of type "
					+ LocalFileServer.class.getName() + " but was: " + type.getName());
		}
	}

	private LocalFileServer getFileSystemBuilder(ExtensionContext context) {
		return context.getRoot().getStore(NAMESPACE).get(FS_INSTANCE, LocalFileServer.class);
	}
}
