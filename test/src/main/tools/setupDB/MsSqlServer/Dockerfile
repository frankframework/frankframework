FROM mcr.microsoft.com/mssql/server
COPY sqljdbc_xa.dll /opt/mssql-tools/bin/sqljdbc_xa.dll
COPY create_user.sql /
COPY xa_install.sql /
COPY dockerInit.sh /
CMD /dockerInit.sh
