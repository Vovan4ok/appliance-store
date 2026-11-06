# Private network: the VM and Cloud SQL have no public IP. Cloud Run joins it through Direct VPC egress.

resource "google_compute_network" "vpc" {
  name                    = "store-vpc"
  auto_create_subnetworks = false
  depends_on              = [google_project_service.apis]
}

resource "google_compute_subnetwork" "main" {
  name          = "store-${var.region}"
  network       = google_compute_network.vpc.id
  region        = var.region
  ip_cidr_range = "10.10.0.0/24"
  # Lets the VM reach Google APIs (Secret Manager, Artifact Registry, Logging) without a public IP
  private_ip_google_access = true
}

# Address range Google uses for Cloud SQL's private IP (private services access)
resource "google_compute_global_address" "private_services" {
  name          = "store-private-services"
  purpose       = "VPC_PEERING"
  address_type  = "INTERNAL"
  prefix_length = 20
  network       = google_compute_network.vpc.id
}

resource "google_service_networking_connection" "private_services" {
  network                 = google_compute_network.vpc.id
  service                 = "servicenetworking.googleapis.com"
  reserved_peering_ranges = [google_compute_global_address.private_services.name]
}

# Outbound internet for the VM (Docker Hub images: Redis, Temporal) without giving it a public IP
resource "google_compute_router" "main" {
  name    = "store-router"
  network = google_compute_network.vpc.id
  region  = var.region
}

resource "google_compute_router_nat" "main" {
  name                               = "store-nat"
  router                             = google_compute_router.main.name
  region                             = var.region
  nat_ip_allocate_option             = "AUTO_ONLY"
  source_subnetwork_ip_ranges_to_nat = "ALL_SUBNETWORKS_ALL_IP_RANGES"

  log_config {
    enable = true
    filter = "ERRORS_ONLY"
  }
}

# Redis and Temporal: only reachable from inside the subnet (Cloud Run instances get addresses from it)
resource "google_compute_firewall" "worker_from_subnet" {
  name          = "store-allow-worker-from-subnet"
  network       = google_compute_network.vpc.id
  direction     = "INGRESS"
  source_ranges = [google_compute_subnetwork.main.ip_cidr_range]
  target_tags   = ["store-worker"]

  allow {
    protocol = "tcp"
    ports    = ["6379", "7233"]
  }
}

# SSH only through Identity-Aware Proxy (Google login + IAM), no open port 22 on the internet
resource "google_compute_firewall" "iap_ssh" {
  name          = "store-allow-iap-ssh"
  network       = google_compute_network.vpc.id
  direction     = "INGRESS"
  source_ranges = ["35.235.240.0/20"]
  target_tags   = ["store-worker"]

  allow {
    protocol = "tcp"
    # 22: SSH; 8233: Temporal Web UI, opened on your machine with gcloud compute start-iap-tunnel
    ports = ["22", "8233"]
  }
}
