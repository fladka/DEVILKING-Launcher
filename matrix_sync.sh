#!/bin/bash
# DEVILKING OS - Git Automation Utility
MESSAGE=$1
if [ -z "$MESSAGE" ]; then
  MESSAGE="sys: automated matrix update"
fi

echo ">> [SYSTEM]: Initiating Git Handshake..."
git add .
git commit -m "$MESSAGE"
git push origin main
echo ">> [SYSTEM]: Matrix Synced to GitHub. Evolution complete."
