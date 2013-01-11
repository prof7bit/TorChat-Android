package prof7bit.reactor.ex;

import java.io.IOException;

public class ConnectionClosedHere extends IOException {
	private static final long serialVersionUID = 377644111604273744L;

	public ConnectionClosedHere(String detailMessage) {
		super(detailMessage);
	}
}
