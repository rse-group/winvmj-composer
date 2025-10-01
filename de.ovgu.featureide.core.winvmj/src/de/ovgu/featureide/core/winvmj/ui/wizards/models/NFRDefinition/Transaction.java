package de.ovgu.featureide.core.winvmj.ui.wizards.models.NFRDefinition;

import de.ovgu.featureide.core.winvmj.ui.wizards.models.NFRDefinition.exception.NFRDefinitionException;
import de.ovgu.featureide.core.winvmj.ui.wizards.models.NFRDefinition.utils.NFRDefinitionUtil;

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
