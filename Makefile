# Makefile for Keycloak User Storage SPI
IMAGE_ORG ?= harbor.cnapcloud.com/library
IMAGE_REPOSITORY ?= ${IMAGE_ORG}/keycloak-user-storage
IMAGE_TAG ?= $(shell git rev-parse --short=7 HEAD)

# Gitops vairables
GITOPS_REPO := https://github.com/cnapcloud/gitops.git
GITOPS_PATH := tmp/gitops
GIT_BRANCH ?= $(shell git rev-parse --abbrev-ref HEAD)

help: ## print target list
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

build:
	./gradlew clean build -x test --no-build-cache
	
report: ## make test report and send the result	
	
docker-build: ## Build the Docker image using BuildKit
	@echo "[docker-build] Build the Docker image"
	buildctl --addr tcp://buildkitd.cicd.svc:1234 build \
	--frontend dockerfile.v0 \
	--local context=. \
	--local dockerfile=. \
	--output type=image,name=$(IMAGE_REPOSITORY):$(IMAGE_TAG),push=true
	clean
	
update-tag: ## Update the image tag in GitOps repository
	@echo "[update-tag] Update the image tag in GitOps repo."
	update-tag.sh \
	--repo $(GITOPS_REPO) \
	--branch main \
	--image $(IMAGE_REPOSITORY):$(IMAGE_TAG)

# Build and push multi-arch Docker image to Docker Hub
# make docker-push IMAGE_ORG=cnapcloud IMAGE_TAG=0.0.1
push: ## push to docker hub
	export BUILDX_NO_DEFAULT_ATTESTATIONS=1 && \
	export DOCKER_BUILDKIT=1 && \
	docker buildx build --platform linux/amd64,linux/arm64 -t $(IMAGE_REPOSITORY):$(IMAGE_TAG) --push .

clean: ## Clean up Docker builder cache
	@echo "[clean] Clean up Docker builder cache."
	buildctl --addr tcp://buildkitd.cicd.svc:1234 prune --all --force

.PHONY: help build report docker-build-multiarch docker-build docker-build-local docker-push clean