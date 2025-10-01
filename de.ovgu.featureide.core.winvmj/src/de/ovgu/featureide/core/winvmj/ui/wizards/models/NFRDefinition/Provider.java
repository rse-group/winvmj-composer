package de.ovgu.featureide.core.winvmj.ui.wizards.models.NFRDefinition;

public class Provider {
    private final String providerType;

    public Provider(String providerType) {
        this.providerType = providerType;
    }

    public String get() {
        return this.providerType;
    }
}