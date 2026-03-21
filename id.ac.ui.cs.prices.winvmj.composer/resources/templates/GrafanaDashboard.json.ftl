{
  "annotations": { "list": [] },
  "editable": true,
  "graphTooltip": 1,
  "schemaVersion": 39,
  "tags": ["monitoring", "winvmj", "generated"],
  "templating": {
    "list": [
      {
        "current": { "selected": true, "text": "All", "value": "$__all" },
        "datasource": { "type": "prometheus", "uid": "prometheus" },
        "definition": "label_values(up, job)",
        "includeAll": true,
        "multi": true,
        "name": "service",
        "query": { "query": "label_values(up, job)" },
        "refresh": 2,
        "type": "query"
      },
      {
        "current": { "selected": true, "text": "All", "value": "$__all" },
        "datasource": { "type": "prometheus", "uid": "prometheus" },
        "definition": "label_values({__name__=~\"http_requests_total|method_calls_total|db_queries_total\"}, feature)",
        "includeAll": true,
        "multi": true,
        "name": "feature",
        "query": { "query": "label_values({__name__=~\"http_requests_total|method_calls_total|db_queries_total\"}, feature)" },
        "refresh": 2,
        "type": "query"
      }
    ]
  },
  "time": { "from": "now-30m", "to": "now" },
  "timezone": "browser",
  "title": "${productName} Monitoring",
  "uid": "${productName?lower_case}-monitoring",
  "panels": [

    {
      "type": "row",
      "title": "HTTP Metrics",
      "gridPos": { "h": 1, "w": 24, "x": 0, "y": 0 },
      "collapsed": false,
      "id": 100
    },
    {
      "type": "timeseries",
      "title": "HTTP Request Rate",
      "gridPos": { "h": 8, "w": 8, "x": 0, "y": 1 },
      "id": 1,
      "datasource": { "type": "prometheus", "uid": "prometheus" },
      "targets": [
        {
          "expr": "sum(rate(http_requests_total{feature=~\"$feature\"}[5m])) by (http_route, http_method)",
          "legendFormat": "{{http_method}} {{http_route}}"
        }
      ],
      "fieldConfig": {
        "defaults": {
          "unit": "reqps",
          "custom": { "drawStyle": "line", "fillOpacity": 10 }
        }
      }
    },
    {
      "type": "timeseries",
      "title": "HTTP Request Duration (p95)",
      "gridPos": { "h": 8, "w": 8, "x": 8, "y": 1 },
      "id": 2,
      "datasource": { "type": "prometheus", "uid": "prometheus" },
      "targets": [
        {
          "expr": "histogram_quantile(0.95, sum(rate(http_request_duration_ms_bucket{feature=~\"$feature\"}[5m])) by (le, http_route))",
          "legendFormat": "p95 {{http_route}}"
        },
        {
          "expr": "histogram_quantile(0.50, sum(rate(http_request_duration_ms_bucket{feature=~\"$feature\"}[5m])) by (le, http_route))",
          "legendFormat": "p50 {{http_route}}"
        }
      ],
      "fieldConfig": {
        "defaults": {
          "unit": "ms",
          "custom": { "drawStyle": "line", "fillOpacity": 10 }
        }
      }
    },
    {
      "type": "timeseries",
      "title": "HTTP Error Rate",
      "gridPos": { "h": 8, "w": 8, "x": 16, "y": 1 },
      "id": 3,
      "datasource": { "type": "prometheus", "uid": "prometheus" },
      "targets": [
        {
          "expr": "sum(rate(http_requests_total{feature=~\"$feature\", http_status_code=~\"4..|5..\"}[5m])) by (http_route, http_status_code)",
          "legendFormat": "{{http_status_code}} {{http_route}}"
        }
      ],
      "fieldConfig": {
        "defaults": {
          "unit": "reqps",
          "custom": { "drawStyle": "bars", "fillOpacity": 50 }
        }
      }
    },

    {
      "type": "row",
      "title": "Method Metrics",
      "gridPos": { "h": 1, "w": 24, "x": 0, "y": 10 },
      "collapsed": false,
      "id": 101
    },
    {
      "type": "timeseries",
      "title": "Method Call Rate",
      "gridPos": { "h": 8, "w": 8, "x": 0, "y": 11 },
      "id": 4,
      "datasource": { "type": "prometheus", "uid": "prometheus" },
      "targets": [
        {
          "expr": "sum(rate(method_calls_total{feature=~\"$feature\"}[5m])) by (class, method)",
          "legendFormat": "{{class}}.{{method}}"
        }
      ],
      "fieldConfig": {
        "defaults": {
          "unit": "ops",
          "custom": { "drawStyle": "line", "fillOpacity": 10 }
        }
      }
    },
    {
      "type": "timeseries",
      "title": "Method Duration (p95)",
      "gridPos": { "h": 8, "w": 8, "x": 8, "y": 11 },
      "id": 5,
      "datasource": { "type": "prometheus", "uid": "prometheus" },
      "targets": [
        {
          "expr": "histogram_quantile(0.95, sum(rate(method_duration_ms_bucket{feature=~\"$feature\"}[5m])) by (le, class, method))",
          "legendFormat": "p95 {{class}}.{{method}}"
        }
      ],
      "fieldConfig": {
        "defaults": {
          "unit": "ms",
          "custom": { "drawStyle": "line", "fillOpacity": 10 }
        }
      }
    },
    {
      "type": "timeseries",
      "title": "Method Error Rate",
      "gridPos": { "h": 8, "w": 8, "x": 16, "y": 11 },
      "id": 6,
      "datasource": { "type": "prometheus", "uid": "prometheus" },
      "targets": [
        {
          "expr": "sum(rate(method_errors_total{feature=~\"$feature\"}[5m])) by (class, method, exception)",
          "legendFormat": "{{class}}.{{method}} [{{exception}}]"
        }
      ],
      "fieldConfig": {
        "defaults": {
          "unit": "ops",
          "custom": { "drawStyle": "bars", "fillOpacity": 50 }
        }
      }
    },

    {
      "type": "row",
      "title": "DB Metrics",
      "gridPos": { "h": 1, "w": 24, "x": 0, "y": 20 },
      "collapsed": false,
      "id": 102
    },
    {
      "type": "timeseries",
      "title": "DB Query Rate",
      "gridPos": { "h": 8, "w": 8, "x": 0, "y": 21 },
      "id": 7,
      "datasource": { "type": "prometheus", "uid": "prometheus" },
      "targets": [
        {
          "expr": "sum(rate(db_queries_total{feature=~\"$feature\"}[5m])) by (db_operation, db_table)",
          "legendFormat": "{{db_operation}} {{db_table}}"
        }
      ],
      "fieldConfig": {
        "defaults": {
          "unit": "ops",
          "custom": { "drawStyle": "line", "fillOpacity": 10 }
        }
      }
    },
    {
      "type": "timeseries",
      "title": "DB Query Duration (p95)",
      "gridPos": { "h": 8, "w": 8, "x": 8, "y": 21 },
      "id": 8,
      "datasource": { "type": "prometheus", "uid": "prometheus" },
      "targets": [
        {
          "expr": "histogram_quantile(0.95, sum(rate(db_query_duration_ms_bucket{feature=~\"$feature\"}[5m])) by (le, db_operation, db_table))",
          "legendFormat": "p95 {{db_operation}} {{db_table}}"
        }
      ],
      "fieldConfig": {
        "defaults": {
          "unit": "ms",
          "custom": { "drawStyle": "line", "fillOpacity": 10 }
        }
      }
    },
    {
      "type": "timeseries",
      "title": "DB Query Error Rate",
      "gridPos": { "h": 8, "w": 8, "x": 16, "y": 21 },
      "id": 9,
      "datasource": { "type": "prometheus", "uid": "prometheus" },
      "targets": [
        {
          "expr": "sum(rate(db_query_errors_total{feature=~\"$feature\"}[5m])) by (db_operation, db_table)",
          "legendFormat": "{{db_operation}} {{db_table}}"
        }
      ],
      "fieldConfig": {
        "defaults": {
          "unit": "ops",
          "custom": { "drawStyle": "bars", "fillOpacity": 50 }
        }
      }
    },

    {
      "type": "row",
      "title": "JVM Metrics",
      "gridPos": { "h": 1, "w": 24, "x": 0, "y": 30 },
      "collapsed": false,
      "id": 103
    },
    {
      "type": "timeseries",
      "title": "JVM Heap Memory",
      "gridPos": { "h": 8, "w": 8, "x": 0, "y": 31 },
      "id": 10,
      "datasource": { "type": "prometheus", "uid": "prometheus" },
      "targets": [
        {
          "expr": "jvm_memory_used_bytes{area=\"heap\"}",
          "legendFormat": "Used {{id}}"
        },
        {
          "expr": "jvm_memory_committed_bytes{area=\"heap\"}",
          "legendFormat": "Committed {{id}}"
        }
      ],
      "fieldConfig": {
        "defaults": {
          "unit": "bytes",
          "custom": { "drawStyle": "line", "fillOpacity": 10 }
        }
      }
    },
    {
      "type": "timeseries",
      "title": "JVM GC Pause Duration",
      "gridPos": { "h": 8, "w": 8, "x": 8, "y": 31 },
      "id": 11,
      "datasource": { "type": "prometheus", "uid": "prometheus" },
      "targets": [
        {
          "expr": "rate(process_runtime_jvm_gc_duration_sum[5m]) / rate(process_runtime_jvm_gc_duration_count[5m])",
          "legendFormat": "Avg GC pause"
        }
      ],
      "fieldConfig": {
        "defaults": {
          "unit": "s",
          "custom": { "drawStyle": "line", "fillOpacity": 10 }
        }
      }
    },
    {
      "type": "timeseries",
      "title": "JVM Threads & CPU",
      "gridPos": { "h": 8, "w": 8, "x": 16, "y": 31 },
      "id": 12,
      "datasource": { "type": "prometheus", "uid": "prometheus" },
      "targets": [
        {
          "expr": "process_runtime_jvm_threads_count",
          "legendFormat": "Thread count"
        },
        {
          "expr": "process_runtime_jvm_cpu_utilization",
          "legendFormat": "CPU utilization"
        }
      ],
      "fieldConfig": {
        "defaults": {
          "custom": { "drawStyle": "line", "fillOpacity": 10 }
        }
      }
    },

    {
      "type": "row",
      "title": "Logs (Loki)",
      "gridPos": { "h": 1, "w": 24, "x": 0, "y": 40 },
      "collapsed": false,
      "id": 104
    },
    {
      "type": "logs",
      "title": "Application Logs",
      "gridPos": { "h": 10, "w": 24, "x": 0, "y": 41 },
      "id": 13,
      "datasource": { "type": "loki", "uid": "loki" },
      "targets": [
        {
          "expr": "{service_name=~\"$service\"} |~ \"$feature\"",
          "refId": "A"
        }
      ],
      "options": {
        "showTime": true,
        "showLabels": true,
        "showCommonLabels": false,
        "wrapLogMessage": true,
        "prettifyLogMessage": false,
        "enableLogDetails": true,
        "sortOrder": "Descending"
      }
    },

    {
      "type": "row",
      "title": "Traces (Tempo)",
      "gridPos": { "h": 1, "w": 24, "x": 0, "y": 52 },
      "collapsed": false,
      "id": 105
    },
    {
      "type": "traces",
      "title": "Trace Search",
      "gridPos": { "h": 10, "w": 24, "x": 0, "y": 53 },
      "id": 14,
      "datasource": { "type": "tempo", "uid": "tempo" },
      "targets": [
        {
          "queryType": "traceqlSearch",
          "filters": [
            { "id": "service-name", "tag": "service.name", "operator": "=", "value": ["$service"], "scope": "resource" },
            { "id": "feature", "tag": "feature", "operator": "=", "value": ["$feature"], "scope": "span" }
          ],
          "limit": 20,
          "refId": "A"
        }
      ]
    }
  ]
}
