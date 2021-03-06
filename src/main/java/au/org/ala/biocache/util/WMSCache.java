/**************************************************************************
 *  Copyright (C) 2013 Atlas of Living Australia
 *  All Rights Reserved.
 * 
 *  The contents of this file are subject to the Mozilla Public
 *  License Version 1.1 (the "License"); you may not use this file
 *  except in compliance with the License. You may obtain a copy of
 *  the License at http://www.mozilla.org/MPL/
 * 
 *  Software distributed under the License is distributed on an "AS
 *  IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  rights and limitations under the License.
 ***************************************************************************/
package au.org.ala.biocache.util;

import au.org.ala.biocache.dto.PointType;
import org.apache.log4j.Logger;

import java.io.InputStream;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * A cache of points and colours for WMS.
 *
 * Cache size defaults can overridden in wms.properties or directly at runtime.
 * TODO refactor to use the common configuration file biocache-config.properties
 *
 * Management of the cache size not exact.
 *
 * @author Adam
 */
public class WMSCache {

    private final static Logger logger = Logger.getLogger(WMSCache.class);
    //max size of cached params in bytes
    static long MAX_CACHE_SIZE = 104857600;
    //min size of cached params in bytes
    static long MIN_CACHE_SIZE = 52428800;
    //max size of any one object in the cache in bytes
    static long LARGEST_CACHEABLE_SIZE = 52428800;
    //max age of any one object in the cache in ms
    static long MAX_AGE = 3600000;
    //in memory store of params
    static ConcurrentHashMap<String, WMSTile> cache = new ConcurrentHashMap<String, WMSTile>();
    //cache size management
    final static Object counterLock = new Object();
    static long cacheSize;
    static CountDownLatch counter;
    //thread for cache size limitation
    final static Thread cacheCleaner;
    //settable properties file
    static Properties wmsProperties;
    //lock on get operation
    final static Object getLock = new Object();
    //cache size before cleaner is triggered
    static long triggerCleanSize = MIN_CACHE_SIZE + (MAX_CACHE_SIZE - MIN_CACHE_SIZE) / 2;

    static {
        counter = new CountDownLatch(1);

        cacheCleaner = new Thread() {

            @Override
            public void run() {
                try {
                    while (true) {
                        counter.await();

                        synchronized (counterLock) {
                            cacheSize = MIN_CACHE_SIZE;
                            counter = new CountDownLatch(1);
                        }

                        cleanCache();
                    }
                } catch (InterruptedException e) {
                } catch (Exception e) {
                    logger.error("wms cache cleaner stopping unexpectedly", e);
                }
            }
        };
        cacheCleaner.start();

        try {
            wmsProperties = new Properties();
            InputStream is = ParamsCache.class.getResourceAsStream("/wms.properties");
            wmsProperties.load(is);

            MAX_CACHE_SIZE = Long.parseLong(wmsProperties.getProperty("MAX_CACHE_SIZE", String.valueOf(MAX_CACHE_SIZE)));
            MIN_CACHE_SIZE = Long.parseLong(wmsProperties.getProperty("MIN_CACHE_SIZE", String.valueOf(MIN_CACHE_SIZE)));
            LARGEST_CACHEABLE_SIZE = Long.parseLong(wmsProperties.getProperty("LARGEST_CACHEABLE_SIZE", String.valueOf(LARGEST_CACHEABLE_SIZE)));
            MAX_AGE = Long.parseLong(wmsProperties.getProperty("MAX_AGE", String.valueOf(MAX_AGE)));

            logger.info("MAX_CACHE_SIZE > " + MAX_CACHE_SIZE);
            logger.info("MIN_CACHE_SIZE > " + MIN_CACHE_SIZE);
            logger.info("LARGEST_CACHEABLE_SIZE > " + LARGEST_CACHEABLE_SIZE);
            logger.info("MAX_AGE > " + MAX_AGE);
        } catch (Exception e) {
            logger.error("cannot load wms.properties", e);
        }
    }

    /**
     * Store search params and return key.
     *
     * @param q Search url params to store as String.
     * @param colourMode to store as String
     * @param pointType resolution of data to store as PointType
     * @param wco data to store as WMSTile
     * @return true when successfully added to the cache.  WMSCache must be 
     * enabled, not full.  wco must be not too large and not cause the cache
     * to exceed max size when added.
     */
    public static boolean put(String q, String colourMode, PointType pointType, WMSTile wco) {
        if (isFull() || !isEnabled()) {
            return false;
        }

        wco.updateSize();

        if (wco.getSize() < LARGEST_CACHEABLE_SIZE) {
            synchronized (counterLock) {
                if (cacheSize + wco.getSize() > MAX_CACHE_SIZE) {
                    return false;
                }
                cache.put(getKey(q, colourMode, pointType), wco);
                cacheSize += wco.getSize();
                logger.debug("new cache size: " + cacheSize);
                if (cacheSize > triggerCleanSize) {
                    counter.countDown();
                }
            }

            wco.setCached(true);

            return true;
        } else {
            return false;
        }
    }

    /**
     * cache key built from query, colourmode and point type.
     *
     * @param query
     * @param colourmode
     * @param pointType
     * @return cache key as String
     */
    public static String getKey(String query, String colourmode, PointType pointType) {
        return query + "|" + colourmode + "|" + pointType.getLabel();
    }

    /**
     * Retrieve search parameter object
     * @return search parameter q as String, or null if not in memory
     * or in file storage.
     *
     * @param query Search url params to store as String.
     * @param colourmode colourmode as String
     * @param pointType data resolution as PointType
     * @return WMSTile that can be in varying states:
     * - being filled when !getCached() and isCacheable() and is locked
     * - will not be filled when !isCacheable()
     * - ready to use when getCached()
     */
    public static WMSTile get(String query, String colourmode, PointType pointType) {
        String key = getKey(query, colourmode, pointType);
        WMSTile obj = null;
        synchronized (getLock) {
            obj = cache.get(key);

            if (obj != null && obj.getCreated() + MAX_AGE < System.currentTimeMillis()) {
                cache.remove(key);
                obj = null;
            }

            if (obj == null) {
                obj = new WMSTile();
                cache.put(key, obj);
            }
        }

        if (obj != null) {
            obj.lastUse = System.currentTimeMillis();
        }

        return obj;
    }

    /**
     * empty the cache to <= MIN_CACHE_SIZE
     */
    static void cleanCache() {
        List<Entry<String, WMSTile>> entries = new ArrayList(cache.entrySet());

        //sort ascending by last use time
        Collections.sort(entries, new Comparator<Entry<String, WMSTile>>() {

            @Override
            public int compare(Entry<String, WMSTile> o1, Entry<String, WMSTile> o2) {
                long c = o1.getValue().lastUse - o2.getValue().lastUse;
                return (c < 0) ? -1 : ((c > 0) ? 1 : 0);
            }
        });

        long size = 0;
        int numberRemoved = 0;
        for (int i = 0; i < entries.size(); i++) {
            if (size + entries.get(i).getValue().getSize() > MIN_CACHE_SIZE) {
                String key = entries.get(i).getKey();
                cache.remove(key);
                numberRemoved++;
            } else {
                size += entries.get(i).getValue().getSize();
            }
        }

        synchronized (counterLock) {
            cacheSize -= (MIN_CACHE_SIZE - size);
            size = cacheSize;
        }
        logger.debug("removed " + numberRemoved + " cached wms points, new cache size " + size);
    }

    /**
     * WMSCache is enabled
     *
     * @return true when WMSCache is enabled
     */
    public static boolean isEnabled() {
        return MAX_CACHE_SIZE > 0;
    }

    /**
     * empty the WMSCache
     */
    public static void empty() {
        synchronized (counterLock) {
            cacheSize = 0;
            counter = new CountDownLatch(1);
        }
        cache.clear();
    }

    /**
     * remove a specific cache entry.
     *
     * @param q Search url params to store as String.
     * @param colourMode to store as String
     * @param pointType resolution of data to store as PointType
     */
    public static void remove(String q, String colourMode, PointType pointType) {
        cache.remove(getKey(q, colourMode, pointType));
    }

    /**
     * determine if a WMSTile created with the given characteristics
     * will be too large for the cache.
     *
     * @param wco
     * @param occurrenceCount
     * @param hasCounts
     * @return
     */
    public static boolean isCachable(WMSTile wco, int occurrenceCount, boolean hasCounts) {
        long size = WMSTile.sizeOf(occurrenceCount, hasCounts);
        if (size > LARGEST_CACHEABLE_SIZE) {
            if (wco != null) {
                wco.setSize(size);
            }
            return false;
        }

        return true;
    }

    /**
     * Test if cache is full.
     *
     * Note: all put requests into the cache will fail should it be full.
     *
     * @return
     */
    static public boolean isFull() {
        return cacheSize >= MAX_CACHE_SIZE;
    }

    static public void setMaxCacheSize(long sizeInBytes) {
        MAX_CACHE_SIZE = sizeInBytes;
        updateTriggerCleanSize();
    }

    static public long getMaxCacheSize() {
        return MAX_CACHE_SIZE;
    }

    static public void setMinCacheSize(long sizeInBytes) {
        MIN_CACHE_SIZE = sizeInBytes;
        updateTriggerCleanSize();
    }

    static public long getMinCacheSize() {
        return MIN_CACHE_SIZE;
    }

    static public void setLargestCacheableSize(long sizeInBytes) {
        LARGEST_CACHEABLE_SIZE = sizeInBytes;
    }

    static public long getLargestCacheableSize() {
        return LARGEST_CACHEABLE_SIZE;
    }

    static long getSize() {
        return cacheSize;
    }

    /**
     * cache cleaner is triggered when the size of the cache is
     * half way between the min and max size.
     */
    static void updateTriggerCleanSize() {
        triggerCleanSize = MIN_CACHE_SIZE + (MAX_CACHE_SIZE - MIN_CACHE_SIZE) / 2;
        logger.debug("triggerCleanSize=" + triggerCleanSize + " MIN_CACHE_SIZE=" + MIN_CACHE_SIZE + " MAX_CACHE_SIZE=" + MAX_CACHE_SIZE);
    }
}
