docker run -itd --name iaf-test-db-db2 --env-file env_list.sh --privileged=true -p 50000:50000 --restart unless-stopped ibmcom/db2
