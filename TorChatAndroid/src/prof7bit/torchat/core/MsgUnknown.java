package prof7bit.torchat.core;

/**
 * This class handles all incoming protocol messages with unknown command.
 * It will answer with "not_implemented" and otherwise do nothing. 
 *
 * @author Bernd Kreuss <prof7bit@gmail.com>
 *
 */
public class MsgUnknown extends Msg {

	public MsgUnknown(Connection connection) {
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
		System.out.println("MsgUnknown.execute()");
		// TODO Auto-generated method stub
	}

}
