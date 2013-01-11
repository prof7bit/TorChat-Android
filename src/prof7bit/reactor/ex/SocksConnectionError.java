package prof7bit.reactor.ex;

import java.io.IOException;

public class SocksConnectionError extends IOException {
	private static final long serialVersionUID = 6259586626320066994L;
	private int statusCode;
	
	public SocksConnectionError(String detail, int statusCode){
		super(detail);
		this.statusCode = statusCode;
	}
	
	public int getStatusCode(){
		return statusCode;
	}
}
