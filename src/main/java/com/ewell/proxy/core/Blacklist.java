package com.ewell.proxy.core;

import java.util.concurrent.ConcurrentSkipListMap;

/**
 * @author wy
 * @date 2022/1/14 10:20 AM
 * @desctiption
 */
public class Blacklist {

    public static ConcurrentSkipListMap<String, Integer> map = new ConcurrentSkipListMap<>();

    public static void addIp(String ip) {
        map.compute(ip, (k, v) -> {
            if (v == null) {
                return 1;
            }
            return ++v;
        });
    }

    public static boolean inBlacklist(String ip) {
        Integer integer = map.get(ip);
        return integer != null && integer > 10;
    }

}
