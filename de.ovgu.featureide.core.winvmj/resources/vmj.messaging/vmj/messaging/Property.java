package vmj.messaging;

public record Property (
        String varName,
        String type,
        Object value
){}