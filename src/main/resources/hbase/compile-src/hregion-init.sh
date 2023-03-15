#!/usr/bin/env bash
set -euo pipefail

if [[ -z $(grep -F "master" "/etc/hosts") ]];
then
        echo "252.11.1.10   master" >> /etc/hosts
        echo "252.11.1.2    hmaster" >> /etc/hosts
        echo "252.11.1.3    hregion1" >> /etc/hosts
        echo "252.11.1.4    hregion2" >> /etc/hosts
        echo "master written to host"
fi
