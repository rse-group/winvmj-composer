module webshop.product.servicepaymentordercreditcard {
    requires vmj.auth.model;
    requires vmj.routing.route;
    requires vmj.hibernate.integrator;
    
    requires net.bytebuddy;
    requires java.xml.bind;
    requires com.sun.xml.bind;
    requires com.fasterxml.classmate;
    requires jdk.unsupported;

    requires paymentgateway.config.core;
    requires paymentgateway.payment.core;
    requires paymentgateway.payment.creditcard;
    requires webshop.seller.core;
    requires webshop.catalog.core;
    requires webshop.customer.core;
    requires webshop.order.core;
    requires webshop.paymentorder.core;
    requires webshop.paymentorder.creditcard;
}