# One service account per role, each with only what it needs

resource "google_service_account" "web" {
  account_id   = "store-web"
  display_name = "Appliance store web (Cloud Run)"
}

resource "google_service_account" "worker" {
  account_id   = "store-worker"
  display_name = "Appliance store worker VM"
}

locals {
  # The worker sends emails (reminders, expiry) but never calls OpenAI
  web_secrets    = local.all_secrets
  worker_secrets = setsubtract(local.all_secrets, ["openai-api-key"])
}

resource "google_secret_manager_secret_iam_member" "web" {
  for_each  = local.web_secrets
  secret_id = google_secret_manager_secret.all[each.value].id
  role      = "roles/secretmanager.secretAccessor"
  member    = google_service_account.web.member
}

resource "google_secret_manager_secret_iam_member" "worker" {
  for_each  = local.worker_secrets
  secret_id = google_secret_manager_secret.all[each.value].id
  role      = "roles/secretmanager.secretAccessor"
  member    = google_service_account.worker.member
}

# Read/write avatars in the mounted bucket
resource "google_storage_bucket_iam_member" "web_uploads" {
  bucket = google_storage_bucket.uploads.name
  role   = "roles/storage.objectUser"
  member = google_service_account.web.member
}

# The VM pulls the app image itself (Cloud Run uses its own service agent)
resource "google_artifact_registry_repository_iam_member" "worker_pull" {
  location   = google_artifact_registry_repository.store.location
  repository = google_artifact_registry_repository.store.name
  role       = "roles/artifactregistry.reader"
  member     = google_service_account.worker.member
}

# Logs and metrics from both roles
resource "google_project_iam_member" "telemetry" {
  for_each = {
    for pair in setproduct(["web", "worker"], ["roles/logging.logWriter", "roles/monitoring.metricWriter"]) :
    "${pair[0]}-${pair[1]}" => pair
  }
  project = var.project_id
  role    = each.value[1]
  member  = each.value[0] == "web" ? google_service_account.web.member : google_service_account.worker.member
}
