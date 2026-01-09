package id.ac.ui.cs.prices.winvmj.composer.ui.wizards.models.NFRDefinition;

public class Instance {
    private final String instanceType;

    public Instance(String instanceType) {
        this.instanceType = instanceType;
    }

    public String get() {
        return this.instanceType;
    }
}