package org.smartfrog.services.anubis;

public class StabilityHistory {
	public final boolean isStable;
	public final long timeRef;

	public StabilityHistory(boolean isStable, long timeRef) {
		super();
		this.isStable = isStable;
		this.timeRef = timeRef;
	}
}