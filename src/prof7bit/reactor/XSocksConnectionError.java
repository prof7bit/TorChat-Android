package prof7bit.reactor;

import java.io.IOException;

public class XSocksConnectionError extends IOException {
	private static final long serialVersionUID = 6259586626320066994L;
	private int statusCode;
	
	public XSocksConnectionError(String detail, int statusCode){
		super(detail);
		this.statusCode = statusCode;
	}
	
	public int getStatusCode(){
		return statusCode;
	}
}
