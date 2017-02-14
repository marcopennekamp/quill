package io.getquill

import io.getquill.context.jdbc.{ JdbcContext, UUIDStringEncoding }

class SqliteJdbcContext[N <: NamingStrategy] extends JdbcContext[SqliteDialect, N] with UUIDStringEncoding {

}
