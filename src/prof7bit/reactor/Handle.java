package prof7bit.reactor;

import java.io.IOException;
import java.nio.channels.SelectableChannel;

/**
 * Abstract base class for TCP and ListenPort.
 * 
 * All descendants of the Handle class will be registered with a Reactor
 * and they will all have their own event handler interface to which the 
 * Reactor will dispatch the events they subscribed for.
 * 
 * @author Bernd Kreuss <prof7bit@gmail.com>
 */
public abstract class Handle {
	
	protected SelectableChannel channel;
	protected Reactor reactor;
	
	protected void registerWithReactor(int ops){
		reactor.register(this, ops);
	}
	
	public void close(IOException reason){
		reactor.requestCloseHandle(this, reason);
	}
	
	public void close(String reason){
		close(new IOException(reason));
	}
	
	abstract void doEventClose(IOException e);
}
