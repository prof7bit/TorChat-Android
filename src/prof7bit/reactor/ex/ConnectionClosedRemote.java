package prof7bit.reactor.ex;

import java.io.IOException;

public class ConnectionClosedRemote extends IOException {
	private static final long serialVersionUID = 3587976820171006786L;

	public ConnectionClosedRemote(String detailMessage) {
		super(detailMessage);
	}
}
