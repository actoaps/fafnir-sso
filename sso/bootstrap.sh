#!/bin/sh
export WEBSERVICE_OPTS=\
${NEW_RELIC_KEY:+-javaagent:/newrelic.jar}
cat << YAML > newrelic.yml
common: &default_settings
  license_key: ${NEW_RELIC_KEY}
  app_name: ${NEW_RELIC_NAME}
YAML
bin/sso
