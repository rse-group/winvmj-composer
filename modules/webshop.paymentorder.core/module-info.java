module webshop.paymentorder.core {
	exports webshop.paymentorder;
    exports webshop.paymentorder.core;
	requires webshop.order.core;
	requires vmj.routing.route;
	requires vmj.hibernate.integrator;
	requires vmj.auth;
	requires java.logging;
	// https://stackoverflow.com/questions/46488346/error32-13-error-cannot-access-referenceable-class-file-for-javax-naming-re/50568217
	requires java.naming;
    requires org.slf4j;
    requires com.rabbitmq.client;

    opens webshop.paymentorder.core to org.hibernate.orm.core, gson, vmj.hibernate.integrator;
}
