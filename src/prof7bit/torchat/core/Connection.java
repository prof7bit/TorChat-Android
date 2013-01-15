package prof7bit.torchat.core;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;

import prof7bit.reactor.TCPHandler;
import prof7bit.reactor.Reactor;
import prof7bit.reactor.TCP;

/**
 * This class represents an established TorChat p2p connection, it can either 
 * be an incoming or outgoing connection. Every buddy always needs both of them. 
 *
 * @author Bernd Kreuss <prof7bit@gmail.com>
 *
 */
public class Connection implements TCPHandler{
	private TCP tcp;
	private byte[] bufIncomplete = new byte[0];
	
	public void send(MessageBuffer b){
		tcp.send(b.encodeForSending());
	}
	
	/**
	 * Here we have accepted an incoming connection, this constructor
	 * was called by our Listener. The Handle exists and is connected 
	 * already, we can use it right away.
	 * 
	 * @param c an already connected Handle object 
	 */
	public Connection(TCP c){
		tcp = c;
	}
	
	/**
	 * Create a new outgoing connection through the Tor proxy (Socks4a)
	 * The constructor will return immediately and a new Handle will be created 
	 * but it is not yet finished connecting. After some time either the 
	 * onConnect() or the onDisconnect() method will be called. We can already 
	 * start sending, it will be queued until connect succeeds.
	 * 
	 * @param r the reactor that should monitor this connection
	 * @param addr IP-address or host name to connect to
	 * @param port Port to connect to
	 * @throws IOException problems opening the local socket (not the connection itself)  
	 */
	public Connection(Reactor r, String addr, int port) throws IOException{
		tcp = new TCP(r, addr, port, this, "127.0.0.1", 9050, "TorChat");
	}

	@Override
	public void onConnect() {
		// TODO Auto-generated method stub
	}

	@Override
	public void onDisconnect(Exception e) {
		System.out.println("Connection.onDisconnect: " + e.toString());
		// TODO Auto-generated method stub
	}

	@Override
	public void onReceive(ByteBuffer bufReceived){
		
		// bufTotal = existing data + new data
		int lenReceived = bufReceived.limit();
		int lenIncomplete = 0;
		lenIncomplete = bufIncomplete.length;
		int lenTotal = lenIncomplete + lenReceived;
		byte[] bufTotal = new byte[lenTotal];
		System.arraycopy(bufIncomplete, 0, bufTotal, 0, lenIncomplete);
		System.arraycopy(bufReceived.array(), 0, bufTotal, lenIncomplete, lenReceived);
		
		// split bufTotal at 0x0a and call onCompleteMesssage() with every chunk
		int posMsgStart = 0;
		int posDelimiter = 0;
		while (posDelimiter < lenTotal){
			if (bufTotal[posDelimiter] == 0x0a){
				int lenMsg = posDelimiter - posMsgStart;
				if (lenMsg > 0){
					byte[] msg = new byte[lenMsg];
					System.arraycopy(bufTotal, posMsgStart, msg, 0, lenMsg);
					onCompleteMessage(msg);
				}
				posMsgStart = posDelimiter + 1;
				posDelimiter = posMsgStart - 1;
			}
			posDelimiter++;
		}
		
		// copy remaining (incomplete) last message into bufIncomplete.
		int lenRemain = lenTotal - posMsgStart; 
		if (lenRemain > 0){
			bufIncomplete = new byte[lenRemain];
			System.arraycopy(bufTotal, posMsgStart, bufIncomplete, 0, lenRemain);
		}else{
			if (bufIncomplete.length > 0){
				bufIncomplete = new byte[0];
			}
		}
	}

	/**
	 * This will be called for every complete message. It will try to 
	 * instantiate the appropriate message class for this type of message,
	 * parse and enqueue it for processing. Unknown commands will result in a 
	 * MsgUnknown message to be enqueued, malformed messages (empty or unable 
	 * to parse) are a protocol violation and it will close the connection.
	 * 
	 * @param bytes the raw transfer-encoded message, delimiters already stripped
	 */
	private void onCompleteMessage(byte[] bytes){
		MessageBuffer buf = new MessageBuffer(bytes);
		try {
			String command = buf.readCommand();
			Msg msg = getMsgInstanceFromCommand(command);
			msg.parse(buf);
			msg.execute(); // TODO: should enqueue it for executing in separate thread
		} catch (EOFException e) {
			// this would be thrown by readCommand()
			this.tcp.close("peer has sent empty message");
		} catch (XMessageParseException e) {
			// this would be thrown by parse()
			this.tcp.close("peer has sent malformed message: " + e.getMessage());
		} catch (Exception e) {
			// This would be thrown by getMsgInstanceFromCommand()
			// this should never happen and would be a bug in TorChat itself.
			System.err.println("Houston, we have a problem!");
			e.printStackTrace();
			this.tcp.close("internal protocol error");
		} finally {
			try {
				buf.close();
			} catch (IOException e) {
				// won't happen
			}
		}
	}
	
	/**
	 * Instantiate and return the correct message for this command.
	 * If the command can not be found then instantiate MsgUnknown.
	 * 
	 * @param command String containing the command
	 * @return an instance of the appropriate message class
	 * @throws Exception missing or wrong constructor or illegal arguments
	 * when calling constructor or other things that indicate wrongly 
	 * implemented message classes. This would be a bug in TorChat
	 * itself or one of the Msg_xxxx classes and is not supposed to happen.
	 */
	private Msg getMsgInstanceFromCommand(String command) throws Exception{
		try {
			String packageName = this.getClass().getPackage().getName();
			Class<?> C = Class.forName(packageName + ".Msg_" + command);
			return (Msg) C.getConstructor(Connection.class).newInstance(this);
		} catch (ClassNotFoundException e) {
			// this is normal, it happens for unknown incoming commands,in this 
			// case we use the null-message which will just send the reply 
			// "not_implemented" and otherwise does nothing.
			return new MsgUnknown(this);
		}
	}
}
