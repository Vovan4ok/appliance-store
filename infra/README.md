# GCP deployment

```
Internet ─► Cloud Run "appliance-store" (web role, HTTPS, 1–3 instances, metrics/health on private :8081)
               │ Direct VPC egress (private ranges only)
               ├─► Cloud SQL PostgreSQL 16 (private IP, SSL only, daily backups)
               ├─► VM store-worker (private IP 10.10.0.10): Redis · Temporal server · app in worker role
               └─► Cloud Storage bucket <project>-uploads (avatars, mounted at /app/uploads)
Secrets: Secret Manager · Images: Artifact Registry (europe-central2-docker.pkg.dev/<project>/store/app)
```

Everything is described in `terraform/`. Rough cost: ~$40/month (VM ~$25, Cloud SQL ~$10, Cloud Run ~$5).
A budget alert emails you at 50 %, 90 % and 100 % of `monthly_budget` (real cost, free-trial credits excluded).

## Secrets

| Secret | Who sets it |
|---|---|
| `db-password`, `redis-password`, `jwt-secret` | Terraform generates them on the first apply. They are written with *write-only* attributes, so they are in Secret Manager but **never in the Terraform state**. Rotate: bump `secrets_version` |
| `openai-api-key`, `mail-username`, `mail-password`, `mail-from` | You: `infra/scripts/set-secrets.ps1` copies them from your local `.env` |

All email goes to the `mail-from` inbox (`MAIL_REDIRECT_TO`): the sample accounts use strangers' real addresses.

## First-time setup

Prerequisites: `gcloud` and `terraform` installed, `gcloud auth login` and `gcloud auth application-default login` done.

```powershell
# 1. Bucket for the Terraform state (once)
gcloud storage buckets create gs://appliance-store-513822-tfstate --location=europe-central2 --uniform-bucket-level-access --public-access-prevention
gcloud storage buckets update gs://appliance-store-513822-tfstate --versioning

cd infra/terraform
terraform init

# 2. Foundation first: network, database, registry, bucket, empty secrets (no services yet)
terraform apply -var="deploy_services=false"

# 3. Your private values into Secret Manager (from .env)
..\scripts\set-secrets.ps1

# 4. First image
gcloud auth configure-docker europe-central2-docker.pkg.dev
$tag = git rev-parse --short HEAD
docker build -t europe-central2-docker.pkg.dev/appliance-store-513822/store/app:$tag ../..
docker push europe-central2-docker.pkg.dev/appliance-store-513822/store/app:$tag

# 5. Services: Cloud Run (web) and the worker VM
terraform apply -var="app_image=europe-central2-docker.pkg.dev/appliance-store-513822/store/app:$tag"
terraform output web_url
```

## Day to day

- **Temporal Web UI**: `terraform output -raw temporal_ui_tunnel` prints the IAP tunnel command, then open http://localhost:8233
- **SSH to the worker VM** (IAP, no public port): `gcloud compute ssh store-worker --zone europe-central2-a --tunnel-through-iap`;
  startup log: `sudo journalctl -t store-startup`; containers: `sudo docker compose -f /opt/store/docker-compose.yml ps`
- **Re-run the VM setup** (e.g. after changing a secret): `sudo google_metadata_script_runner startup` on the VM
- **Stop paying**: `terraform destroy` removes everything (with `deletion_protection = false`, the default)
