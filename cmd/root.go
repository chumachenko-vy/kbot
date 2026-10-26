package cmd

import (
	"os"

	"github.com/spf13/cobra"
)

var rootCmd = &cobra.Command{
	Use:   "kbot",
	Short: "kbot - simple Telegram bot",
	Long: `kbot is a simple Telegram bot written in Go
using cobra and telebot v4. Run "kbot start" to start the bot.`,
}

func Execute() {
	err := rootCmd.Execute()
	if err != nil {
		os.Exit(1)
	}
}
