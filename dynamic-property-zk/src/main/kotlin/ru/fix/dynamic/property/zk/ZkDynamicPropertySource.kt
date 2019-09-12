package ru.fix.dynamic.property.zk;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.data.Stat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.fix.dynamic.property.api.DynamicPropertyListener;
import ru.fix.dynamic.property.api.marshaller.DynamicPropertyMarshaller;
import ru.fix.dynamic.property.std.source.AbstractPropertySource;
import ru.fix.stdlib.concurrency.threads.ReferenceCleaner;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Implementation of {@link DynamicPropertyListener} which uses {@link TreeCache} inside and
 * provides subscriptions to property change events
 */
public class ZkDynamicPropertySource extends AbstractPropertySource {
    private static final Logger log = LoggerFactory.getLogger(ZkDynamicPropertySource.class);

    private final String configLocation;
    private CuratorFramework curatorFramework;
    private final DynamicPropertyMarshaller marshaller;

    private Map<String, Collection<DynamicPropertyListener<String>>> listeners = new ConcurrentHashMap<>();

    private TreeCache treeCache;

    public ZkDynamicPropertySource(
            String zookeeperQuorum,
            String configLocation,
            DynamicPropertyMarshaller marshaller
    ) throws Exception {
        this(
                CuratorFrameworkFactory.newClient(
                        zookeeperQuorum,
                        new ExponentialBackoffRetry(1000, 10)
                ),
                configLocation,
                marshaller
        );
    }

    /**
     * @param curatorFramework Ready to use curator framework
     * @param configLocation   Root path where ZkDynamicPropertySource will store properties. E.g.
     *                         '/cpapsm/config/SWS'
     */
    public ZkDynamicPropertySource(
            CuratorFramework curatorFramework,
            String configLocation,
            DynamicPropertyMarshaller marshaller
    ) throws Exception {
        super(marshaller, ReferenceCleaner.getInstance());

        this.curatorFramework = curatorFramework;
        this.configLocation = configLocation;
        this.marshaller = marshaller;
        init();
    }

    private String zkDataToStringOrNull(byte[] data, String logDetails){
        try {
            return new String(
                    data,
                    StandardCharsets.UTF_8);
        } catch (Exception exc) {
            log.error("Failed to read string value from zk node. {}", logDetails, exc);
            return null;
        }

    }

    private void init() throws Exception {
        if (curatorFramework.getState().equals(CuratorFrameworkState.LATENT)) {
            curatorFramework.start();
        }

        treeCache = new TreeCache(this.curatorFramework, this.configLocation);
        treeCache.getListenable().addListener((currentFramework, treeCacheEvent) -> {
            switch (treeCacheEvent.getType()) {
                case NODE_ADDED:
                case NODE_UPDATED:
                    try {
                        onZkTreeChanged(treeCacheEvent, zkDataToStringOrNull(
                                treeCacheEvent.getData().getData(),
                                treeCacheEvent.toString()
                        ));

                    } catch (Exception exc) {
                        log.error("Zk property updating error for event {}", treeCacheEvent, exc);
                    }
                    break;
                case NODE_REMOVED:
                    onZkTreeChanged(treeCacheEvent, null);
                    break;
                default:
                    break;
            }
        });
        treeCache.start();
    }

    private void onZkTreeChanged(TreeCacheEvent treeCacheEvent, String newValue) {
        String absolutePath = treeCacheEvent.getData().getPath();
        String propertyName = getPropertyNameFromAbsolutePath(absolutePath);

        log.info("Event type {} for node '{}'. New value is '{}'", treeCacheEvent.getType(), absolutePath, newValue);
        invokePropertyListener(propertyName, newValue);
    }



    @Nullable
    @Override
    public String getPropertyValue(@NotNull String propertyName) {
        String path = getAbsolutePathForProperty(propertyName);
        ChildData currentData = treeCache.getCurrentData(path);
        return currentData == null ? null : new String(currentData.getData(), StandardCharsets.UTF_8);
    }


    /**
     * Works through curator directly to load latest data
     */
    public Map<String, Object> getAllProperties() throws Exception {
        Map<String, Object> allProperties = new HashMap<>();
        Stat exist = curatorFramework.checkExists().forPath(configLocation);
        if (exist != null) {
            List<String> childs = curatorFramework.getChildren().forPath(configLocation);
            if (!childs.isEmpty()) {
                CountDownLatch latch = new CountDownLatch(childs.size());
                for (String child : childs) {
                    curatorFramework.getData().watched().inBackground((client, event) -> {
                        allProperties.put(child, new String(event.getData(), StandardCharsets.UTF_8));
                        latch.countDown();
                    }).forPath(getAbsolutePathForProperty(child));
                }
                if (!latch.await(120, TimeUnit.SECONDS)) {
                    throw new TimeoutException("Failed to extract zk properties data");
                }
            }
        }
        return allProperties;
    }


    private String getAbsolutePathForProperty(String propertyName) {
        Objects.requireNonNull(propertyName);
        return configLocation + '/' + propertyName;
    }

    private String getPropertyNameFromAbsolutePath(String absolutePath){
        return absolutePath.substring(configLocation.length() + 1);
    }


    @Override
    public void close() {
        super.close();
        treeCache.close();
    }
}
