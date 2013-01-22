package prof7bit.torchat.core;

import java.io.EOFException;

import junit.framework.TestCase;

import org.junit.Test;

import prof7bit.torchat.core.MessageBuffer;

public class TestMessageBuffer extends TestCase {
	
	private final byte[] binWithSpace   = "foo \\ \n \r\n \r \u0000 bar".getBytes();
	private final byte[] encWithSpace   = "foo \\/ \\n \r\\n \r \u0000 bar".getBytes();
	private final byte[] encWithSpaceLF = "foo \\/ \\n \r\\n \r \u0000 bar\n".getBytes();
	
	private final String strUnclean = "\r\nfoo\r\nbar\rbaz\nblub\n  "; 
	private final String strTrimmed = "foo\nbar\nbaz\nblub"; 

	private MessageBuffer b;
	
	protected void setUp() throws Exception {
		super.setUp();
		b = new MessageBuffer();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		b.close();
	}
	
	/**
	 * compare two byte arrays
	 */
	private boolean eq(byte[] x, byte[] y){
		if (x.length != y.length){
			return false;
		}
		for (int i=0; i<x.length; i++){
			if (x[i] != y[i]){
				return false;
			}
		}
		return true;
	}
	
	@Test
	public void testEq(){
		assertTrue(eq(binWithSpace, binWithSpace));
		assertFalse(eq(binWithSpace, encWithSpace));
	}
	
	@Test
	public void testBinaryEncoding(){
		b.writeBytes(binWithSpace);
		assertTrue(eq(b.encodeForSending().array(), encWithSpaceLF));
	}

	@Test
	public void testBinaryDecoding(){
		b = new MessageBuffer(encWithSpace);
		try {
			assertTrue(eq(b.readBytesUntilEnd(), binWithSpace));
		} catch (EOFException e) {
			fail();
		}
	}

	@Test
	public void testReadUntilEnd(){
		b.writeString("foo bar baz");
		try {
			assertTrue(eq(b.readBytesUntilEnd(), "foo bar baz".getBytes()));
		} catch (EOFException e) {
			fail();
		}
		
		try {
			b.readBytesUntilEnd();
			fail();
		} catch (EOFException e) {
			// must throw
		}
	}

	@Test
	public void testStringSanitizing(){
		
		// sanitizing during readString()
		// (write raw bytes and then use readString() to sanitize them)
		b.writeBytes(strUnclean.getBytes());
		try {
			String actual = b.readString();
			assertEquals(strTrimmed, actual);
		} catch (EOFException e) {
			fail();
		}
		
		// sanitizing during writeString()
		// (writeString() and then look at raw bytes to see if sanitized)
		b.reset();
		b.resetReadPos();
		b.writeString(strUnclean);
		try {
			String actual = new String(b.readBytes());
			assertEquals(strTrimmed, actual);
		} catch (EOFException e) {
			fail();
		}
	}

	@Test
	public void testReadCommand(){
		String cmd;
		// intentionally polluting it with line breaks to test robustness
		b.writeBytes("foo\r\n bar\r\n".getBytes());
		try {
			cmd = b.readCommand();
			assertEquals("foo", cmd);
		} catch (EOFException e) {
			fail();
		}
		
		// try again, must again give same result
		try {
			cmd = b.readCommand();
			assertEquals("foo", cmd);
		} catch (EOFException e) {
			fail();
		}
		
		// now read more, position must now be at beginning of "bar"
		try {
			cmd = b.readString();
			assertEquals("bar", cmd);
		} catch (EOFException e) {
			fail();
		}
		
		// command with only line breaks must throw EOF
		b.reset();
		b.writeBytes("\r\n".getBytes());
		try {
			cmd = b.readCommand();
			fail();
		} catch (EOFException e) {
			// must throw EOF
		}
		
	}		

	
	@Test
	public void testReadString(){
		String s;
		b.writeBytes("foo  bar".getBytes()); // two spaces!

		try {
			s = b.readString();
			assertEquals("foo", s);
		} catch (EOFException e) {
			fail();
		}
		
		// empty string (between two spaces)
		try {
			s = b.readString();
			assertEquals("", s);
		} catch (EOFException e) {
			fail();
		}
		
		try {
			s = b.readString();
			assertEquals("bar", s);
		} catch (EOFException e) {
			fail();
		}
		
		// EOF because nothing more to read
		try {
			s = b.readString();
			fail();
		} catch (EOFException e) {
			// now it must throw;
		}
	}
	
	
	/**
	 * debug output ascii string to hex
	 */
	@SuppressWarnings("unused")
	private void hex(String s){
		for (int i=0; i<s.length(); i++){
			System.out.printf("%02x ", (byte) s.charAt(i));
		}
		System.err.println();
	}

}
