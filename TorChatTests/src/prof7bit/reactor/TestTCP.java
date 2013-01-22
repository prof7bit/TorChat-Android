package prof7bit.reactor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import prof7bit.reactor.ListenPort;
import prof7bit.reactor.ListenPortHandler;
import prof7bit.reactor.Reactor;
import prof7bit.reactor.TCP;
import prof7bit.reactor.TCPHandler;
import prof7bit.reactor.XConnectionClosedHere;
import prof7bit.reactor.XConnectionClosedRemote;

public class TestTCP implements ListenPortHandler, TCPHandler {

	private static int TEST_PORT = 3456; 
	private Reactor reactor;
	private ListenPort listener;
	private TCP tcpOut;
	
	private Latch latchAcc;
	private Latch latchRcvA;
	private Latch latchRcvB;	
	private Latch latchConA;
	private Latch latchConB;
	private Latch latchDisA;
	private Latch latchDisB;
	
	private ByteBuffer bufRcvA;
	private ByteBuffer bufRcvB;
	private Exception exDisA;
	private Exception exDisB;
	
	@Before
	public void setUp() throws Exception {
		latchAcc = new Latch(1);
		latchRcvA = new Latch(1);
		latchRcvB = new Latch(1);
		latchConA = new Latch(1);
		latchConB = new Latch(1);
		latchDisA = new Latch(1);
		latchDisB = new Latch(1);
		
		reactor = new Reactor();
		listener = new ListenPort(reactor, this);
		listener.listen(TEST_PORT);
	}

	@After
	public void tearDown() throws Exception {
		reactor.close();
	}
	

	@Test
	public void testConnectEchoDisconnect() {
		ByteBuffer bufSnd = ByteBuffer.wrap("test transmission".getBytes());
		try {
			tcpOut = new TCP(reactor, "127.0.0.1", TEST_PORT, this);
		} catch (IOException e) {
			fail("TCP constructor exception");
		}
		tcpOut.send(bufSnd); // will be queued until connect happens
		
		latchRcvA.await(1000); 
		assertEquals(0, latchAcc.getCount());
		assertEquals(0, latchConA.getCount());
		assertEquals(1, latchConB.getCount());
		assertEquals(0, latchRcvA.getCount());
		assertEquals(0, latchRcvB.getCount());
		assertEquals(1, latchDisA.getCount()); 
		assertEquals(1, latchDisB.getCount()); 
		
		bufSnd.position(0);
		bufRcvA.position(0);
		bufRcvB.position(0);
		assertTrue(bufSnd.equals(bufRcvB));
		assertTrue(bufSnd.equals(bufRcvA));
		
		// now disconnect
		tcpOut.close("test reason");
		latchDisA.await(1000);
		latchDisB.await(1000);
		assertTrue(exDisA instanceof XConnectionClosedHere);
		assertTrue(exDisB instanceof XConnectionClosedRemote);
		assertEquals("test reason", exDisA.getMessage());
	}

	
	
	// handler for ListenPort
	
	@Override
	public TCPHandler onAccept(TCP tcp) {
		latchAcc.countDown();
		return new IncomingHandler(tcp);
	}
	
	
	
	// handler for outgoing TCP
	
	@Override
	public void onConnect() {
		latchConA.countDown();
	}

	@Override
	public void onDisconnect(Exception e) {
		exDisA = e;
		latchDisA.countDown();
	}

	@Override
	public void onReceive(ByteBuffer buf) {
		bufRcvA = buf;
		latchRcvA.countDown();
	}
	
	
	
	// handler for incoming TCP
	
	private class IncomingHandler implements TCPHandler{

		private TCP tcp;
		
		public IncomingHandler(TCP tcp){
			this.tcp = tcp;
		}           
		
		@Override
		public void onConnect() {
			latchConB.countDown();
		}

		@Override
		public void onDisconnect(Exception e) {
			exDisB = e;
			latchDisB.countDown();
		}

		@Override
		public void onReceive(ByteBuffer buf) {
			bufRcvB = buf;
			tcp.send(buf);
			latchRcvB.countDown();
		}
	}
	
	
	// used for waiting in main thread 
	
	private class Latch extends CountDownLatch{

		public Latch(int count) {
			super(count);
		}
		
		public void await(int timeout){
			try {
				if(!await(timeout, TimeUnit.MILLISECONDS)){
					fail("timeout");
				}
			} catch (InterruptedException e) {
				fail("interrupted while waiting");
			}
		}
	}
}
