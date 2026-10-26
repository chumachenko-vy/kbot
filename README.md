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
