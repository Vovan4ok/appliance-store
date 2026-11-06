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

## Continuous deployment

`.github/workflows/deploy.yml` runs on every push to `master`:

1. **Test**: `./mvnw verify` (unit + integration tests, checkstyle); nothing is deployed if it fails
2. **Build** one image, tagged with the commit SHA, pushed to Artifact Registry (layer cache in GitHub Actions)
3. **Web**: `gcloud run services update --image …`. Cloud Run starts the new revision, waits for its startup
   probe and only then moves traffic; a revision that doesn't start never receives traffic
4. **Worker**: sets the VM's `app-image` metadata. The VM's `store-image-watcher` service long-polls the metadata
   server, pulls the image and recreates only the worker container (Redis and Temporal keep running).
   CI needs no SSH access to the VM. Log: `sudo journalctl -u store-image-watcher`
5. **Smoke test**: `/` and `/login` answer 200, `/actuator/health` stays private (404)

GitHub authenticates with **Workload Identity Federation**: no key is stored anywhere. Google only accepts tokens
from this repository (checked by numeric repository and owner id, so a renamed or re-created repo gets nothing)
for pushes to the branches in `deploy_branches`. The `store-deployer` service account can only push to this
registry, deploy this Cloud Run service and change this VM's metadata.

Infrastructure changes stay manual on purpose: `terraform plan`, review, `terraform apply`.

## Monitoring

- **Dashboard** "Appliance Store" in Cloud Monitoring (`terraform output dashboard_url`), all PromQL:
  Cloud Run requests/errors/latency/instances (Google's own metrics, free), orders, pending/out-of-stock,
  logins, AI outcomes/time/tokens, heap and DB pools per role, Cloud SQL CPU/connections, worker VM CPU
- **Metrics pipeline** (Google Managed Prometheus, billed per sample, so the `gcp` Spring profile exports only
  ~26 series per role, scraped every 60 s: a few cents a month):
  - web: `collector` sidecar in the Cloud Run instance scrapes the private `:8081` (config: secret `gmp-config`).
    With request-based CPU it scrapes reliably only while requests are served — exactly when the web counters change
  - worker: the Ops Agent on the VM scrapes `localhost:8081` (job `appliance-store-worker`), always on
- **Logs**: Cloud Run logs as usual; the VM's journal (startup script, image watcher, every container) goes to
  Cloud Logging through the Ops Agent (`resource.type="gce_instance"`)
- **Alerts** (email to `alert_email`, set in the uncommitted `local.auto.tfvars`):
  - *Shop is down*: the uptime check of `/login` (every 5 min, several continents) fails from ≥ 2 regions for 10 min
  - *Shop returns server errors*: > 5 % of Cloud Run requests end with 5xx for 10 min
  - *Order worker is not reporting*: no worker metrics for 10 min (reminders and expiry would stop)
  - plus the budget alert (50/90/100 % of `monthly_budget`)

> **Fresh project:** Cloud Monitoring rejects an alert on a metric it has never received. The *Order worker is
> not reporting* alert therefore fails on the very first apply; run `terraform apply` again a few minutes after the
> worker VM is up and its Ops Agent has sent the first metrics.
