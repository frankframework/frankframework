package org.frankframework.management.security;


import java.math.BigInteger;
import java.security.KeyPair;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
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
		long now = System.currentTimeMillis();
		Date notBefore = new Date(now);
		Date notAfter = new Date(now + days * 86_400_000L);
		X500Name subject = new X500Name(dn);
		X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
				subject, BigInteger.valueOf(now), notBefore, notAfter, subject, pair.getPublic());
		ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(pair.getPrivate());
		return new JcaX509CertificateConverter().getCertificate(builder.build(signer));
	}
}
