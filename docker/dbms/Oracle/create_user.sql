-- In Oracle 12c the hidden parameter _ORACLE_SCRIPT has been introduced to allow scripts to be executed.
-- When this setting is set to true, queries are performed in the root context, opposed to a pluggable database. 
-- This avoids using C## which is discouraged by Oracle.
-- Note that this can only be used if you don't use pluggable databases!
alter session set "_ORACLE_SCRIPT"=true;


--Drop user testiaf_user cascade;

Create user testiaf_user identified by testiaf_user00 default tablespace users; 

Grant dba to testiaf_user;
Grant resource to testiaf_user;
Grant connect to testiaf_user;

--DROP ROLE ROLE_WEBSPHERE_XA;
CREATE ROLE ROLE_WEBSPHERE_XA;
GRANT SELECT ON SYS.DBA_PENDING_TRANSACTIONS TO ROLE_WEBSPHERE_XA;
GRANT SELECT ON SYS.DBA_2PC_PENDING TO ROLE_WEBSPHERE_XA;
GRANT EXECUTE ON SYS.DBMS_SYSTEM TO ROLE_WEBSPHERE_XA;
GRANT SELECT ON SYS.PENDING_TRANS$ TO ROLE_WEBSPHERE_XA;

Grant ROLE_WEBSPHERE_XA to testiaf_user;

-- http://docs.codehaus.org/display/BTM/FAQ
grant select on sys.dba_pending_transactions to testiaf_user;
grant select on sys.pending_trans$ to testiaf_user;
grant select on sys.dba_2pc_pending to testiaf_user;
grant execute on sys.dbms_system to testiaf_user;
commit;

exit
