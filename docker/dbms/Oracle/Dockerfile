FROM store/oracle/database-enterprise:12.2.0.1
COPY create_user.sql /home/oracle
COPY entrypoint.sh /home/oracle
CMD /bin/bash /home/oracle/entrypoint.sh