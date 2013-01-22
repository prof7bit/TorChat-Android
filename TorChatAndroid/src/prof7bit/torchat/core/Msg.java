package prof7bit.torchat.core;

/*
 * This is the abstract base class for all protocol messages 
 */
abstract class Msg {
	protected Connection connection;
	
	public Msg(Connection connection){
		this.connection = connection;
	}
	
	public abstract void parse(MessageBuffer buf) throws XMessageParseException;
	public abstract MessageBuffer serialize();
	public abstract void execute();
}
