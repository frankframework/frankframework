FROM postgres
COPY enableXA.sql /docker-entrypoint-initdb.d/
ENV POSTGRES_PASSWORD testiaf_user00
ENV POSTGRES_USER testiaf_user
ENV POSTGRES_DB testiaf
