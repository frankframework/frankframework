docker container stop iaf-test-as-websphere-with-oracle
docker container rm iaf-test-as-websphere-with-oracle
docker run --publish 80:9080 --publish 9443:9443 --publish 9043:9043 --mount type=bind,source="%~dp0/../../../../../../test/src/test/testtool",target=/opt/frank/testtool-ext -e jdbc.dbms.default=oracle --name iaf-test-as-websphere-with-oracle iaf-test-as-websphere