FROM mcr.microsoft.com/mssql/server:2019-GA-ubuntu-16.04
COPY create_user.sql /
COPY dockerInit.sh /
CMD /bin/bash /dockerInit.sh
