module webshop.product.fullfeature {
    requires vmj.auth.model;
    requires vmj.routing.route;
    requires vmj.hibernate.integrator;
    
    requires net.bytebuddy;
    requires java.xml.bind;
    requires com.sun.xml.bind;
    requires com.fasterxml.classmate;
    requires jdk.unsupported;

    requires paymentgateway.config.core;
    requires paymentgateway.config.flip;
    requires paymentgateway.config.midtrans;
    requires paymentgateway.config.oy;
    requires paymentgateway.payment.core;
    requires paymentgateway.payment.creditcard;
    requires paymentgateway.payment.ewallet;
    requires webshop.seller.core;
    requires webshop.catalog.core;
    requires webshop.catalog.brand;
    requires webshop.catalog.category;
    requires webshop.customer.core;
    requires webshop.order.core;
    requires webshop.order.unauthorized;
    requires webshop.order.simplified;
    requires webshop.paymentorder.core;
    requires webshop.paymentorder.creditcard;
    requires webshop.paymentorder.ewallet;

}