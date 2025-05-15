package vmj.messaging.rabbitmq;

import vmj.messaging.StateTransferMessage;
import vmj.messaging.MessageConsumeException;

public interface MessageConsumer {
	public void consume(StateTransferMessage message) throws MessageConsumeException;
}
