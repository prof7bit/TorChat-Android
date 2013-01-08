package prof7bit.torchat.core;

import java.io.IOException;
import java.nio.ByteBuffer;

import prof7bit.reactor.Reactor;
import prof7bit.reactor.TCP;

public class Connection implements TCP.Callback{
	private TCP tcp;
	private byte[] bufIncomplete = null;
	
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
		tcp.callback = this;
	}
	
	/**
	 * Create a new outgoing connection. The constructor will return
	 * immediately and a new Handle will be created but it is not yet 
	 * finished connecting. After some time either the onConnect() or
	 * the onDisconnect() method will be called. We can already start
	 * sending, it will be queued until connect succeeds.
	 * 
	 * @param address IP-address or host name to connect to
	 * @param port
	 * @throws IOException 
	 */
	public Connection(Reactor r, String address, int port) throws IOException{
		tcp = new TCP(r, address, port, this);
	}

	@Override
	public void onConnect() {
		// TODO Auto-generated method stub
	}

	@Override
	public void onDisconnect(Exception e) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onReceive(ByteBuffer bufReceived){
		
		// bufTotal = existing data + new data
		int lenReceived = bufReceived.limit();
		int lenIncomplete = 0;
		if (bufIncomplete != null){
			lenIncomplete = bufIncomplete.length;
		}
		int lenTotal = lenIncomplete + lenReceived;
		byte[] bufTotal = new byte[lenTotal];
		if (lenIncomplete > 0){
			System.arraycopy(bufIncomplete, 0, bufTotal, 0, lenIncomplete);
		}
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
			// Most often the remaining data will be empty, so for this reason 
			// I am treating this as a separate case, introducing some more
			// if/else and make it look more complicated but avoiding to 
			// re-allocate an unused byte[0] over and over again. This way
			// it will just stay null almost all of the time.
			bufIncomplete = null;
		}
	}

	/**
	 * This will be called for every complete message
	 * 
	 * @param msg the raw transfer-encoded message, delimiters already stripped
	 */
	private void onCompleteMessage(byte[] msg){
		MessageBuffer b = new MessageBuffer(msg);
		
		// debug print message
		while (true){
			String s = b.readString();
			if (s == null){
				break;
			}
			System.out.println("-->" + s + "<--");
		}
	}
}
