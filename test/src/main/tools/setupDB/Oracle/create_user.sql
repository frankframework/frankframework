Drop user c##testiaf_user cascade;

Create user c##testiaf_user identified by c##testiaf_user default tablespace users; 

Grant dba to c##testiaf_user;
Grant resource to c##testiaf_user;
Grant connect to c##testiaf_user;

DROP ROLE C##ROLE_ING_WEBSPHERE_XA;
CREATE ROLE C##ROLE_ING_WEBSPHERE_XA;
GRANT SELECT ON SYS.DBA_PENDING_TRANSACTIONS TO C##ROLE_ING_WEBSPHERE_XA;
GRANT SELECT ON SYS.DBA_2PC_PENDING TO C##ROLE_ING_WEBSPHERE_XA;
GRANT EXECUTE ON SYS.DBMS_SYSTEM TO C##ROLE_ING_WEBSPHERE_XA;
GRANT SELECT ON SYS.PENDING_TRANS$ TO C##ROLE_ING_WEBSPHERE_XA;

Grant ROLE_ING_WEBSPHERE_XA to c##testiaf_user;

-- http://docs.codehaus.org/display/BTM/FAQ
grant select on sys.dba_pending_transactions to c##testiaf_user;
grant select on sys.pending_trans$ to c##testiaf_user;
grant select on sys.dba_2pc_pending to c##testiaf_user;
grant execute on sys.dbms_system to c##testiaf_user;
commit;
exit