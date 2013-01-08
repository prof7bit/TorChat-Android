package prof7bit.reactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * This class represents a network port listening for incoming TCP connections.
 * It is a thin wrapper around ServerSocketChannel. Callback.onAccept() must be
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
	 * is to assign its implementation of TCP.Callback to tcp.callback,
	 * this must happen before it returns from onAccept().
	 */
	public interface Callback {
		void onAccept(TCP tcp);
	}
	
	/**
	 * The application must assign its implementation 
	 * of the Callback interface to this member 
	 */
	public Callback callback;

	public ListenPort(Reactor r, Callback cb){
		reactor = r;
		callback = cb;
		if (callback == null){
			throw new NullPointerException("ListenPort instance without callback");
		}
	}
	
	public void listen(int port) throws IOException{
		ServerSocketChannel ssc = ServerSocketChannel.open();
		ssc.socket().setReuseAddress(true);
		ssc.socket().bind(new InetSocketAddress(port));
		ssc.configureBlocking(false); 
		channel = ssc;
		registerWithReactor(SelectionKey.OP_ACCEPT);
	}
	
	protected void doEventAccept() throws IOException{
		ServerSocketChannel ssc = (ServerSocketChannel) channel;
		SocketChannel sc = ssc.accept();
		TCP tcp = new TCP(reactor, sc);
		callback.onAccept(tcp);
		
		// the onAccept handler *must* install a callback before it returns!
		if (tcp.callback == null){
			throw new NullPointerException("TCP instance (incoming) without callback");
		}
	}
}
