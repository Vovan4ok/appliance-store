# PostgreSQL 16 on Cloud SQL: private IP only, encrypted connections only, daily backups

resource "google_sql_database_instance" "main" {
  name                = "store-postgres"
  database_version    = "POSTGRES_16"
  region              = var.region
  deletion_protection = var.deletion_protection

  settings {
    # Shared-core tiers (db-f1-micro) exist only in the Enterprise edition
    edition           = "ENTERPRISE"
    tier              = var.db_tier
    availability_type = "ZONAL"
    disk_type         = "PD_SSD"
    disk_size         = 10
    disk_autoresize   = true

    ip_configuration {
      ipv4_enabled    = false
      private_network = google_compute_network.vpc.id
      ssl_mode        = "ENCRYPTED_ONLY"
    }

    backup_configuration {
      enabled    = true
      start_time = "02:00"
      backup_retention_settings {
        retained_backups = 7
      }
    }

    maintenance_window {
      day  = 7 # Sunday
      hour = 3
    }

    insights_config {
      query_insights_enabled = true
    }
  }

  depends_on = [google_service_networking_connection.private_services]
}

resource "google_sql_database" "store" {
  name     = "appliance_store"
  instance = google_sql_database_instance.main.name
}

resource "google_sql_user" "app" {
  name     = "appuser"
  instance = google_sql_database_instance.main.name
  # Same generated password as the db-password secret, never stored in state
  password_wo         = ephemeral.random_password.db.result
  password_wo_version = var.secrets_version
}
