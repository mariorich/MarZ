#!/bin/bash
set -e

HOST_IP="204.90.115.200"
HOST_NAME="S0W1.DAL-EBIS.IHOST.COM"

if ! grep -q "$HOST_NAME" /etc/hosts; then
    echo "$HOST_IP $HOST_NAME" | sudo tee -a /etc/hosts > /dev/null
fi