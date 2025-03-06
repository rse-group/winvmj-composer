module webshop.product.servicecatalogorder {
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
    requires webshop.catalog.brand;
    requires webshop.catalog.category;
    requires webshop.customer.core;
    requires webshop.order.core;
}