FROM --platform=$BUILDPLATFORM golang:1.26 AS builder
ARG TARGETOS
ARG TARGETARCH
ARG VERSION=v1.0.0
WORKDIR /src
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 GOOS=$TARGETOS GOARCH=$TARGETARCH go build -o /out/kbot \
    -ldflags "-X=github.com/chumachenko-vy/kbot/cmd.appVersion=$VERSION"

FROM scratch
LABEL org.opencontainers.image.source=https://github.com/chumachenko-vy/kbot
COPY --from=builder /out/kbot /kbot
COPY --from=builder /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/
ENTRYPOINT ["/kbot", "start"]
