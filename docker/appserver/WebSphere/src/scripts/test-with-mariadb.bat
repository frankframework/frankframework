docker container stop iaf-test-as-websphere-with-mariadb
docker container rm iaf-test-as-websphere-with-mariadb
docker run --publish 80:9080 --publish 9443:9443 --publish 9043:9043  -e larva.adapter.execute= -e jdbc.dbms.default=mariadb --name iaf-test-as-websphere-with-mariadb iaf-test-as-websphere