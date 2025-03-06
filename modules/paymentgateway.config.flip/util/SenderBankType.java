package paymentgateway.config.flip;

public enum SenderBankType {
    EWALLET("wallet_account"),
    VIRTUALACCOUNT("virtual_account");

    private final String value;

    SenderBankType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
