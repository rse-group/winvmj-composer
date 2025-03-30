module vmj.messaging {
	exports vmj.messaging;
    exports vmj.messaging.rabbitmq;
	
    requires com.rabbitmq.client;
    requires vmj.hibernate.integrator;
    requires gson;
}