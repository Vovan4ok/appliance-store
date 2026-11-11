variable "project_id" {
  description = "GCP project that hosts everything"
  type        = string
}

variable "region" {
  description = "Region for Cloud Run, Cloud SQL, the VM, the registry and the bucket"
  type        = string
}

variable "zone" {
  description = "Zone for the worker VM"
  type        = string
}

variable "billing_account_id" {
  description = "Billing account the budget alert watches (gcloud billing projects describe <project>)"
  type        = string
}

variable "deploy_services" {
  description = <<-EOT
    Create the Cloud Run service and the worker VM. The first apply runs with false: it creates the network,
    database, registry and empty secrets, so the secret values and the first image can be added before anything
    tries to start.
  EOT
  type        = bool
  default     = true
}

variable "app_image" {
  description = <<-EOT
    Image both roles run, e.g. europe-central2-docker.pkg.dev/<project>/store/app:<git-sha>. Only used when a
    service is created: later deployments (CI) change the image directly and Terraform leaves it alone.
  EOT
  type        = string
  default     = ""
}

variable "ai_chat_provider" {
  description = "openai turns the AI description generator on (needs the openai-api-key secret), none turns it off"
  type        = string
  default     = "openai"
}

variable "recommendations_ai" {
  description = "AI re-ranking of the widget's recommendations (needs ai_chat_provider = openai). Cached per product and language, capped at recommendations_ai_calls_per_minute LLM calls; any failure falls back to the scoring order"
  type        = bool
  default     = true
}

variable "recommendations_ai_calls_per_minute" {
  description = "Global cap on LLM calls for the recommendation re-ranking, across all Cloud Run instances"
  type        = number
  default     = 6
}

variable "demo_read_only_accounts" {
  description = "Logins that can see everything but change nothing: the sample accounts, whose passwords are public in the README. Keep your own employee account out of this list"
  type        = list(string)
  default     = ["phobos@gmail.com", "moon@gmail.com", "mercury@gmail.com", "jupiter@gmail.com"]
}

variable "web_max_instances" {
  description = "Upper limit for Cloud Run instances (each holds up to db_pool_size database connections)"
  type        = number
  default     = 3
}

variable "db_pool_size" {
  description = "Database connections per app instance; db-f1-micro allows 25 in total"
  type        = number
  default     = 5
}

variable "db_tier" {
  description = "Cloud SQL machine type; db-f1-micro is the smallest (shared CPU, 0.6 GB)"
  type        = string
  default     = "db-f1-micro"
}

variable "worker_machine_type" {
  description = "VM for Redis, the Temporal server and the app's worker role"
  type        = string
  default     = "e2-medium"
}

variable "deletion_protection" {
  description = "Protect the database and the Cloud Run service from terraform destroy (false for a demo you tear down)"
  type        = bool
  default     = false
}

variable "monthly_budget" {
  description = "Budget alert in the billing account's currency; emails at 50%, 90% and 100% of it"
  type        = number
  default     = 50
}

variable "secrets_version" {
  description = "Bump to rotate the generated passwords (database, Redis, JWT signing key)"
  type        = number
  default     = 1
}

variable "github_repository_id" {
  description = "Numeric id of the GitHub repository allowed to deploy (api.github.com/repos/<owner>/<repo> → id)"
  type        = string
}

variable "github_owner_id" {
  description = "Numeric id of the repository owner (→ owner.id)"
  type        = string
}

variable "deploy_branches" {
  description = "Branches whose pushes may deploy"
  type        = list(string)
  default     = ["master"]
}

variable "alert_email" {
  description = "Where monitoring alerts go. Kept out of git: set it in local.auto.tfvars (ignored) or TF_VAR_alert_email"
  type        = string
}
