# Email to the billing account's admins at 50%, 90% and 100% of the monthly budget.
# Credits are excluded, so the alerts show the real cost even while the free trial pays for it.
resource "google_billing_budget" "monthly" {
  billing_account = var.billing_account_id
  display_name    = "appliance-store monthly"

  budget_filter {
    projects               = ["projects/${data.google_project.current.number}"]
    credit_types_treatment = "EXCLUDE_ALL_CREDITS"
  }

  amount {
    specified_amount {
      units = tostring(var.monthly_budget)
    }
  }

  threshold_rules {
    threshold_percent = 0.5
  }
  threshold_rules {
    threshold_percent = 0.9
  }
  threshold_rules {
    threshold_percent = 1.0
  }
  threshold_rules {
    threshold_percent = 1.0
    spend_basis       = "FORECASTED_SPEND"
  }

  depends_on = [google_project_service.apis]
}

# Read after the APIs are on (it needs Cloud Resource Manager, enabled by this same config)
data "google_project" "current" {
  depends_on = [google_project_service.apis]
}

output "web_url" {
  description = "The shop on Cloud Run"
  value       = try(google_cloud_run_v2_service.web[0].uri, "not deployed yet (deploy_services = false)")
}

output "image_repository" {
  description = "Push images here: <repository>/app:<tag>"
  value       = "${local.registry_host}/${var.project_id}/${google_artifact_registry_repository.store.repository_id}"
}

output "worker_internal_ip" {
  description = "Redis (6379) and Temporal (7233) for the web role"
  value       = google_compute_address.worker.address
}

output "database_private_ip" {
  value = google_sql_database_instance.main.private_ip_address
}

output "temporal_ui_tunnel" {
  description = "Run this, then open http://localhost:8233"
  value       = "gcloud compute start-iap-tunnel store-worker 8233 --local-host-port=localhost:8233 --zone ${var.zone}"
}
