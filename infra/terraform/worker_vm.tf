# Always-on VM: Redis, the Temporal server and the app in its worker role (Docker Compose)

resource "google_compute_address" "worker" {
  name         = "store-worker-internal"
  region       = var.region
  subnetwork   = google_compute_subnetwork.main.id
  address_type = "INTERNAL"
  address      = "10.10.0.10"
}

locals {
  registry_host = "${var.region}-docker.pkg.dev"

  worker_compose = templatefile("${path.module}/templates/worker-compose.yml.tftpl", {
    db_host        = google_sql_database_instance.main.private_ip_address
    db_name        = google_sql_database.store.name
    db_user        = google_sql_user.app.name
    db_pool_size   = var.db_pool_size
    reminder_after = "24h"
    expire_after   = "72h"
  })
}

resource "google_compute_instance" "worker" {
  count = var.deploy_services ? 1 : 0

  name         = "store-worker"
  machine_type = var.worker_machine_type
  zone         = var.zone
  tags         = ["store-worker"]

  boot_disk {
    initialize_params {
      image = "debian-cloud/debian-12"
      size  = 20
      type  = "pd-balanced"
    }
  }

  network_interface {
    subnetwork = google_compute_subnetwork.main.id
    network_ip = google_compute_address.worker.address
    # No access_config: no public IP. Outbound traffic goes through Cloud NAT
  }

  service_account {
    email  = google_service_account.worker.email
    scopes = ["cloud-platform"] # what it may actually do is limited by its IAM roles
  }

  shielded_instance_config {
    enable_secure_boot          = true
    enable_vtpm                 = true
    enable_integrity_monitoring = true
  }

  metadata = {
    enable-oslogin = "TRUE"
    app-image      = var.app_image
    worker-compose = local.worker_compose
    startup-script = templatefile("${path.module}/templates/worker-startup.sh.tftpl", {
      project_id    = var.project_id
      registry_host = local.registry_host
    })
  }

  allow_stopping_for_update = true

  lifecycle {
    # Deployments (CI) switch the image; Terraform must not roll it back
    ignore_changes = [metadata["app-image"]]
  }

  depends_on = [
    google_compute_router_nat.main,
    google_secret_manager_secret_version.db_password,
    google_secret_manager_secret_version.redis_password,
    google_secret_manager_secret_version.jwt_secret,
  ]
}
