package org.frankframework.testutil.junit;

import static org.apiguardian.api.API.Status.STABLE;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Provides DataSources wired through Narayana TX as Test Arguments
 *
 * @author Niels Meijer
 */
@Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@API(status = STABLE, since = "5.7")
@ArgumentsSource(NarayanaArgumentsProvider.class)
public @interface NarayanaArgumentSource {

}
