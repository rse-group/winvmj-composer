package de.ovgu.featureide.core.winvmj.ui.wizards.models.NFRDefinition;

import de.ovgu.featureide.core.winvmj.ui.wizards.models.NFRDefinition.exception.NFRDefinitionException;
import de.ovgu.featureide.core.winvmj.ui.wizards.models.NFRDefinition.utils.NFRDefinitionUtil;

public class TPS {
    private final String tps;

    public TPS(String tps) {
        if (!NFRDefinitionUtil.isNumber(tps)) {
            throw new NFRDefinitionException("TPS is not a valid number!");
        }
        
        this.tps = tps;
    }

    public String get() {
        return this.tps;
    }
}
