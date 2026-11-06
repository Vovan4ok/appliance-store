# GitHub Actions deploys without any stored key: Workload Identity Federation trades the workflow's GitHub OIDC
# token for short-lived Google credentials of the deployer service account.

resource "google_iam_workload_identity_pool" "github" {
  workload_identity_pool_id = "github"
  display_name              = "GitHub Actions"
  depends_on                = [google_project_service.apis]
}

resource "google_iam_workload_identity_pool_provider" "github" {
  workload_identity_pool_id          = google_iam_workload_identity_pool.github.workload_identity_pool_id
  workload_identity_pool_provider_id = "appliance-store"
  display_name                       = "appliance-store repository"

  attribute_mapping = {
    "google.subject"                = "assertion.sub"
    "attribute.repository"          = "assertion.repository"
    "attribute.repository_id"       = "assertion.repository_id"
    "attribute.ref"                 = "assertion.ref"
    "attribute.repository_owner_id" = "assertion.repository_owner_id"
  }

  # Numeric ids, not names: a deleted-and-recreated or renamed repository can't inherit the access.
  # Only pushes to the deploy branches qualify (pull requests and forks get no token for this provider).
  attribute_condition = join(" && ", [
    "assertion.repository_id == '${var.github_repository_id}'",
    "assertion.repository_owner_id == '${var.github_owner_id}'",
    "assertion.ref in [${join(", ", [for b in var.deploy_branches : "'refs/heads/${b}'"])}]",
  ])

  oidc {
    issuer_uri = "https://token.actions.githubusercontent.com"
  }
}

resource "google_service_account" "deployer" {
  account_id   = "store-deployer"
  display_name = "GitHub Actions deployer"
}

resource "google_service_account_iam_member" "deployer_from_github" {
  service_account_id = google_service_account.deployer.name
  role               = "roles/iam.workloadIdentityUser"
  member             = "principalSet://iam.googleapis.com/${google_iam_workload_identity_pool.github.name}/attribute.repository_id/${var.github_repository_id}"
}

# What a deployment does: push the image, roll Cloud Run to it, point the worker VM at it

resource "google_artifact_registry_repository_iam_member" "deployer_push" {
  location   = google_artifact_registry_repository.store.location
  repository = google_artifact_registry_repository.store.name
  role       = "roles/artifactregistry.writer"
  member     = google_service_account.deployer.member
}

resource "google_cloud_run_v2_service_iam_member" "deployer" {
  count    = var.deploy_services ? 1 : 0
  name     = google_cloud_run_v2_service.web[0].name
  location = var.region
  role     = "roles/run.developer"
  member   = google_service_account.deployer.member
}

resource "google_compute_instance_iam_member" "deployer" {
  count         = var.deploy_services ? 1 : 0
  instance_name = google_compute_instance.worker[0].name
  zone          = var.zone
  role          = "roles/compute.instanceAdmin.v1" # setMetadata: the VM's watcher then rolls the worker
  member        = google_service_account.deployer.member
}

# Deploying a revision / changing the VM means acting as their service accounts
resource "google_service_account_iam_member" "deployer_acts_as" {
  for_each = {
    web    = google_service_account.web.name
    worker = google_service_account.worker.name
  }
  service_account_id = each.value
  role               = "roles/iam.serviceAccountUser"
  member             = google_service_account.deployer.member
}

output "github_workload_identity_provider" {
  description = "For .github/workflows/deploy.yml (workload_identity_provider)"
  value       = google_iam_workload_identity_pool_provider.github.name
}

output "github_deployer_service_account" {
  description = "For .github/workflows/deploy.yml (service_account)"
  value       = google_service_account.deployer.email
}
