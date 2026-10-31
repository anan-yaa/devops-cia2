# CI/CD Pipeline: Java → Maven → Docker → ACR → AKS (GitHub Actions + Ansible)

On every push to `main`, GitHub Actions:

1. **Checks out** the code
2. **Compiles, tests and packages** it with Maven (`mvn clean verify`)
3. **Builds and tags** a Docker image (`<acr>.azurecr.io/cicd-demo:<git-sha>` and `:latest`)
4. **Pushes** the image to Azure Container Registry
5. **Deploys/updates** the app on Azure Kubernetes Service using an **Ansible** playbook
6. **Verifies** the rollout and calls the public endpoint

```
Developer ── git push ──► GitHub ──► GitHub Actions
                                       │ 1. checkout
                                       │ 2. mvn clean verify (compile + JUnit tests + package)
                                       │ 3. docker build (tag = commit SHA)
                                       │ 4. docker push ──────────────► Azure Container Registry
                                       │ 5. ansible-playbook deploy.yml ─► AKS (Deployment, 2 pods)
                                       │ 6. kubectl rollout status + curl    │ pulls image from ACR
                                       ▼                                     ▼
                                                       Service (LoadBalancer) ──► public IP ──► Browser
```

## Repo layout

| Path | Purpose |
|---|---|
| `src/main/java/...` | Spring Boot web app (`/`, `/api/info`, `/actuator/health`) |
| `src/test/java/...` | JUnit 5 + MockMvc unit tests |
| `pom.xml` | Maven build |
| `Dockerfile`, `.dockerignore` | Container image (JRE 17, non-root) |
| `.github/workflows/ci-cd.yml` | The pipeline |
| `k8s/` | Namespace, Deployment (2 replicas, probes, rolling update), LoadBalancer Service |
| `ansible/deploy.yml`, `ansible/inventory.ini` | Deployment automation (pull secret, apply manifests, wait, verify) |

## Setup

### 1. Azure resources (run in WSL)
```bash
az login
az provider register -n Microsoft.ContainerService
az provider register -n Microsoft.ContainerRegistry

RG=cicd-rg
LOC=centralindia
AKS=cicd-aks
ACR=ananyaacicd$RANDOM      # must be globally unique
echo "My ACR name is: $ACR" # WRITE THIS DOWN

az group create -n $RG -l $LOC
az acr create -g $RG -n $ACR --sku Basic --admin-enabled true
az aks create -g $RG -n $AKS --node-count 1 --node-vm-size Standard_B2s \
  --tier free --generate-ssh-keys          # takes ~5-10 min

az aks get-credentials -g $RG -n $AKS
kubectl get nodes                          # should show 1 node Ready
```

### 2. GitHub secrets and variable
Repo → Settings → Secrets and variables → Actions

| Type | Name | Value |
|---|---|---|
| Variable | `ACR_LOGIN_SERVER` | `<acr>.azurecr.io` |
| Secret | `ACR_USERNAME` | `az acr credential show -n $ACR --query username -o tsv` |
| Secret | `ACR_PASSWORD` | `az acr credential show -n $ACR --query "passwords[0].value" -o tsv` |
| Secret | `KUBE_CONFIG` | contents of the file from `az aks get-credentials -g $RG -n $AKS --file kubeconfig-ci` |

### 3. Push to `main`
The pipeline runs automatically. The app URL appears in the run summary.

### Change & redeploy demo
Edit `MESSAGE` in `HomeController.java` (`v1` → `v2`), commit and push. The pipeline builds a new image tagged with the new commit SHA, Kubernetes does a rolling update, and the page shows `v2` along with the new tag.

## Security notes
- Registry credentials and the kubeconfig are stored as GitHub encrypted secrets, never in the repo (`kubeconfig*` is git-ignored).
- The pull secret is created by Ansible with `no_log: true`.
- The container runs as a non-root user and has CPU/memory limits.
- Images are tagged with immutable commit SHAs, so every deployment can be traced back to a commit.
- Limitations: ACR admin credentials and a long-lived kubeconfig are used for simplicity. In production you'd use OIDC federated identity plus managed identity / `--attach-acr`, HTTPS ingress, and image scanning.

## Cost control
```bash
az aks stop  -g cicd-rg -n cicd-aks   # when not demoing
az aks start -g cicd-rg -n cicd-aks   # before the viva
az group delete -n cicd-rg            # after grading, deletes everything
```
