#!/bin/bash
LATEST_LOG=$(ls -t /app/target/gatling/*/simulation.log | head -1)
awk -F'\t' '$1=="REQUEST"{c++;if($6-$5>1000){print "EXACT_REQUESTS: " c " TIME_MS: " $6-$5; exit}}' "$LATEST_LOG"
