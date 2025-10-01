package de.ovgu.featureide.core.winvmj.ui.wizards.models.NFRDefinition;

public class Instance {
    private final String instanceType;

    public Instance(String instanceType) {
        this.instanceType = instanceType;
    }

    public String get() {
        return this.instanceType;
    }
}