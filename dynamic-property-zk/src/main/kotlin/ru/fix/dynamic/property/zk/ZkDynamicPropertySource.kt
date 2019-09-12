package ru.fix.dynamic.property.zk

import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.framework.imps.CuratorFrameworkState
import org.apache.curator.framework.recipes.cache.TreeCache
import org.apache.curator.framework.recipes.cache.TreeCacheEvent
import org.apache.curator.framework.recipes.cache.TreeCacheListener
import org.apache.curator.retry.ExponentialBackoffRetry
import org.slf4j.LoggerFactory
import ru.fix.dynamic.property.api.marshaller.DynamicPropertyMarshaller
import ru.fix.dynamic.property.std.source.AbstractPropertySource
import ru.fix.stdlib.concurrency.threads.ReferenceCleaner
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Implementation of [DynamicPropertySource] that uses Zookeeper [TreeCache]
 * and provides subscriptions to property change events
 */
class ZkDynamicPropertySource
/**
 * @param curatorFramework Ready to use curator framework
 * @param configLocation   Root path where ZkDynamicPropertySource will store properties.
 * E.g. '/my-application/config'
 */
constructor(
        private val curatorFramework: CuratorFramework,
        configLocation: String,
        private val marshaller: DynamicPropertyMarshaller
) : AbstractPropertySource(marshaller, ReferenceCleaner.getInstance()) {

    companion object {
        private val log = LoggerFactory.getLogger(ZkDynamicPropertySource::class.java)
    }

    private var treeCache: TreeCache? = null
    private val rootPath: String =
            if (configLocation.endsWith('/')) {
                configLocation.substring(0, configLocation.length - 1)
            } else {
                configLocation
            }

    init {

        if (curatorFramework.state == CuratorFrameworkState.LATENT) {
            curatorFramework.start()
        }

        treeCache = TreeCache(this.curatorFramework, this.rootPath)
        treeCache!!.listenable.addListener(TreeCacheListener { currentFramework, treeCacheEvent ->
            when (treeCacheEvent.getType()) {
                TreeCacheEvent.Type.NODE_ADDED,
                TreeCacheEvent.Type.NODE_UPDATED -> {
                    try {
                        onZkTreeChanged(treeCacheEvent, zkDataToStringOrNull(
                                treeCacheEvent.getData().getData(),
                                treeCacheEvent.toString()
                        ))
                    } catch (exc: Exception) {
                        log.error("Zk property updating error for event {}", treeCacheEvent, exc)
                    }
                }
                TreeCacheEvent.Type.NODE_REMOVED -> {
                    onZkTreeChanged(treeCacheEvent, null)
                }
                else -> {
                }
            }
        })
        treeCache!!.start()
    }

    /**
     * @param zookeeperQuorum list of zookeeper hosts
     * @param configLocation Root path where ZkDynamicPropertySource will store properties.
     *                       E.g. '/my-application/config'
     */
    constructor(
            zookeeperQuorum: String,
            configLocation: String,
            marshaller: DynamicPropertyMarshaller
    ) : this(
            CuratorFrameworkFactory.newClient(
                    zookeeperQuorum,
                    ExponentialBackoffRetry(1000, 10)
            ),
            configLocation,
            marshaller
    )


    /**
     * Works through curator directly to load latest data
     */
    fun readAllProperties(): Map<String, Any> {
        val allProperties = HashMap<String, Any>()
        val exist = curatorFramework.checkExists().forPath(rootPath)
        if (exist != null) {
            val childs = curatorFramework.children.forPath(rootPath)
            if (!childs.isEmpty()) {
                val latch = CountDownLatch(childs.size)
                for (child in childs) {
                    curatorFramework.data.watched().inBackground { client, event ->
                        allProperties[child] = String(event.data, StandardCharsets.UTF_8)
                        latch.countDown()
                    }.forPath(getAbsolutePathForProperty(child))
                }
                if (!latch.await(120, TimeUnit.SECONDS)) {
                    throw TimeoutException("Failed to extract zk properties data")
                }
            }
        }
        return allProperties
    }


    private fun zkDataToStringOrNull(data: ByteArray, logDetails: String): String? =
            try {
                String(
                        data,
                        StandardCharsets.UTF_8)
            } catch (exc: Exception) {
                log.error("Failed to read string value from zk node. {}", logDetails, exc)
                null
            }


    private fun onZkTreeChanged(treeCacheEvent: TreeCacheEvent, newValue: String?) {
        val absolutePath = treeCacheEvent.data.path
        val propertyName = getPropertyNameFromAbsolutePath(absolutePath)

        log.info("Event type {} for node '{}'. New value is '{}'", treeCacheEvent.type, absolutePath, newValue)
        invokePropertyListener(propertyName, newValue)
    }


    protected override fun getPropertyValue(propertyName: String): String? {
        val path = getAbsolutePathForProperty(propertyName)
        val currentData = treeCache!!.getCurrentData(path)
        return if (currentData == null) null else String(currentData.data, StandardCharsets.UTF_8)
    }


    private fun getAbsolutePathForProperty(propertyName: String): String {
        Objects.requireNonNull(propertyName)
        return "$rootPath/$propertyName"
    }

    private fun getPropertyNameFromAbsolutePath(absolutePath: String): String {
        return absolutePath.substring(rootPath.length + 1)
    }

    override fun close() {
        super.close()
        treeCache!!.close()
    }


}
