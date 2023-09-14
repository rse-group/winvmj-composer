module ${productPackage} {
    requires vmj.routing.route;
    requires vmj.hibernate.integrator;
    
    requires net.bytebuddy;
    requires java.xml.bind;
    requires com.sun.xml.bind;
    requires com.fasterxml.classmate;
    requires jdk.unsupported;

    <#list requiredModules as module>
    requires ${module};
    </#list>
	
	requires prices.auth.vmj;
    requires prices.auth.vmj.model;
}