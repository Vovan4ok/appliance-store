# Docker images for both roles
resource "google_artifact_registry_repository" "store" {
  repository_id = "store"
  location      = var.region
  format        = "DOCKER"
  description   = "Appliance store images (web on Cloud Run, worker on the VM)"

  # Keep storage costs flat: the 10 newest images stay, older ones are deleted
  cleanup_policy_dry_run = false
  cleanup_policies {
    id     = "keep-recent"
    action = "KEEP"
    most_recent_versions {
      keep_count = 10
    }
  }
  cleanup_policies {
    id     = "delete-old"
    action = "DELETE"
    condition {
      older_than = "604800s" # 7 days
    }
  }

  depends_on = [google_project_service.apis]
}

# Avatar uploads: Cloud Run's disk is temporary, so the bucket is mounted at /app/uploads instead
resource "google_storage_bucket" "uploads" {
  name                        = "${var.project_id}-uploads"
  location                    = var.region
  uniform_bucket_level_access = true
  public_access_prevention    = "enforced"
  force_destroy               = !var.deletion_protection
}
