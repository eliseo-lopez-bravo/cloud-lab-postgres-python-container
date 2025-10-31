#!/usr/bin/env bash
set -e

echo "🧰 Setting up lab environment..."

sudo apt-get update -y
sudo apt-get install -y unzip curl jq git

echo "✅ Basic tools installed successfully."
