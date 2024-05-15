package org.frankframework.management.web.spring;

import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {WebTestConfig.class, ServerStatistics.class})
public class ServerStatisticsTest extends FrankApiTestBase {
}
