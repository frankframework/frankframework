FROM mariadb
COPY create_user.sql /docker-entrypoint-initdb.d/
ENV TZ=Europe/Amsterdam
ENV MYSQL_ROOT_PASSWORD MySqlRootPassword
ENV MYSQL_DATABASE testiaf
ENV MYSQL_USER testiaf_user
ENV MYSQL_PASSWORD testiaf_user00
ENV wait_timeout 10