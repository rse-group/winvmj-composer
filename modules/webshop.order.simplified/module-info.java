module webshop.order.simplified {
	requires webshop.order.core;
    requires webshop.catalog.core;
    requires webshop.customer.core;
    exports webshop.order.simplified;

	requires vmj.routing.route;
	requires vmj.hibernate.integrator;
	requires vmj.auth;
	requires java.logging;
	requires java.naming;

	opens webshop.order.simplified to org.hibernate.orm.core, gson;
}
