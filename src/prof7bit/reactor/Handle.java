package prof7bit.reactor;

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
	
	public void close(){
		reactor.requestCloseHandle(this);
	}
	
	@Override
	protected void finalize(){
		System.out.println(this.toString() + " garbage collected");
	}
}
