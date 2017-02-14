package io.getquill

import io.getquill.context.jdbc.{ JdbcContext, UUIDObjectEncoding }

class PostgresJdbcContext[N <: NamingStrategy] extends JdbcContext[PostgresDialect, N] with UUIDObjectEncoding {

}
