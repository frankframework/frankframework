package org.frankframework.management.security;


import java.util.Objects;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;

import lombok.Getter;

import org.frankframework.management.gateway.PhoneHomeOutboundGateway;

public class JwtKeyGeneratorSupplier implements InitializingBean {

	@Value("${management.gateway.outbound.class}")
	private String outboundClass;

	@Getter
	private AbstractJwtKeyGenerator jwtKeyGenerator;

	@Override
	public void afterPropertiesSet() throws Exception {
		if (Objects.equals(outboundClass, PhoneHomeOutboundGateway.class.getName())) {
			jwtKeyGenerator = new KeystoreJwtKeyGenerator();
		} else {
			jwtKeyGenerator = new JwtKeyGenerator();
		}
	}
}
