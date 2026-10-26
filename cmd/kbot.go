package cmd

import (
	"fmt"
	"log"
	"os"
	"time"

	"github.com/spf13/cobra"
	telebot "gopkg.in/telebot.v4"
)

var TeleToken = os.Getenv("TELE_TOKEN")

var kbotCmd = &cobra.Command{
	Use:     "kbot",
	Aliases: []string{"start"},
	Short:   "Start Telegram bot",
	Run: func(cmd *cobra.Command, args []string) {
		fmt.Printf("kbot %s started\n", appVersion)

		kbot, err := telebot.NewBot(telebot.Settings{
			Token:  TeleToken,
			Poller: &telebot.LongPoller{Timeout: 10 * time.Second},
		})
		if err != nil {
			log.Fatalf("Check TELE_TOKEN env variable: %s", err)
		}

		kbot.Handle("/start", func(c telebot.Context) error {
			return c.Send("Привіт! Напиши hello або /version")
		})

		kbot.Handle("/version", func(c telebot.Context) error {
			return c.Send("kbot " + appVersion)
		})

		kbot.Handle(telebot.OnText, func(c telebot.Context) error {
			log.Printf("%s: %s", c.Sender().Username, c.Text())
			switch c.Text() {
			case "hello":
				return c.Send(fmt.Sprintf("Hello, I'm kbot %s!", appVersion))
			default:
				return c.Send("Не знаю такої команди, спробуй hello")
			}
		})

		kbot.Start()
	},
}

func init() {
	rootCmd.AddCommand(kbotCmd)
}
