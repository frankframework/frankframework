FROM mcr.microsoft.com/mssql/server
COPY . /
COPY dockerInit.sh /
COPY sqljdbc_xa.dll /opt/mssql-tools/bin/sqljdbc_xa.dll
#RUN chmod +x /dockerInit.sh
CMD /bin/bash ./entrypoint.sh
