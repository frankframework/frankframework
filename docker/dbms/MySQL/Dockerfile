FROM mysql
COPY create_user.sql /docker-entrypoint-initdb.d/
ENV TZ=Europe/Amsterdam
ENV MYSQL_ROOT_PASSWORD MySqlRootPassword
ENV innodb_lock_wait_timeout 5
ENV wait_timeout 5
ENV lock_wait_timeout 5
ENV performance_schema 1
