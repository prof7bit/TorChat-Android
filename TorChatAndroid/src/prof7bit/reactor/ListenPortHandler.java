package prof7bit.reactor;

/**
 * The application must provide an implementation of this interface
 * to be able to receive notifications about events
 */
public interface ListenPortHandler {
	TCPHandler onAccept(TCP tcp);
}