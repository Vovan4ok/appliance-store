# Cloud Monitoring dashboard: Google's built-in metrics (Cloud Run, Cloud SQL, VM) next to the app's own metrics
# from Managed Prometheus, all in PromQL. Panels are listed row by row and laid out on a 48-column grid.

locals {
  run    = "monitored_resource=\"cloud_run_revision\",service_name=\"appliance-store\""
  worker = "job=\"appliance-store-worker\""

  # [title, PromQL, kind] — kind: "number" (scorecard) or "chart" (line chart); 4 numbers or 3 charts per row
  dashboard_rows = [
    [
      ["Requests / s", "sum(rate(run_googleapis_com:request_count{${local.run}}[5m]))", "number"],
      ["Server errors (5xx share)", "sum(rate(run_googleapis_com:request_count{${local.run},response_code_class=\"5xx\"}[5m])) / sum(rate(run_googleapis_com:request_count{${local.run}}[5m]))", "number"],
      ["Latency p95 (ms)", "histogram_quantile(0.95, sum by (le) (rate(run_googleapis_com:request_latencies_bucket{${local.run}}[5m])))", "number"],
      ["Pending orders", "max(store_orders_pending{${local.worker}})", "number"],
    ],
    [
      ["Requests / s by status class", "sum by (response_code_class) (rate(run_googleapis_com:request_count{${local.run}}[5m]))", "chart"],
      ["Cloud Run instances", "sum by (state) (run_googleapis_com:container_instance_count{${local.run}})", "chart"],
      ["Latency p50 / p95 (ms)", "label_replace(histogram_quantile(0.5, sum by (le) (rate(run_googleapis_com:request_latencies_bucket{${local.run}}[5m]))), \"q\", \"p50\", \"\", \"\") or label_replace(histogram_quantile(0.95, sum by (le) (rate(run_googleapis_com:request_latencies_bucket{${local.run}}[5m]))), \"q\", \"p95\", \"\", \"\")", "chart"],
    ],
    [
      ["Order events per hour", "sum by (event) (increase(store_orders_total[1h]))", "chart"],
      ["Pending orders / out-of-stock appliances", "label_replace(max(store_orders_pending{${local.worker}}), \"gauge\", \"pending orders\", \"\", \"\") or label_replace(max(store_appliances_out_of_stock{${local.worker}}), \"gauge\", \"out of stock\", \"\", \"\")", "chart"],
      ["Logins per hour", "sum by (result, channel) (increase(store_auth_logins_total[1h]))", "chart"],
    ],
    [
      ["AI descriptions per hour by outcome", "sum by (outcome, reason) (increase(store_ai_descriptions_seconds_count[1h]))", "chart"],
      ["AI average time (s)", "sum by (outcome) (rate(store_ai_descriptions_seconds_sum[1h])) / sum by (outcome) (rate(store_ai_descriptions_seconds_count[1h]))", "chart"],
      ["OpenAI tokens per hour", "sum by (gen_ai_token_type) (increase(gen_ai_client_token_usage_total[1h]))", "chart"],
    ],
    [
      ["Heap used by role", "sum by (job) (jvm_memory_used_bytes{area=\"heap\"})", "chart"],
      ["Database connections in use (app pools)", "sum by (job) (hikaricp_connections_active)", "chart"],
      ["Cloud SQL: CPU and connections", "label_replace(avg(cloudsql_googleapis_com:database_cpu_utilization{monitored_resource=\"cloudsql_database\"}) * 100, \"metric\", \"CPU %\", \"\", \"\") or label_replace(sum(cloudsql_googleapis_com:database_postgresql_num_backends{monitored_resource=\"cloudsql_database\"}), \"metric\", \"connections\", \"\", \"\")", "chart"],
    ],
    [
      ["Worker VM CPU %", "avg(compute_googleapis_com:instance_cpu_utilization{monitored_resource=\"gce_instance\",instance_name=\"store-worker\"}) * 100", "chart"],
      ["Worker reporting (1 = up)", "count(store_orders_pending{${local.worker}})", "chart"],
      ["Rate-limited AI requests per hour", "sum(increase(store_ai_rate_limited_total[1h]))", "chart"],
    ],
  ]

  row_heights = [for row in local.dashboard_rows : row[0][2] == "number" ? 8 : 16]
  row_offsets = [for i, _ in local.dashboard_rows : sum(concat([0], slice(local.row_heights, 0, i)))]

  dashboard_tiles = flatten([
    for r, row in local.dashboard_rows : [
      for c, panel in row : merge(
        # Cloud Monitoring stores zero positions as "absent": omit them, or every plan would show a change
        jsondecode(c > 0 ? jsonencode({ xPos = c * (48 / length(row)) }) : "{}"),
        jsondecode(r > 0 ? jsonencode({ yPos = local.row_offsets[r] }) : "{}"),
        {
          width  = 48 / length(row)
          height = local.row_heights[r]
          # Both branches as JSON strings (HCL needs one type for both sides of ?:), decoded back into objects
          widget = jsondecode(panel[2] == "number" ? jsonencode({
            title = panel[0]
            scorecard = {
              timeSeriesQuery = { prometheusQuery = panel[1] }
              sparkChartView  = { sparkChartType = "SPARK_LINE" }
            }
            }) : jsonencode({
            title = panel[0]
            xyChart = {
              dataSets = [{
                timeSeriesQuery = { prometheusQuery = panel[1] }
                plotType        = "LINE"
                targetAxis      = "Y1" # the API default; stated so the stored JSON matches
              }]
            }
          }))
      })
    ]
  ])
}

resource "google_monitoring_dashboard" "store" {
  dashboard_json = jsonencode({
    displayName  = "Appliance Store"
    mosaicLayout = { columns = 48, tiles = local.dashboard_tiles }
  })
  depends_on = [google_project_service.apis]
}

output "dashboard_url" {
  value = "https://console.cloud.google.com/monitoring/dashboards/builder/${element(split("/", google_monitoring_dashboard.store.id), 3)}?project=${var.project_id}"
}
