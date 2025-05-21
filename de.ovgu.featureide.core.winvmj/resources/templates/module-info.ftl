module ${productPackage} {
    requires vmj.auth.model;
    requires vmj.routing.route;
    requires vmj.hibernate.integrator;
    
    requires net.bytebuddy;
    requires java.xml.bind;
    requires com.sun.xml.bind;
    requires com.fasterxml.classmate;
    requires jdk.unsupported;

    <#list requiredModules as requiredModule>
    requires ${requiredModule};
    </#list>

    <#list exportedModules as exportedModule>
    exports ${exportedModule};
    </#list>
}