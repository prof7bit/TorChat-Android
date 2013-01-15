package prof7bit.torchat.core;

/**
 * This class handles the protocol message "not_implemented".
 *
 * @author Bernd Kreuss <prof7bit@gmail.com>
 *
 */
public class Msg_not_implemented extends Msg {

	public Msg_not_implemented(Connection connection) {
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
		System.out.println("Msg_not_implemented.execute()");
		// don't do anything, just eat and ignore.
		// TODO maybe should log it
	}

}
