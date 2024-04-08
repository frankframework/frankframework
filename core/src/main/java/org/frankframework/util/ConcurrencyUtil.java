package org.frankframework.util;

import java.util.concurrent.Phaser;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class ConcurrencyUtil {

	public static void waitForZeroPhasesLeft(final Phaser phaser) throws InterruptedException {
		if (phaser == null || phaser.getUnarrivedParties() == 0) return; // already done
		phaser.awaitAdvanceInterruptibly(0);
	}
}
