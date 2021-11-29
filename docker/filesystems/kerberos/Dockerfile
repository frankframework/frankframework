FROM alpine
RUN apk add --no-cache krb5-server krb5 supervisor tini
COPY supervisord.conf /etc/supervisord.conf
COPY docker-entrypoint.sh /
ENTRYPOINT ["/sbin/tini", "--"]
CMD ["/docker-entrypoint.sh"]
