module webshop.order.core {
	exports webshop.order;
    exports webshop.order.core;
    requires webshop.catalog.core;
    requires webshop.customer.core;
	requires vmj.routing.route;
	requires vmj.hibernate.integrator;
	requires vmj.auth;
	requires java.logging;
	// https://stackoverflow.com/questions/46488346/error32-13-error-cannot-access-referenceable-class-file-for-javax-naming-re/50568217
	requires java.naming;
    requires org.slf4j;
    requires com.rabbitmq.client;

    opens webshop.order.core to org.hibernate.orm.core, gson, vmj.hibernate.integrator;
}
