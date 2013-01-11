package prof7bit.reactor.ex;

import java.io.IOException;

public class SocksHandshakeError extends IOException {
	private static final long serialVersionUID = 7762999740312591839L;

	public SocksHandshakeError(String detailMessage) {
		super(detailMessage);
	}
}
