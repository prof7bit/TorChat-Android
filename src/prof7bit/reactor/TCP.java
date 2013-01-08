package prof7bit.reactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * An Instance of this class represents a TCP connection. The application
 * must implement the Callback interface and assign it to the callback
 * member in order to receive onConnect(), onDisconnect() and onReceive()
 * events. TCP is a thin wrapper around java.nio.SocketChannel. TCP also
 * implements asynchronous sending of outgoing data. Every TCP object is 
 * associated with a Reactor object and automatically subscribes to the
 * needed events to make the above things happen.
 * 
 * @author Bernd Kreuss <prof7bit@gmail.com>
 */
public class TCP extends Handle {
	
	/**
	 * The application must implement this interface and assign it to the 
	 * Callback field, so that it can receive notifications. All calls to
	 * these methods will originate from the Reactor thread.
	 */
	public interface Callback {
		public void onConnect();
		public void onDisconnect(Exception e);
		public void onReceive(ByteBuffer buf);
	}
	
	/**
	 * Assign something here that implements TCP.Callback 
	 */
	public Callback callback;
	
	/**
	 * This holds a queue of unsent or partially sent ByteBuffers if more
	 * data has been attempted to send than the underlying network socket 
	 * could handle at once.
	 */
	private Queue<ByteBuffer> unsent = new ConcurrentLinkedQueue<ByteBuffer>();
	
	/**
	 * Construct a new incoming TCP. 
	 * The Reactor will call this constructor automatically.
	 *  
	 * @param r the Reactor to which it should be attached
	 * @param sc the underlying connected SocketChannel
	 * @throws IOException
	 */
	public TCP(Reactor r, SocketChannel sc) throws IOException {
		System.out.println(this.toString() + " incoming constructor");
		initMembers(sc, r, null);
		registerWithReactor(SelectionKey.OP_READ);
	}

	/**
	 * Construct a new outgoing connection.
	 * This will create the Handle object and initiate the connect. It will
	 * not block and the application can immediately start using the send()
	 * method (which also will not block), even if the connection has not yet
	 * been established. Some time later the appropriate callback method will 
	 * be fired once the connection succeeds or fails.
	 * 
	 * @param r
	 * @param addr
	 * @param port
	 * @throws IOException
	 */
	public TCP(Reactor r, String addr, int port, Callback cb) throws IOException{
		System.out.println(this.toString() + " outgoing constructor");
		SocketChannel sc = SocketChannel.open();
		initMembers(sc, r, cb);
		SocketAddress sa = new InetSocketAddress(addr, port);
		if (sc.connect(sa)){
			// according to documentation it is possible for local connections
			// to succeed instantaneously. Then there won't be an OP_CONNECT
			// event later, we must call the handler directly from here. 
			doEventConnect();
		}else{
			// this is what normally happens in most cases
			registerWithReactor(SelectionKey.OP_CONNECT);
		}
	}
	
	/**
	 * initialize member variables, used during construction
	 */
	protected void initMembers(SocketChannel sc, Reactor r, Callback cb) throws IOException{
		sc.configureBlocking(false);
		sc.socket().setTcpNoDelay(true);
		channel = sc;
		reactor = r;
		callback = cb;
	}
	
	/**
	 * Send the bytes in the buffer asynchronously. Data will be enqueued 
	 * for sending and OP_WRITE events will be used to send it from the Reactor
	 * thread until all data has been sent. This method can be used even before
	 * the underlying connection has actually been established yet, data will
	 * be queued and sent upon successful connect. 
	 * 
	 * @param buf the ByteBuffer containing the bytes to be sent.
	 */
	public void send(ByteBuffer buf){
		buf.position(0);
		unsent.offer(buf);
		if (((SocketChannel) channel).isConnected()){
			registerWithReactor(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
		} 
		// if not yet connected it will be deferred until doEventConnect() 
	}
		
	/**
	 * This method is automatically called by the Reactor. It can handle
	 * receive events and also detect a disconnect from the remote host.
	 * 
	 * @throws IOException if that happens the reactor will close the
	 * connection and fire the onDisconnect() event.
	 */
	protected void doEventRead() throws IOException{
		System.out.println(this.toString() + " doEventRead()");
		ByteBuffer buf = ByteBuffer.allocate(2048);
		SocketChannel sc = (SocketChannel)channel;
		int numRead = sc.read(buf);
		if (numRead == -1){
			// this will make the reactor close the channel
			// and then fire our onDisconnect() event.
			throw new IOException("closed by foreign host");
		}else{
			buf.position(0);
			buf.limit(numRead);
			callback.onReceive(buf);
		}
	}
	
	/**
	 * This method is automatically called by the Reactor.
	 */
	protected void doEventDisconnect(Exception e){
		System.out.println(this.toString() + " doEventDisconnect() " + e.getMessage());
		callback.onDisconnect(e);
	}
	
	/**
	 * This method is automatically called by the Reactor.
	 */
	protected void doEventConnect() {
		System.out.println(this.toString() + " doEventConnect()");
		
		if (unsent.isEmpty()){
			registerWithReactor(SelectionKey.OP_READ);
		}else{
			registerWithReactor(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
		}
		callback.onConnect();
	}	
	
	/**
	 * Automatically called by the Reactor. This event will not be propagated
	 * to the application, it is only used internally to automatically send
	 * remaining data from the unsent queue. Any IOException happening here
	 * will cause the channel to be closed and onDisconnect() to be fired.
	 * 
	 * @throws IOException
	 */
	protected void doEventWrite() throws IOException{
		System.out.println(this.toString() + " doEventWrite()");
		SocketChannel sc = (SocketChannel)channel;

		// we will try to write as many buffers as possible in one event
		// we break on the first sign of congestion (partial buffer) 
		while(true){
			ByteBuffer buf = unsent.peek();
			if (buf == null){
				// we are done, queue is empty, re-register without OP_WRITE
				registerWithReactor(SelectionKey.OP_READ);
				break;
			}
			
			sc.write(buf);
			if (buf.hasRemaining()){
				break; // congestion --> enough for the moment
			}else{
				unsent.remove(); // ok --> try next buffer
			}
		}
	}
}
