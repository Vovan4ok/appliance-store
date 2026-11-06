# Not secret: ids and sizes only. Secret values never go into Terraform files (see infra/README.md).
project_id         = "appliance-store-513822"
region             = "europe-central2"
zone               = "europe-central2-a"
billing_account_id = "01DB6C-7CC8E8-A35ED7"

# GitHub repository allowed to deploy (Vovan4ok/appliance-store); numeric ids survive renames
github_repository_id = "1217879765"
github_owner_id      = "90390510"
# gcp-deployment only while that branch is being finished; remove it after merging into master
deploy_branches = ["master", "gcp-deployment"]
