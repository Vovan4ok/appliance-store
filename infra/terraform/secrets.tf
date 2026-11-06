# ── Generated secrets ──
# Ephemeral passwords are generated during apply and written with write-only attributes: they end up in Secret
# Manager (and the database), but never in the Terraform state or plan. Bump var.secrets_version to rotate them.

ephemeral "random_password" "db" {
  length  = 32
  special = false # plain alphanumerics: safe inside JDBC URLs, .env files and shell scripts
}

ephemeral "random_password" "redis" {
  length  = 32
  special = false
}

ephemeral "random_password" "jwt" {
  length  = 64 # the app requires at least 32 bytes
  special = false
}

locals {
  generated_secrets = toset(["db-password", "redis-password", "jwt-secret"])

  # Secrets only you know: values are added by infra/scripts/set-secrets.ps1 from your local .env
  external_secrets = toset(["openai-api-key", "mail-username", "mail-password", "mail-from"])

  all_secrets = setunion(local.generated_secrets, local.external_secrets)
}

resource "google_secret_manager_secret" "all" {
  for_each  = local.all_secrets
  secret_id = each.value

  replication {
    user_managed {
      replicas {
        location = var.region
      }
    }
  }
  depends_on = [google_project_service.apis]
}

resource "google_secret_manager_secret_version" "db_password" {
  secret                 = google_secret_manager_secret.all["db-password"].id
  secret_data_wo         = ephemeral.random_password.db.result
  secret_data_wo_version = var.secrets_version
}

resource "google_secret_manager_secret_version" "redis_password" {
  secret                 = google_secret_manager_secret.all["redis-password"].id
  secret_data_wo         = ephemeral.random_password.redis.result
  secret_data_wo_version = var.secrets_version
}

resource "google_secret_manager_secret_version" "jwt_secret" {
  secret                 = google_secret_manager_secret.all["jwt-secret"].id
  secret_data_wo         = ephemeral.random_password.jwt.result
  secret_data_wo_version = var.secrets_version
}
