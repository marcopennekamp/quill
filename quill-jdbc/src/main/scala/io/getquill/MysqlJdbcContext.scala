package io.getquill

import io.getquill.context.jdbc.{ JdbcContext, UUIDStringEncoding }

class MysqlJdbcContext[N <: NamingStrategy] extends JdbcContext[MySQLDialect, N] with UUIDStringEncoding {

}
