package prof7bit.torchat.core;

/**
 * This class handles the protocol message "pong".
 *
 * @author Bernd Kreuss <prof7bit@gmail.com>
 *
 */
public class Msg_pong extends Msg {

	public Msg_pong(Connection connection) {
		super(connection);
	}

	@Override
	public void parse(MessageBuffer buf) throws XMessageParseException {
		// TODO Auto-generated method stub
	}

	@Override
	public MessageBuffer serialize() {
		MessageBuffer mb = new MessageBuffer();
		// TODO Auto-generated method stub
		return mb;
	}

	@Override
	public void execute() {
		System.out.println("Msg_pong.execute()");
		// TODO Auto-generated method stub
	}

}
