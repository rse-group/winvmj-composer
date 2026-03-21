# OpenTelemetry Collector Configuration
# Product: ${productName}

receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317
      http:
        endpoint: 0.0.0.0:4318

exporters:
  debug:
    verbosity: basic

  prometheusremotewrite:
    endpoint: http://prometheus:9090/api/v1/write

  otlphttp/tempo:
    endpoint: http://tempo:4318

  otlphttp/loki:
    endpoint: http://loki:3100/otlp

service:
  pipelines:
    metrics:
      receivers: [otlp]
      exporters: [prometheusremotewrite, debug]
    traces:
      receivers: [otlp]
      exporters: [otlphttp/tempo, debug]
    logs:
      receivers: [otlp]
      exporters: [otlphttp/loki, debug]
