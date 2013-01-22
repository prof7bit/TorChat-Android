package prof7bit.reactor;

import java.io.IOException;

public class XSocksHandshakeError extends IOException {
	private static final long serialVersionUID = 7762999740312591839L;

	public XSocksHandshakeError(String detailMessage) {
		super(detailMessage);
	}
}
