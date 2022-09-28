alter system set processes=400 scope=spfile;
alter system set sessions=640 scope=spfile;
alter system set transactions=704 scope=spfile;

shutdown immediate;
startup;

exit
