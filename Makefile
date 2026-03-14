# Makefile for Keycloak User Storage SPI
IMAGE_ORG := docker.io/cnapcloud
IMAGE_REPOSITORY := ${IMAGE_ORG}/keycloak-user-storage
IMAGE_VERSION := $(shell git rev-parse --short=7 HEAD)

# Gitops vairables
GITOPS_REPO := http://gitlab.internal/msa/gitops.git
GITOPS_PATH := tmp/gitops
GIT_BRANCH ?= $(shell git rev-parse --abbrev-ref HEAD)


help: ## print target list
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

build:
	./gradlew clean build --no-build-cache
	
report: ## make test report and send the result	

docker-build-mult: ## push docker image with multi-arch support
	export BUILDX_NO_DEFAULT_ATTESTATIONS=1 && \
	export DOCKER_BUILDKIT=1 && \
	docker buildx build --platform linux/amd64,linux/arm64 -t $(IMAGE_REPOSITORY):$(IMAGE_VERSION) --push .
	clean

docker-build: build ## build docker image
	export BUILDX_NO_DEFAULT_ATTESTATIONS=1 && \
	export DOCKER_BUILDKIT=1 && \
	docker buildx build -t $(IMAGE_REPOSITORY):$(IMAGE_VERSION) --load .
	make clean

docker-push: ## push docker image
	docker push $(IMAGE_REPOSITORY):$(IMAGE_VERSION)

clean:
	yes| docker builder prune
	

.PHONY: help build report docker-build-multiarch docker-build docker-push clean