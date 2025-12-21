module vmj.messaging {
	exports vmj.messaging;
    exports vmj.messaging.rabbitmq;
	
    requires com.rabbitmq.client;
    requires id.ac.ui.cs.prices.winvmj.hibernate;
    requires gson;
    
    opens vmj.messaging to gson;
}