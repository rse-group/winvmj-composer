package vmj.messaging;

import java.util.List;
import java.util.UUID;

public record StateTransferMessage(
        Object id, // UUID or int
        String type,
        String action,
        String tableName,
        List<Property   > properties
)
{}

