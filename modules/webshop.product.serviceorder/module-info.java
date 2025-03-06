module webshop.product.serviceorder {
    requires vmj.auth.model;
    requires vmj.routing.route;
    requires vmj.hibernate.integrator;
    
    requires net.bytebuddy;
    requires java.xml.bind;
    requires com.sun.xml.bind;
    requires com.fasterxml.classmate;
    requires jdk.unsupported;

    requires webshop.seller.core;
    requires webshop.catalog.core;
    requires webshop.customer.core;
    requires webshop.order.core;
    requires org.slf4j;
    requires com.rabbitmq.client;
}