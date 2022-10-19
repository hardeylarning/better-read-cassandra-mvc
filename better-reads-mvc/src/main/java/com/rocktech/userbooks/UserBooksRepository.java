package com.rocktech.userbooks;

import org.springframework.data.cassandra.repository.CassandraRepository;

public interface UserBooksRepository extends CassandraRepository<UserBooks, UserBooksPrimaryKey> {


}
