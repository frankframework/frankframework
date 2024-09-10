package org.frankframework.pipes.hash;

/**
 * Defines the supported algorithms which can be used in the {@link org.frankframework.pipes.HashPipe}
 */
public enum Algorithm {
	MD5,
	SHA,
	SHA256("SHA-256"),
	SHA384("SHA-384"),
	SHA512("SHA-512"),
	CRC32,
	ADLER32,
	HmacMD5(true),
	HmacSHA1(true),
	HmacSHA256(true),
	HmacSHA384(true),
	HmacSHA512(true);

	private final String algorithm;

	private final boolean requiresSecret;

	Algorithm(String algorithm, boolean requiresSecret) {
		this.algorithm = algorithm;
		this.requiresSecret = requiresSecret;
	}

	Algorithm(boolean requiresSecret) {
		this(null, requiresSecret);
	}

	Algorithm(String algorithm) {
		this(algorithm, false);
	}

	Algorithm() {
		this(null, false);
	}

	public String getAlgorithm() {
		return algorithm != null ? algorithm : name();
	}

	public boolean isRequiresSecret() {
		return requiresSecret;
	}
}
