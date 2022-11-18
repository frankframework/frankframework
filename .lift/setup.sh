#!/bin/bash

# Remove the default settings.xml that forces all repos to go through a mirror
rm ~/.m2/settings.xml
# ranker was causing an OOM exception so remove for now
rm -rf /opt/muse/packages/ranker/
mkdir -p /opt/muse/packages/ranker/

cat <<EOF >> /opt/muse/packages/ranker/rank.py
#!/usr/bin/env python3

if __name__=="__main__":
    print([])
    exit()


EOF

chmod a+x /opt/muse/packages/ranker/rank.py