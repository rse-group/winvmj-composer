module webshop.paymentorder.creditcard {
	requires webshop.paymentorder.core;
	requires webshop.order.core;
	exports webshop.paymentorder.creditcard;
	requires paymentgateway.payment.core;
	requires paymentgateway.payment.creditcard; //tambahan ruzain
	requires vmj.routing.route;
	requires vmj.hibernate.integrator;
	requires vmj.auth;
	requires java.logging;
	requires java.naming;
	
	opens webshop.paymentorder.creditcard to org.hibernate.orm.core, gson, vmj.hibernate.integrator;
}
