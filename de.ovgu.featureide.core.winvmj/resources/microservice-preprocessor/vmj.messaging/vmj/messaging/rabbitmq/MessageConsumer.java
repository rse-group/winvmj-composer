package vmj.messaging.rabbitmq;

import vmj.messaging.StateTransferMessage;

public interface MessageConsumer {
    public void consume(StateTransferMessage message);
}
