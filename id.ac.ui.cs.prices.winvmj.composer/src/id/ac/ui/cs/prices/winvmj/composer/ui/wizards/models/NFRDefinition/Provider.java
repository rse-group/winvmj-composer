package id.ac.ui.cs.prices.winvmj.composer.ui.wizards.models.NFRDefinition;

public class Provider {
    private final String providerType;

    public Provider(String providerType) {
        this.providerType = providerType;
    }

    public String get() {
        return this.providerType;
    }
}