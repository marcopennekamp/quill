package io.getquill

import io.getquill.context.jdbc.{ JdbcContext, UUIDStringEncoding }

class H2JdbcContext[N <: NamingStrategy] extends JdbcContext[H2Dialect, N] with UUIDStringEncoding {

}
