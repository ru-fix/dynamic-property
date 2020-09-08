package ru.fix.dynamic.property.std.source

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import org.junit.jupiter.api.Test
import ru.fix.dynamic.property.api.source.OptionalDefaultValue
import ru.fix.dynamic.property.jackson.MarshallerBuilder
import java.util.*
import kotlin.collections.ArrayList

class InMemoryPropertySourceTest {

    @Test
    fun `register listener and change property value`() {
        val deque = Collections.synchronizedList(ArrayList<Int>())
        val source = InMemoryPropertySource(MarshallerBuilder.newBuilder().build())

        val subscription = source.createSubscription(
            "foo",
            Integer::class.java,
            OptionalDefaultValue.of(Integer(12))
        ).setAndCallListener {
            deque.add(it!!.toInt())
        }

        assertThat(deque, hasSize(equalTo(1)))
        assertThat(deque[0], equalTo(12))

        source["my"] = "14"
        assertThat(deque, hasSize(equalTo(1)))

        source["foo"] = "14"
        assertThat(deque, hasSize(equalTo(2)))
        assertThat(deque[1], equalTo(14))

        subscription.close()

        //after closing subscription there will be no update from source
        source["foo"] = "42"
        assertThat(deque, hasSize(equalTo(2)))
        assertThat(deque[1], equalTo(14))
    }
}
