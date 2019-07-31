import ru.fix.dynamic.property.api.DynamicPropertyListener
import ru.fix.dynamic.property.api.DynamicPropertySource
import javax.sql.DataSource

class PostgreSqlDynamicPropertySource(
        val dataSource: DataSource,
        val schema: String,
        val table: String
) : DynamicPropertySource {

    init {
        Liquibase

    }
    
//    """select where last update time > last-check_time"""

    override fun <T : Any?> addPropertyChangeListener(propertyName: String, type: Class<T>, listener: DynamicPropertyListener<T>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun close() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T : Any?> getProperty(key: String, type: Class<T>, defaultValue: T): T {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}