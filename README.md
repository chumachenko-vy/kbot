# kbot

Simple Telegram bot written in Go using [cobra](https://github.com/spf13/cobra) and [telebot v4](https://gopkg.in/telebot.v4).

Bot: [t.me/vy_kbot_bot](https://t.me/vy_kbot_bot)

## Installation

```bash
git clone https://github.com/chumachenko-vy/kbot.git
cd kbot
go build -ldflags "-X=github.com/chumachenko-vy/kbot/cmd.appVersion=v1.0.0"
```

## Usage

Get a bot token from [@BotFather](https://t.me/BotFather), set it and start the bot:

```bash
read -s "TELE_TOKEN?Paste token: "
export TELE_TOKEN
./kbot start
```

CLI commands:

- `./kbot version` — print version
- `./kbot start` — start the bot (alias for `./kbot kbot`)
- `./kbot --help` — help

Bot commands in Telegram:

- `/start` — greeting
- `/version` — bot version
- `hello` — bot replies with a greeting and its version
- any other text — bot replies that the command is unknown

## Build & Run with Make

```bash
make version            # v1.0.0-<short sha>
make test               # go test
make build              # local binary
make image push         # ghcr.io/chumachenko-vy/kbot:<version>-linux-amd64
make arm                # build & push linux/arm64 image
```

## CI/CD

Every push to the `develop` branch runs the [KBOT-CICD](.github/workflows/cicd.yaml) workflow:

1. **CI** — `make lint`, `make test`, build the image and push it to `ghcr.io`
   as `ghcr.io/chumachenko-vy/kbot:<tag>-<os>-<arch>`
   (`<tag>` = latest git tag + short commit SHA, e.g. `v1.0.0-106879e`).
2. **CD** — update `image.tag` in `helm/values.yaml` and commit it back to `develop` (`[skip ci]`).
3. **ArgoCD** watches `develop` / `helm/` ([argocd/kbot-app.yaml](argocd/kbot-app.yaml)),
   detects the new tag and syncs the Deployment in the Kubernetes cluster.

```mermaid
flowchart LR
    dev([Developer]) -->|git push develop| gh[(GitHub repo<br/>chumachenko-vy/kbot)]
    gh -->|on: push develop| ci

    subgraph GHA[GitHub Actions: KBOT-CICD]
        direction TB
        ci[CI job<br/>checkout · make lint · make test] --> build[make image<br/>linux/amd64]
        build --> push[make push]
        push --> cd[CD job<br/>yq: image.tag = v1.0.0-sha]
        cd -->|commit skip ci| commit[git push develop]
    end

    push -->|docker push| ghcr[(ghcr.io<br/>chumachenko-vy/kbot:<br/>v1.0.0-sha-linux-amd64)]
    commit --> gh

    subgraph K8S[Kubernetes cluster]
        argo[ArgoCD<br/>Application kbot] -->|helm template + sync| deploy[Deployment kbot<br/>ns: kbot]
        secret[[Secret kbot<br/>TELE_TOKEN]] -.-> deploy
    end

    gh -.->|poll helm/ on develop| argo
    ghcr -->|image pull| deploy
    deploy <-->|long polling| tg([Telegram API<br/>@vy_kbot_bot])
    user([Telegram user]) <--> tg
```

### Deploy with ArgoCD

```bash
kubectl create ns kbot
kubectl -n kbot create secret generic kbot --from-literal=token="$TELE_TOKEN"
kubectl apply -f argocd/kbot-app.yaml
```
