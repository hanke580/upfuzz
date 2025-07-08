#!/bin/bash
source bin/compute_time.sh

DIR_NAME=$(find failure -type f -name "incons*" | sort -t_ -k2,2n | head -n1 | awk -F'/' '{print $1 "/" $2}')

compute_triggering_time $DIR_NAME
