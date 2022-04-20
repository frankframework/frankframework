# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

# ActiveMQ Artemis

FROM openjdk:8-jre-slim

# Make sure pipes are considered to determine success, see: https://github.com/hadolint/hadolint/wiki/DL4006
SHELL ["/bin/bash", "-o", "pipefail", "-c"]
WORKDIR /opt

# artemis version (latest java 8 compatible)
ENV ARTEMIS_VERSION 2.19.1


# Download Apache Artemis
RUN apt-get update && apt-get install -y wget --no-install-recommends \
	&& wget -q -O artemis.tar.gz https://downloads.apache.org/activemq/activemq-artemis/${ARTEMIS_VERSION}/apache-artemis-${ARTEMIS_VERSION}-bin.tar.gz --no-check-certificate \
	&& tar -xvf artemis.tar.gz \
	&& rm artemis.tar.gz \
	&& mv apache-artemis-${ARTEMIS_VERSION} activemq-artemis \
	&& apt-get clean \
	&& rm -rf /var/lib/apt/lists/*

COPY ./docker-run.sh /
RUN chmod +rx /*.sh

# Expose some outstanding folders
VOLUME ["/var/lib/artemis-instance"]
WORKDIR /var/lib/artemis-instance

ENTRYPOINT ["/docker-run.sh"]
CMD ["run"]