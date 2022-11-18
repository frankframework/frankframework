#!/bin/bash

# Remove the default settings.xml that forces all repos to go through a mirror
rm ~/.m2/settings.xml
# ranker was causing an OOM exception so remove for now
rm -rf /opt/muse/packages/ranker/