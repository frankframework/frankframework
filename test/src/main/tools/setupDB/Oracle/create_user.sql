Drop user testiaf_user cascade;

Create user testiaf_user identified by testiaf_user default tablespace users; 

Grant dba to testiaf_user;
Grant resource to testiaf_user;
Grant connect to testiaf_user;

DROP ROLE ROLE_ING_WEBSPHERE_XA;
CREATE ROLE ROLE_ING_WEBSPHERE_XA;
GRANT SELECT ON SYS.DBA_PENDING_TRANSACTIONS TO ROLE_ING_WEBSPHERE_XA;
GRANT SELECT ON SYS.DBA_2PC_PENDING TO ROLE_ING_WEBSPHERE_XA;
GRANT EXECUTE ON SYS.DBMS_SYSTEM TO ROLE_ING_WEBSPHERE_XA;
GRANT SELECT ON SYS.PENDING_TRANS$ TO ROLE_ING_WEBSPHERE_XA;

Grant ROLE_ING_WEBSPHERE_XA to testiaf_user;

-- http://docs.codehaus.org/display/BTM/FAQ
grant select on sys.dba_pending_transactions to testiaf_user;
grant select on sys.pending_trans$ to testiaf_user;
grant select on sys.dba_2pc_pending to testiaf_user;
grant execute on sys.dbms_system to testiaf_user;

commit;
exit