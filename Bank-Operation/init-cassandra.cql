CREATE KEYSPACE IF NOT EXISTS bank_operation
    WITH replication = {'class': 'SimpleStrategy', 'replication_factor': '1'}
     AND durable_writes = true;