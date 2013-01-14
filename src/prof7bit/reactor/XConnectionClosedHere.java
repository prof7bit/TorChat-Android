package prof7bit.reactor;

import java.io.IOException;

public class XConnectionClosedHere extends IOException {
	private static final long serialVersionUID = 377644111604273744L;

	public XConnectionClosedHere(String detailMessage) {
		super(detailMessage);
	}
}
