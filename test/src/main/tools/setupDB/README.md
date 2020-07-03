# Docker Containers 

## Getting Started

There are three folders containing Docker configurations:
    - MsSqlServer
    - Oracle
    - MySQL

These instructions will cover usage information and for the docker container 

### Prerequisities

In order to run this container you'll need docker installed.

* [Windows](https://docs.docker.com/windows/started)
* [OS X](https://docs.docker.com/mac/started/)
* [Linux](https://docs.docker.com/linux/started/)

### Usage

#### Create image and container.

If you want to create a docker container with MSSQl or Oracle server, open a console command, go to your home directory. \
FOR MSSQL CONTAINER: \
home\iaf\docker\IAF-MS-SQL \
and execute this command: \
docker-compose up -d

FOR ORACLE CONTAINER: \
home\iaf\docker\IAF-Oracle \
and execute this command: \
docker-compose --env-file docker/.env up -d

FOR MySQL CONTAINER: \
home\iaf\docker\IAF-MySQL \
and execute this command: \
docker-compose up -d

you can see something like that:
$ docker-compose up -d --build \
Creating volume "iaf-ms-sql_db_data" with default driver \
Building ************
Step 1/4 : FROM *******  \
latest: Pulling from ******  \
Digest: sha256:e064843673f08f22192c044ffa6a594b0670a3eb3f9ff7568dd7a65a698fc4d6  \
Status: Downloaded newer image for *************************  \
 ---> 3c7ee124fdd6  \
Step 2/4 : COPY . /  \
 ---> bba2032a711d  \
Step 3/4 : COPY dockerInit.sh /  \
 ---> b6655f5af72f  \
Step 4/4 : CMD /bin/bash ./entrypoint.sh  \
 ---> Running in d6e83fde3a22  \
Removing intermediate container d6e83fde3a22  \
 ---> bc1e68a2e558  \
Successfully built bc1e68a2e558 

To check our container type: 

docker ps -a

All containers setup a default database, named 'testiaf', and create a user 
with login 'testiaf_user' and password 'testiaf_user00'.

To get access inside of your container you can type: \
[Windows] \
winpty docker container exec -ti <container_ID> bash  \
[Linux] \
docker container exec -ti <container_ID> bash 

You can login in mssql using this command: \
/opt/mssql-tools/bin/sqlcmd -S localhost -U testiaf_user -P "testiaf_user00"

You can login in MySQL using this command: \
mysql -u testiaf_user --password=testiaf_user00