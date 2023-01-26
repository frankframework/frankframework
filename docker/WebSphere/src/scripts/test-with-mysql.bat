docker container stop iaf-test-as-websphere-with-mysql
docker container rm iaf-test-as-websphere-with-mysql
docker run --publish 80:9080 --publish 9443:9443 --publish 9043:9043  -e larva.adapter.execute= -e jdbc.dbms.default=mysql --name iaf-test-as-websphere-with-mysql iaf-test-as-websphere