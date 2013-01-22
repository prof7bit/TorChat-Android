package prof7bit.reactor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * An Instance of this class represents a TCP connection. The application
 * must implement the ListenPortHandler interface and assign it to the eventHandler
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
	 * The application's event handler will be assigned here 
	 */
	private TCPHandler eventHandler;
	
	/**
	 * This holds a queue of unsent or partially sent ByteBuffers if more
	 * data has been attempted to send than the underlying network socket 
	 * could handle at once.
	 */
	private Queue<ByteBuffer> unsent = new ConcurrentLinkedQueue<ByteBuffer>();
	
	/**
	 * This signals that we may not yet subscribe to OP_WRITE and not yet send 
	 * queued data because we are still talking to the socks proxy. The socks
	 * connection handler itself does not use the send queue at all.  
	 */
	private boolean insideSocksHandshake = false;
		
	/**
	 * Construct a new incoming TCP. 
	 * The Framework will call this constructor automatically for incoming 
	 * connections. Note that we do not yet initialize the event handler here, 
	 * for incoming connections this will happen *after* the onAccept() has
	 * returned. See the code of ListenPort.doEventAccept() for details.
	 *  
	 * @param r the reactor that should manage this TCP object
	 * @param sc the underlying connected SocketChannel
	 * @throws IOException if I/O error occurs
	 */
	public TCP(Reactor r, SocketChannel sc) throws IOException {
		System.out.println(this.toString() + " incoming constructor");
		initMembers(sc, r);
		registerWithReactor(SelectionKey.OP_READ);
	}

	/**
	 * Construct a new outgoing connection.
	 * This will create the Handle object and initiate the connect. It will
	 * not block and the application can immediately start using the send()
	 * method (which also will not block), even if the connection has not yet
	 * been established. Some time later the appropriate eventHandler method will 
	 * be fired once the connection succeeds or fails.
	 * 
	 * @param r the reactor that should manage this TCP object
	 * @param addr the server to connect to
	 * @param port the port of the server to connect to
	 * @param eh the event handler that will receive the events
	 * @throws IOException if I/O error occurs
	 */
	public TCP(Reactor r, String addr, int port, TCPHandler eh) throws IOException{
		System.out.println(this.toString() + " outgoing constructor");
		connect(r, addr, port, eh);
	}

	/**
	 * Construct a new outgoing TCP connection through a socks4a proxy.
	 * Towards the application this behaves exactly like the other constructor,
	 * you can immediately start sending (queued), etc. The only difference is
	 * this will connect through a socks4a proxy (4a means the socks proxy will 
	 * resolve host names) 
	 * 
	 * @param r The reactor that should manage this TCP object
	 * @param addr The server to connect to
	 * @param port The port of the server to connect to
	 * @param eh The event handler of the application, may NOT be null
	 * @param proxy_addr address of the socks proxy
	 * @param proxy_port port of the socks proxy
	 * @param proxy_user user to use in socks4 authentication
	 * @throws IOException if an I/O error occurs while initializing socket
	 */
	public TCP(Reactor r, String addr, int port, TCPHandler eh, String proxy_addr, int proxy_port, String proxy_user) throws IOException{
		checkEventHandler(eh);
		// the socks handler will upon successful connection replace itself 
		// with the event handler that was provided by the application.
		Socks4aHandler sockshandler = new Socks4aHandler(this, addr, port, proxy_user, eh);
		connect(r, proxy_addr, proxy_port, sockshandler);
	}
	
	/**
	 * this is only meant to be used from inside the constructor
	 */
	private void connect(Reactor r, String addr, int port, TCPHandler eh) throws IOException {
		SocketChannel sc = SocketChannel.open();
		initMembers(sc, r);
		setEventHandler(eh);
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
	 * initialize some member variables, used during construction
	 */
	private void initMembers(SocketChannel sc, Reactor r) throws IOException{
		sc.configureBlocking(false);
		sc.socket().setTcpNoDelay(true);
		channel = sc;
		reactor = r;
	}

	/**
	 * The application must use this to register its event handler if it 
	 * receives a new incoming connection. For outgoing connections this
	 * has happened in the constructor already.
	 * 
	 * @param eventHandler ListenPortHandler implementation provided by application
	 */
	public void setEventHandler(TCPHandler eventHandler) {
		checkEventHandler(eventHandler);
		this.eventHandler = eventHandler;
	}
	
	private void checkEventHandler(TCPHandler eh){
		if (eh == null){
			throw new  IllegalArgumentException("TCP event handler must not be null");
		}
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
		if (insideSocksHandshake){
			return;
		}
		if (((SocketChannel) channel).isConnected()){
			registerWithReactor(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
		} 
		// if not yet connected or if still inside socks handshake then
		// subscribing OP_WRITE will be deferred until connection is complete.
		// The Socks handler itself will bypass the queue and write directly.
	}
	
	/**
	 * this is used only during socks connect, here don't want to use the
	 * send queue because the queue contains data sent from the application 
	 * which must not be mixed with data sent during the socks handshake.
	 *    
	 * @param buf ByteBuffer with data to send
	 * @throws IOException
	 */
	private void sendNow(ByteBuffer buf) throws IOException{
		SocketChannel sc = (SocketChannel) channel;
		while (buf.hasRemaining()){
			sc.write(buf);
		}
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
			throw new XConnectionClosedRemote("closed by foreign host");
		}else{
			buf.position(0);
			buf.limit(numRead);
			eventHandler.onReceive(buf);
		}
	}
	
	/**
	 * This method is automatically called by the Reactor. The IOException
	 * object tells the reason why exactly it had to be closed. This event
	 * also occurs when a connect attempt failed.
	 */
	@Override
	protected void doEventClose(IOException e){
		System.out.println(this.toString() + " doEventClose() " + e.getMessage());
		eventHandler.onDisconnect(e);
	}
	
	/**
	 * This method is automatically called by the Reactor. Here we can also
	 * register OP_WRITE if we have queued data to send already. 
	 * 
	 * Note that we will NOT register OP_WRITE if we did connect to a socks 
	 * proxy, in this case the queued data must wait even longer, the socks 
	 * handler will finally fire doEventConnect() a second time and then we 
	 * will be back to normal and the app will receive the connect event.
	 */
	protected void doEventConnect() {
		System.out.println(this.toString() + " doEventConnect()");
		
		if (unsent.isEmpty() | insideSocksHandshake){
			registerWithReactor(SelectionKey.OP_READ);
		}else{
			registerWithReactor(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
		}
		eventHandler.onConnect();
	}	
	
	/**
	 * Automatically called by the Reactor. This event will not be propagated
	 * to the application, it is only used internally to automatically send
	 * remaining data from the unsent queue. Any IOException happening here
	 * will cause the channel to be closed and onDisconnect() to be fired.
	 * Note that this event won't be fired during a Socks handshake, and the 
	 * Socks handler will bypass the send queue entirely. 
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
	
	/**
	 * This event handler implements the client side of a Socks4a connection
	 * request. After it has successfully succeeded the handler will replace
	 * itself with the handler that the application has provided earlier and
	 * normal TCP sending and receiving for the application may take place.
	 */
	private class Socks4aHandler implements TCPHandler{
		private TCP tcp;
		private String address;
		private int port;
		private String user; // user-ID for Socks-Proxy
		private TCPHandler appHandler;

		/**
		 * Create a new event handler to handle the socks 4a connection protocol
		 * 
		 * @param tcp the TCP-object for which this handler will be used. This
		 * must be a TCP that is already connected to the socks4a proxy
		 * @param address The address or hostname to connect to
		 * @param port The port to connect to
		 * @param user User-ID used during Socks4a protocol
		 * @param appHandler EeventHandler to install after connection succeeded
		 */
		public Socks4aHandler(TCP tcp, String address, int port, String user, TCPHandler appHandler){
			this.tcp = tcp;
			this.address = address;
			this.port = port;
			this.user = user;
			this.appHandler = appHandler;
			tcp.insideSocksHandshake = true;
		}

		@Override
		public void onConnect() {
			System.out.println("socks4a onConnect()");
			ByteArrayOutputStream req = new ByteArrayOutputStream(64);
			req.write(0x04); // socks version 4
			req.write(0x01); // request TCP stream connection
			
			// port of the server to connect to in big-endian
			req.write((byte) ((port & 0xff00) >> 8));
			req.write((byte) (port & 0x00ff));
			
			// deliberately invalid IP address to denote that we wish the 
			// 4a variant of the socks protocol (proxy will resolve name)
			req.write(0x00);
			req.write(0x00);
			req.write(0x00);
			req.write(0x01);
			
			// User-ID, null-terminated string
			byte[] buser = user.getBytes();
			req.write(buser, 0, buser.length);
			req.write(0x00);
			
			// host name to connect to, null-terminated string
			byte[] baddr = address.getBytes();
			req.write(baddr, 0, baddr.length);
			req.write(0x00);
			
			// now we send the request. Note that we do not send it through the
			// queue, we send it immediately. After this the proxy will send us 
			// an answer about success or failure, we handle that in onReceive() 
			try {
				sendNow(ByteBuffer.wrap(req.toByteArray()));
			} catch (IOException e) {
				e.printStackTrace();
				tcp.close(e);
			}
		}

		@Override
		public void onDisconnect(Exception e) {
			System.out.println("socks4a onDisconnect()");
			appHandler.onDisconnect(e);
		}

		@Override
		public void onReceive(ByteBuffer buf) {
			System.out.println("socks4a onReceive()");
			if (buf.limit() != 8){
				tcp.close(new XSocksHandshakeError("malformed reply from socks proxy"));
				return;
			}
			byte status = buf.array()[1];
			if (status != 0x5a){
				String msg = String.format(Locale.ENGLISH, "socks4a error %d while connecting %s:%s", status, address, port); 
				tcp.close(new XSocksConnectionError(msg, status));
				return;
			}
			
			// tcp stream established, now hand over all control to the application
			tcp.setEventHandler(appHandler);
			tcp.insideSocksHandshake = false;
			tcp.doEventConnect();
		}
	}
}
