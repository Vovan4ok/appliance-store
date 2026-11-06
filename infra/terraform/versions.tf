terraform {
  # 1.11+: write-only attributes and ephemeral resources keep generated passwords out of the state file
  required_version = ">= 1.11"

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 8.10"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.9"
    }
  }

  # State lives in a versioned, private bucket (created once by hand, see infra/README.md):
  # every apply is recorded and an older state can be restored
  backend "gcs" {
    bucket = "appliance-store-513822-tfstate"
    prefix = "terraform/state"
  }
}

provider "google" {
  project = var.project_id
  region  = var.region
  zone    = var.zone

  # The budget API is billed to (and quota-checked against) this project when using user credentials
  billing_project       = var.project_id
  user_project_override = true
}
