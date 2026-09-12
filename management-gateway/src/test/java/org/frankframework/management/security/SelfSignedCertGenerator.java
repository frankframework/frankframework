package org.frankframework.management.security;


import java.math.BigInteger;
import java.security.KeyPair;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

class SelfSignedCertGenerator {
	private SelfSignedCertGenerator() {
	}

	static X509Certificate generate(String dn, KeyPair pair, int days) throws CertificateException, OperatorCreationException {
		BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
		Instant validFrom = Instant.now();
		Instant validTo = validFrom.plus(days, ChronoUnit.DAYS);
		X500Name subject = new X500Name(dn);

		X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
				subject, serial, Date.from(validFrom),
				Date.from(validTo), subject, pair.getPublic());
		ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(pair.getPrivate());
		return new JcaX509CertificateConverter().getCertificate(builder.build(signer));
	}
}
