# gmail-inbox-status

**gmail-inbox-status** prints the number of messages (all messages or only unread) in your Gmail inbox on the standard output.
It supports several accounts, and is meant to support scripting tools in command line workflows for remembering to check your e-mail.
It also has a mode to check if your (unread) inbox is empty, and if not, exit unsuccessfully.

## Installation

You will need a Java 11 JDK installed.

You will need to create an OAuth 2.0 Client ID in the [Google Cloud Platform](https://console.cloud.google.com/apis/credentials).
This is unfortunate, but unfortunately there does not seem to be any other way to get connected to the Google APIs.

Download the client secret JSON and place it as `~/.gmail-inbox-status/google-oauth-credentials.json`.

Install the tool with `./install.sh`. It will in install it in `/usr/local/bin`.

## Usage

The first time you run `gmail-inbox-status`, it will open up a browser where you will need to authenticate to your Gmail account, and give permission to access the e-mail.

Let's say you have 15 messages in your inbox, but no unread.
```shell
$ gmail-inbox-status
15
$ gmail-inbox-status --unread
0
$ gmail-inbox-status --check-empty && echo "empty" || echo "not empty"
not empty
$ gmail-inbox-status --check-empty --unread && echo "empty" || echo "not empty"
empty
```

## Alternatives

* [cmdg](https://github.com/ThomasHabets/cmdg) – TUI written in Go
* [qGmail](https://github.com/UtkarshVerma/qgmail) - cli written in Go
* [google-gmail1-cli](https://crates.io/crates/google-gmail1-cli) - library and cli written in Ruby
