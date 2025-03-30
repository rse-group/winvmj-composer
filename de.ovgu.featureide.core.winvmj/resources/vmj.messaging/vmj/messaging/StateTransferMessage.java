package vmj.messaging;

import java.util.List;
import java.util.UUID;

public record StateTransferMessage(
        UUID id,
        String type,
        String action,
        String tableName,
        List<Property   > properties
)
{}

