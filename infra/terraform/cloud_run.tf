# The shop itself (web role): public HTTPS, scales 1..N, talks to the private network for DB/Redis/Temporal

locals {
  db_url = "jdbc:postgresql://${google_sql_database_instance.main.private_ip_address}:5432/${google_sql_database.store.name}?sslmode=require"

  web_env = {
    SPRING_PROFILES_ACTIVE  = "prod,gcp" # gcp: export only the metrics the dashboard and alerts use
    DB_URL                  = local.db_url
    DB_USERNAME             = google_sql_user.app.name
    DB_POOL_SIZE            = tostring(var.db_pool_size)
    REDIS_HOST              = google_compute_address.worker.address
    TEMPORAL_ADDRESS        = "${google_compute_address.worker.address}:7233"
    TEMPORAL_WORKER_ENABLED = "false" # the VM runs the worker; Cloud Run only starts/approves workflows
    COOKIE_SECURE           = "true"
    # Tomcat's RemoteIpValve: trusts X-Forwarded-For/-Proto only from internal addresses (Cloud Run's front end
    # connects from 169.254.x.x) and takes the client IP from the RIGHT end of X-Forwarded-For, which Google
    # appends. "framework" took the LEFT end, which any client can fake (bypassing per-IP rate limits)
    FORWARD_HEADERS_STRATEGY = "native"
    UPLOAD_DIR               = "/app/uploads"
    MAIL_HOST                = "smtp-relay.brevo.com"
    MAIL_PORT                = "587"
    MAIL_SMTP_AUTH           = "true"
    MAIL_SMTP_STARTTLS       = "true"
    AI_CHAT_PROVIDER         = var.ai_chat_provider
    DEMO_READ_ONLY_ACCOUNTS  = join(",", var.demo_read_only_accounts)
  }

  web_secret_env = {
    DB_PASSWORD    = "db-password"
    REDIS_PASSWORD = "redis-password"
    JWT_SECRET     = "jwt-secret"
    OPENAI_API_KEY = "openai-api-key"
    MAIL_USERNAME  = "mail-username"
    MAIL_PASSWORD  = "mail-password"
    MAIL_FROM      = "mail-from"
    # Sample accounts use strangers' real addresses: every email goes to your own (sender) inbox
    MAIL_REDIRECT_TO = "mail-from"
  }
}

resource "google_cloud_run_v2_service" "web" {
  count = var.deploy_services ? 1 : 0

  name                = "appliance-store"
  location            = var.region
  ingress             = "INGRESS_TRAFFIC_ALL"
  deletion_protection = var.deletion_protection
  # Public website: no IAM check for callers (avoids an allUsers binding)
  invoker_iam_disabled = true

  template {
    service_account                  = google_service_account.web.email
    execution_environment            = "EXECUTION_ENVIRONMENT_GEN2" # needed for the Cloud Storage volume
    timeout                          = "60s"
    max_instance_request_concurrency = 40

    scaling {
      # One warm instance: Spring Boot needs ~20 s to start, nobody should wait for that
      min_instance_count = 1
      max_instance_count = var.web_max_instances
    }

    vpc_access {
      network_interfaces {
        network    = google_compute_network.vpc.id
        subnetwork = google_compute_subnetwork.main.id
      }
      # Only private addresses (DB, VM) go through the VPC; OpenAI and Brevo go out directly
      egress = "PRIVATE_RANGES_ONLY"
    }

    volumes {
      name = "uploads"
      gcs {
        bucket    = google_storage_bucket.uploads.name
        read_only = false
        # Files owned by the app's non-root user (uid 10001 in the Dockerfile)
        mount_options = ["uid=10001", "gid=10001", "implicit-dirs"]
      }
    }

    containers {
      name  = "app"
      image = var.app_image

      ports {
        container_port = 8080
      }

      resources {
        limits = {
          cpu    = "1"
          memory = "1Gi"
        }
        cpu_idle          = true # CPU only while handling requests: billed per request, not per hour
        startup_cpu_boost = true
      }

      dynamic "env" {
        for_each = local.web_env
        content {
          name  = env.key
          value = env.value
        }
      }

      dynamic "env" {
        for_each = local.web_secret_env
        content {
          name = env.key
          value_source {
            secret_key_ref {
              secret  = google_secret_manager_secret.all[env.value].secret_id
              version = "latest"
            }
          }
        }
      }

      volume_mounts {
        name       = "uploads"
        mount_path = "/app/uploads"
      }

      # Health lives on the management port 8081, which is never reachable from the internet
      startup_probe {
        http_get {
          path = "/actuator/health/readiness"
          port = 8081
        }
        initial_delay_seconds = 10
        period_seconds        = 5
        timeout_seconds       = 3
        failure_threshold     = 30
      }

      liveness_probe {
        http_get {
          path = "/actuator/health/liveness"
          port = 8081
        }
        period_seconds    = 30
        timeout_seconds   = 5
        failure_threshold = 3
      }
    }

    # Managed Prometheus collector: scrapes the app's private :8081 from inside the instance and writes to Cloud
    # Monitoring. With request-based CPU it scrapes reliably only while requests are served; the counters it reads
    # only change then, and always-on numbers come from the worker VM and Cloud Run's own metrics.
    containers {
      name       = "collector"
      image      = "us-docker.pkg.dev/cloud-ops-agents-artifacts/cloud-run-gmp-sidecar/cloud-run-gmp-sidecar:1.2.0"
      depends_on = ["app"]

      resources {
        limits = {
          cpu    = "0.5"
          memory = "256Mi"
        }
        cpu_idle = true
        # gcloud deployments set the boost on every container; declared here so Terraform and CI agree
        startup_cpu_boost = true
      }

      volume_mounts {
        name       = "gmp-config"
        mount_path = "/etc/rungmp/"
      }
    }

    volumes {
      name = "gmp-config"
      secret {
        secret = google_secret_manager_secret.gmp_config.secret_id
        items {
          version = "latest"
          path    = "config.yaml"
        }
      }
    }
  }

  lifecycle {
    # Deployments (CI) switch the image; Terraform must not roll it back
    ignore_changes = [template[0].containers[0].image, client, client_version]
  }

  depends_on = [
    google_secret_manager_secret_iam_member.web,
    google_storage_bucket_iam_member.web_uploads,
    google_secret_manager_secret_version.db_password,
    google_secret_manager_secret_version.redis_password,
    google_secret_manager_secret_version.jwt_secret,
  ]
}
