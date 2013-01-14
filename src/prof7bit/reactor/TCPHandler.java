package prof7bit.reactor;

import java.nio.ByteBuffer;

/**
 * The application must provide an implementation of this interface
 * to be able to receive notifications about events
 */
public interface TCPHandler {
	public void onConnect();
	public void onDisconnect(Exception e);
	public void onReceive(ByteBuffer buf);
}