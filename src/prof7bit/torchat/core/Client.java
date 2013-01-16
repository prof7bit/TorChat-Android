package prof7bit.torchat.core;

import java.io.IOException;

import prof7bit.reactor.ListenPort;
import prof7bit.reactor.ListenPortHandler;
import prof7bit.reactor.Reactor;
import prof7bit.reactor.TCP;
import prof7bit.reactor.TCPHandler;

public class Client implements ListenPortHandler{
	private ClientHandler clientHandler;
	private Reactor reactor;
	private ListenPort listenPort;

	public Client(ClientHandler clientHandler, int port) throws IOException {
		this.clientHandler = clientHandler;
		this.reactor = new Reactor();
		this.listenPort = new ListenPort(reactor, this);
		this.listenPort.listen(port);
	}
	
	public void close() throws InterruptedException {
		this.reactor.close();
	}

	@Override
	public TCPHandler onAccept(TCP tcp) {
		Connection c = new Connection(tcp);
		return c;
	}
}
