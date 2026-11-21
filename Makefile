.RECIPEPREFIX = >
APP        := kbot
REGISTRY   ?= ghcr.io
REPOSITORY ?= chumachenko-vy/$(APP)
VERSION    := $(shell git describe --tags --abbrev=0 2>/dev/null || echo v1.0.0)-$(shell git rev-parse --short HEAD)
TARGETOS   ?= linux
TARGETARCH ?= amd64
IMAGE_TAG  := $(REGISTRY)/$(REPOSITORY):$(VERSION)-$(TARGETOS)-$(TARGETARCH)

.PHONY: version format lint test get build image push linux arm clean

version:
> @echo $(VERSION)

format:
> gofmt -s -w ./

lint:
> go vet ./...

test:
> go test -v ./...

get:
> go get

build: format get
> CGO_ENABLED=0 GOOS=$(TARGETOS) GOARCH=$(TARGETARCH) go build -v -o kbot \
>   -ldflags "-X=github.com/chumachenko-vy/kbot/cmd.appVersion=$(VERSION)"

image:
> docker build . --platform $(TARGETOS)/$(TARGETARCH) --build-arg VERSION=$(VERSION) -t $(IMAGE_TAG)

push:
> docker push $(IMAGE_TAG)

linux:
> $(MAKE) image push TARGETOS=linux TARGETARCH=amd64

arm:
> $(MAKE) image push TARGETOS=linux TARGETARCH=arm64

clean:
> rm -f kbot
> -docker rmi $(IMAGE_TAG)
