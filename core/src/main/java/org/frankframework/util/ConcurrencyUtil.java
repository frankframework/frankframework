package org.frankframework.util;

import java.util.concurrent.Phaser;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class ConcurrencyUtil {

	public static void waitForZeroPhasesLeft(final Phaser phaser) throws InterruptedException {
		if (phaser.getUnarrivedParties() == 0) return; // already done
		int currentPhase;
		do {
			currentPhase = phaser.awaitAdvanceInterruptibly(phaser.getPhase());
		} while (currentPhase > 0);
	}
}
