FROM ibmcom/db2

COPY init_user.* /var/custom/

HEALTHCHECK --start-period=240s CMD /opt/ibm/db2/V11.5/bin/db2 CONNECT TO TESTIAF USER testiaf_user USING testiaf_user00