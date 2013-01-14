package prof7bit.reactor;

import java.io.IOException;

public class XConnectionClosedRemote extends IOException {
	private static final long serialVersionUID = 3587976820171006786L;

	public XConnectionClosedRemote(String detailMessage) {
		super(detailMessage);
	}
}
