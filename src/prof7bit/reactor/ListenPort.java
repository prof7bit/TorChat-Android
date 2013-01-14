package prof7bit.reactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * This class represents a network port listening for incoming TCP connections.
 * It is a thin wrapper around ServerSocketChannel. ListenPortHandler.onAccept() must be
 * implemented by the application and will be fired when an incoming TCP
 * connection is accepted.
 * 
 * @author Bernd Kreuss <prof7bit@gmail.com>
 */
public class ListenPort extends Handle{
	
	/**
	 * The application's event handler will be assigned here 
	 */
	private ListenPortHandler eventHandler;

	public ListenPort(Reactor r, ListenPortHandler eh){
		reactor = r;
		eventHandler = eh;
	}
	
	/**
	 * bind the port and start listening for incoming TCP connections.
	 * Every incoming TCP connection will now be automatically accepted and 
	 * every time this happens the onAccept() handler will be called along 
	 * with a newly instantiated TCP object.
	 *  
	 * @param port the port to bind
	 * @throws IOException socket cannot be opened or bound
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
	 * called by the reactor.
	 */
	protected void doEventAccept() throws IOException{
		ServerSocketChannel ssc = (ServerSocketChannel) channel;
		SocketChannel sc = ssc.accept();
		TCP tcp = new TCP(reactor, sc);
		TCPHandler eh = eventHandler.onAccept(tcp);
		tcp.setEventHandler(eh);
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
