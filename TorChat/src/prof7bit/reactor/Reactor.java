package prof7bit.reactor;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;



/**
 * This implements the reactor pattern on top of the java.nio.Selector class.
 * 
 * @author Bernd Kreuss <prof7bit@gmail.com>
 */
public class Reactor extends Thread{	
	
	/**
	 * The underlying java.nio.Selector used by this reactor.
	 */
	private Selector selector;	
	
	/**
	 * tasks that need to be run between two selector.select() calls
	 * by the rector thread, such as registration of channels, etc
	 * are enqueued here and will be run immediately before select()
	 */
	private Queue<Runnable> pendingTasks = new ConcurrentLinkedQueue<Runnable>();

	/**
	 * Internal flag to signal thread termination request.
	 */
	private Boolean terminating = false;
		
	/**
	 * Create a new Reactor object and start it. This is usually one of the 
	 * first things the application does. After this the application can go 
	 * on and create ListenPorts and TCP objects and pass them a reference 
	 * to this reactor. Before the application ends the reactor's close() 
	 * method should be called to cleanly terminate and join the thread.
	 *  
	 * @throws IOException if I/O error occurs
	 */
	public Reactor() throws IOException{
		selector = Selector.open();
		start();
	}
	
	/**
	 * Request the Reactor thread and all associated network activity to 
	 * be shut down in an orderly manner and terminated as soon as possible. 
	 * This method will block until it actually *is* terminated. All open 
	 * handles associated with this reactor will be closed, all TCP disconnect 
	 * handlers will have been called before this method returns. 
	 *  
	 * @throws InterruptedException if interrupt() while in the join() call
	 */
	public void close() throws InterruptedException{
		addTask(new CloseAllAndTerminateRequest());
		join();
	}

	/**
	 * This is the thread's run method. Don't call this, its called
	 * automatically when the thread starts. It will end itself and
	 * clean up everything as soon as possible when close() is called. 
	 */
	@Override
	public void run(){
		try {
			while(!terminating){
				select(0);
			}
		} catch (Exception e) {
			System.err.println("WTF??? BUG: fatal error in select loop");
			e.printStackTrace();
			System.exit(1);
		}
		
		try {
			selector.close();
		} catch (Exception e) {
			System.err.println("WTF??? BUG: fatal error when closing selector");
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * This method is called in an infinite loop to wait for events and 
	 * dispatch them. It will perform all pending registration requests, then 
	 * block until an event on one of the registered Handle objects happens 
	 * and then dispatch them one after the other to their event handlers. 
	 * After this it will return and has to be called again. 
	 * 
	 * @param timeout maximum milliseconds to block, 0 means infinite time.
	 * @throws IOException shouldn't ever happen if used correctly. 
	 */
	private void select(long timeout) throws IOException {
		
		// perform any pending registration or cancellation requests
		while (!pendingTasks.isEmpty()){
			pendingTasks.poll().run();
		}
		
		selector.select(timeout);
		
		for (Iterator<SelectionKey> iter = selector.selectedKeys().iterator(); iter.hasNext();) { 
			SelectionKey key = iter.next(); 
			iter.remove();
			try {
				if (key.isConnectable()) {
					// outgoing connection established or connection failed.
					// (either finishConnect throws or it succeeds)
					((SocketChannel) key.channel()).finishConnect();
					((TCP) key.attachment()).doEventConnect();
				}
				
				if (key.isAcceptable()) { 
					// new incoming connection 
					((ListenPort) key.attachment()).doEventAccept();
				} 
				
				if (key.isReadable()) { 
					// bytes received or disconnect.
					((TCP) key.attachment()).doEventRead();
				}
				
				if (key.isWritable()) {
					// send buffer has become empty again, can write more data. 
					// Only connections that currently have anything queued will 
					// temporarily register for this operation and they will 
					// unregister themselves once their queue becomes empty.
					((TCP) key.attachment()).doEventWrite();
				}
				
			} catch (IOException e) {
				// on any IO exception we simply close the Handle
				// which will make it fire its onDisconnect() event.
				requestCloseHandle((Handle) key.attachment(), e);
			}
		}
	}
	
	/**
	 * Register a Handle object with this Reactor or change its registration.
	 * This method is automatically invoked by the handle objects themselves. 
	 * This method is thread safe and will not block. All registration requests
	 * will be queued to be run from the Reactor thread immediately before the 
	 * next call to selector.select(). This method is automatically called by 
	 * the Handle objects themselves when needed.
	 * 
	 * @param h The TCP or ListenPort that wishes to register
	 * @param ops interested operations (SelectionKey.OP_XXX bitmap)
	 */
	protected void register(Handle h, int ops){
		addTask(new RegistrationRequest(h, ops));
	}

	/**
	 * Request the handle to be closed. This will also make it fire
	 * the onDisonnect() handler if it is a TCP connection. This method
	 * is automatically called by the handle objects themselves. 
	 * 
	 * @param h the Handle to close
	 * @param reason IOException that should be passed to the event handler
	 */
	protected void requestCloseHandle (Handle h, IOException reason){
		addTask(new CloseRequest(h, reason));
	}
	
	/**
	 * enqueue additional code to be run (once) before the next select() call
	 *   
	 * @param r Runnable object containing the code
	 */
	private void addTask(Runnable r){
		pendingTasks.offer(r);
		selector.wakeup();
	}

	/**
	 * A request to register a Handle with this Reactor. Instances of this 
	 * class can be enqueued to be run between two calls to selector.select() 
	 */
	private class RegistrationRequest implements Runnable {
		private Handle handle;
		private int operations;
		
		public RegistrationRequest(Handle h, int ops){
			handle = h;
			operations = ops;
		}
		
		@Override
		public void run(){
			try {
				handle.channel.register(handle.reactor.selector, operations, handle);
			} catch (ClosedChannelException e) {
				e.printStackTrace();
				// nothing we can do here, just ignore it 
			}
		}
	}
	
	/**
	 * A request to close a handle. Closing a handle should not happen from
	 * a different thread while the selector is currently waiting on it, so
	 * we simply make this a queued request to be run from the selector thread. 
	 * This will also fire the onDisconnect() event because the selector won't
	 * be able to do it anymore after the handle is closed and unregistered.  
	 */
	private class CloseRequest implements Runnable {
		private Handle handle;
		private IOException reason;
		
		public CloseRequest(Handle handle, IOException reason){
			this.handle = handle;
			this.reason = reason;
		}
		
		@Override
		public void run(){
			try {
				handle.channel.close();
				handle.doEventClose(reason);
			} catch (IOException e) {
				e.printStackTrace();
				// ignore
			}
		}
	}

	/**
	 * This is a sub-request of CloseAllAndTerminateRequest  
	 */
	private class TerminateRequest implements Runnable{
		
		@Override
		public void run(){
			terminating = true;
		}
	}

	/**
	 * This will be enqueued to shut down and terminate the reactor, it
	 * will also close all handles and all disconnect handlers will fire
	 */
	private class CloseAllAndTerminateRequest implements Runnable {
		
		@Override
		public void run() {
			for (SelectionKey key:selector.keys()) {
				addTask(new CloseRequest((Handle) key.attachment(), 
						new XConnectionClosedHere("reactor shutdown")));
			}
			addTask(new TerminateRequest());
		}
	}
}
