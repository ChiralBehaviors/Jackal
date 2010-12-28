package org.smartfrog.services.anubis.loadtesting;

import org.smartfrog.services.anubis.locator.AnubisValue;

public class ValueHistory {
	public final Action action;

	public final String instance;
	public final String name;
	public final long time;
	public final Object value;

	public ValueHistory(AnubisValue av, Action action) {
		this.name = av.getName();
		this.time = av.getTime();
		this.instance = av.getInstance();
		this.value = av.getValue();
		this.action = action;
	}
}