# ── Metrics collection config for the Cloud Run sidecar (not secret: Cloud Run mounts files from Secret Manager) ──
resource "google_secret_manager_secret" "gmp_config" {
  secret_id = "gmp-config"
  replication {
    user_managed {
      replicas {
        location = var.region
      }
    }
  }
  depends_on = [google_project_service.apis]
}

resource "google_secret_manager_secret_version" "gmp_config" {
  secret = google_secret_manager_secret.gmp_config.id
  secret_data = yamlencode({
    apiVersion = "monitoring.googleapis.com/v1beta"
    kind       = "RunMonitoring"
    metadata   = { name = "appliance-store-web" } # becomes the job label
    spec = {
      endpoints = [{
        port     = 8081
        path     = "/actuator/prometheus"
        interval = "60s" # billed per sample: once a minute is plenty for these charts
      }]
      targetLabels = { metadata = ["service", "revision"] }
    }
  })
}

resource "google_secret_manager_secret_iam_member" "web_gmp_config" {
  secret_id = google_secret_manager_secret.gmp_config.id
  role      = "roles/secretmanager.secretAccessor"
  member    = google_service_account.web.member
}

# ── Who gets the alert emails (set alert_email in local.auto.tfvars, which is not committed) ──
resource "google_monitoring_notification_channel" "email" {
  display_name = "Appliance store alerts"
  type         = "email"
  labels = {
    email_address = var.alert_email
  }
  depends_on = [google_project_service.apis]
}

# ── Uptime: the login page, checked every 5 minutes from several continents ──
resource "google_monitoring_uptime_check_config" "web" {
  count        = var.deploy_services ? 1 : 0
  display_name = "appliance-store /login"
  timeout      = "10s"
  period       = "300s"

  http_check {
    path           = "/login"
    port           = 443
    use_ssl        = true
    validate_ssl   = true
    request_method = "GET"
    accepted_response_status_codes {
      status_class = "STATUS_CLASS_2XX"
    }
  }

  monitored_resource {
    type = "uptime_url"
    labels = {
      project_id = var.project_id
      host       = trimprefix(google_cloud_run_v2_service.web[0].uri, "https://")
    }
  }

  content_matchers {
    content = "password" # the login form is really rendered, not just some 200 page
    matcher = "CONTAINS_STRING"
  }
}

locals {
  alert_doc_footer = "Runbook: infra/README.md. Logs: Cloud Logging (Cloud Run service appliance-store, VM store-worker)."
}

resource "google_monitoring_alert_policy" "uptime" {
  count                 = var.deploy_services ? 1 : 0
  display_name          = "Shop is down"
  combiner              = "OR"
  notification_channels = [google_monitoring_notification_channel.email.id]

  conditions {
    display_name = "Login page fails from at least 2 locations"
    condition_threshold {
      filter          = "metric.type=\"monitoring.googleapis.com/uptime_check/check_passed\" AND metric.label.check_id=\"${google_monitoring_uptime_check_config.web[0].uptime_check_id}\" AND resource.type=\"uptime_url\""
      comparison      = "COMPARISON_GT"
      threshold_value = 1
      duration        = "600s"
      aggregations {
        alignment_period     = "1200s"
        per_series_aligner   = "ALIGN_NEXT_OLDER"
        cross_series_reducer = "REDUCE_COUNT_FALSE"
        group_by_fields      = ["resource.label.*"]
      }
    }
  }

  documentation {
    content   = "The login page has failed the uptime check from at least 2 regions for 10 minutes. ${local.alert_doc_footer}"
    mime_type = "text/markdown"
  }
}

resource "google_monitoring_alert_policy" "server_errors" {
  display_name          = "Shop returns server errors"
  combiner              = "OR"
  notification_channels = [google_monitoring_notification_channel.email.id]

  conditions {
    display_name = "More than 5 % of requests end with 5xx for 10 minutes"
    condition_prometheus_query_language {
      query               = <<-EOT
        sum(rate(run_googleapis_com:request_count{monitored_resource="cloud_run_revision",service_name="appliance-store",response_code_class="5xx"}[5m]))
          / sum(rate(run_googleapis_com:request_count{monitored_resource="cloud_run_revision",service_name="appliance-store"}[5m]))
          > 0.05
      EOT
      duration            = "600s"
      evaluation_interval = "60s"
    }
  }

  documentation {
    content   = "Over 5 % of requests to Cloud Run failed with a 5xx status. ${local.alert_doc_footer}"
    mime_type = "text/markdown"
  }
}

resource "google_monitoring_alert_policy" "worker_down" {
  display_name          = "Order worker is not reporting"
  combiner              = "OR"
  notification_channels = [google_monitoring_notification_channel.email.id]

  conditions {
    display_name = "No metrics from the worker for 10 minutes"
    condition_prometheus_query_language {
      # The worker exports this gauge on every scrape; if it disappears, the worker (or the VM) is down and
      # nobody runs the approval workflows: reminders and expiry stop
      query               = "absent(store_orders_pending{job=\"appliance-store-worker\"})"
      duration            = "600s"
      evaluation_interval = "60s"
    }
  }

  documentation {
    content   = "The worker on store-worker stopped reporting: order reminders and expiry are not running. Check: gcloud compute ssh store-worker --zone ${var.zone} --tunnel-through-iap, then sudo docker compose -f /opt/store/docker-compose.yml ps. ${local.alert_doc_footer}"
    mime_type = "text/markdown"
  }
}
