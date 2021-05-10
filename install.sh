#!/usr/bin/env bash

set -e

TOOL=gmail-inbox-status
BIN=${HOME}/local/bin
BUILT_BINARY=`pwd`/$TOOL/build/install/${TOOL}/bin/${TOOL}

./gradlew install
ln -fs ${BUILT_BINARY} ${BIN}/${TOOL}

if [ -d ~/.oh-my-zsh/custom ]; then
    _GMAIL_INBOX_STATUS_AUTOCOMPLETE=true ${BUILT_BINARY} > ~/.oh-my-zsh/custom/gmail-inbox-status.zsh
fi

