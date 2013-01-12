package prof7bit.reactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * This class represents a network port listening for incoming TCP connections.
 * It is a thin wrapper around ServerSocketChannel. EventHandler.onAccept() must be
 * implemented by the application and will be fired when an incoming TCP
 * connection is accepted.
 * 
 * @author Bernd Kreuss <prof7bit@gmail.com>
 */
public class ListenPort extends Handle{
	
	/**
	 * The application must implement this in order to receive incoming 
	 * connections. All calls to this interface will originate from the
	 * reactor thread. The first thing the application must do in onAccept()
	 * is to assign its implementation of TCP.EventHandler to tcp.callback,
	 * this must happen before it returns from onAccept().
	 */
	public interface EventHandler {
		void onAccept(TCP tcp);
	}
	
	/**
	 * The application must assign its implementation 
	 * of the EventHandler interface to this member 
	 */
	public EventHandler eventHandler;

	public ListenPort(Reactor r, EventHandler eh){
		reactor = r;
		eventHandler = eh;
		if (eventHandler == null){
			throw new NullPointerException("ListenPort instance without eventHandler");
		}
	}
	
	/**
	 * bind the port and start listening for incoming TCP connections.
	 * Every incoming TCP connection will now be automatically accepted and 
	 * every time this happens the onAccept() handler will be called along 
	 * with a newly instantiated TCP object.
	 *  
	 * @param port the port to bind
	 * @throws IOException
	 */
	public void listen(int port) throws IOException{
		ServerSocketChannel ssc = ServerSocketChannel.open();
		ssc.socket().setReuseAddress(true);
		ssc.socket().bind(new InetSocketAddress(port));
		ssc.configureBlocking(false); 
		channel = ssc;
		registerWithReactor(SelectionKey.OP_ACCEPT);
	}

	/**
	 * called by the reactor
	 */
	protected void doEventAccept() throws IOException{
		ServerSocketChannel ssc = (ServerSocketChannel) channel;
		SocketChannel sc = ssc.accept();
		TCP tcp = new TCP(reactor, sc);
		eventHandler.onAccept(tcp);
		
		// the onAccept handler *must* install a eventHandler before it returns!
		if (tcp.eventHandler == null){
			throw new NullPointerException("TCP instance (incoming) without eventHandler");
		}
	}

	/**
	 * called by the reactor when the listening socket is shut down (normally
	 * only when the application terminates). Not much need to pass this event
	 * to the application, just ignore it. 
	 */
	@Override
	void doEventClose(IOException e) {
		// ListenPort will just ignore this event 
	}
}
