#!/usr/bin/env bash

set -eu

lein uberjar

lein test
lein cljsbuild once
node ./target/test.js
