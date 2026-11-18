.RECIPEPREFIX = >
APP := kbot
REGISTRY := ghcr.io/chumachenko-vy
VERSION := $(shell git describe --tags --abbrev=0 2>/dev/null || echo v1.0.0)
TARGETOS ?= linux
TARGETARCH ?= amd64
IMAGE_TAG := $(REGISTRY)/$(APP):$(VERSION)-$(TARGETARCH)

.PHONY: image push linux arm clean

image:
> docker build . --platform $(TARGETOS)/$(TARGETARCH) --build-arg VERSION=$(VERSION) -t $(IMAGE_TAG)

push:
> docker push $(IMAGE_TAG)

linux:
> $(MAKE) image push TARGETARCH=amd64

arm:
> $(MAKE) image push TARGETARCH=arm64

clean:
> -docker rmi $(IMAGE_TAG)
