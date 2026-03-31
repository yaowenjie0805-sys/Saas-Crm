package com.yao.crm.util;

import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class IdGenerator {

    public String generate(String prefix) {
        return prefix + "_" + Long.toString(System.currentTimeMillis(), 36)
            + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
    }
}