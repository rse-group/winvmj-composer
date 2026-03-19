module ${productPackage} {
    requires id.ac.ui.cs.prices.winvmj.auth;
    requires id.ac.ui.cs.prices.winvmj.auth.model;
    requires id.ac.ui.cs.prices.winvmj.core;
    requires id.ac.ui.cs.prices.winvmj.hibernate;
    requires org.slf4j;
    
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