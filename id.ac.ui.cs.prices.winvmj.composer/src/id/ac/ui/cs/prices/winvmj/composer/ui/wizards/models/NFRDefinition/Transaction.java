package id.ac.ui.cs.prices.winvmj.composer.ui.wizards.models.NFRDefinition;

import id.ac.ui.cs.prices.winvmj.composer.ui.wizards.models.NFRDefinition.exception.NFRDefinitionException;
import id.ac.ui.cs.prices.winvmj.composer.ui.wizards.models.NFRDefinition.utils.NFRDefinitionUtil;

public class Transaction {
    private final String transaction;

    public Transaction (String transaction) {
        if (!NFRDefinitionUtil.isNumber(transaction)) {
            throw new NFRDefinitionException("Transaction is not a valid number!");
        }
        this.transaction = transaction;
    }

    public String get() {
        return this.transaction;
    }
}
