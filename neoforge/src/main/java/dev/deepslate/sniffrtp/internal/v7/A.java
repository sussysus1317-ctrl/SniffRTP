/*
 * Decompiled with CFR 0.152.
 */
package dev.deepslate.sniffrtp.internal.v7;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public final class A {
    private static volatile Logger logger = Logger.getLogger("SniffRTP");
    private static final AtomicBoolean P = new AtomicBoolean();

    public static void logger(Logger value) {
        logger = Objects.requireNonNull(value);
    }

    private A() {
    }

    private static String d(int[] nArray, int n) {
        char[] cArray = new char[nArray.length];
        for (int i = 0; i < nArray.length; ++i) {
            cArray[i] = (char)(nArray[i] ^ n + i * 131 + i * i * 17 & 0xFFFF);
        }
        return new String(cArray);
    }

    private static String x1() {
        return A.d(new int[]{9444, 9509, 9832, 9919, 10165, 10429, 10873, 11229, 11567, 11995, 12312, 12818, 13387, 14069, 14464, 15152, 15753, 16571, 17181, 17926, 18914, 19675, 20600, 21497}, 9399);
    }

    private static String x2() {
        return A.d(new int[]{25008, 25109, 25464, 25489, 25734, 26239, 26426, 26771, 27147, 27558, 28152, 28430, 29160, 29649, 30304, 30738, 31592, 32151, 32933, 33761, 34554, 35262, 36169, 37066, 37943, 39038, 40049, 41064, 42008, 43248, 44191, 45544, 46677, 47969, 49106, 50551, 51747, 53194, 54602, 55970, 57586, 58944, 60438, 62204, 63635, 65319, 1432, 3256}, 25043);
    }

    private static String x3() {
        return A.d(new int[]{15017, 15137, 15487, 15507, 15790, 16067, 16408, 16824, 17278, 17620, 18167, 18435, 18944, 19651, 20161, 20796, 21406, 22216, 22868, 23552, 24568, 25274, 26196, 27102, 27962, 29048, 29824, 30922, 32022, 33088, 34280, 35394, 36674, 37761, 39065, 40559}, 15041);
    }

    public static void a() {
        if (!P.compareAndSet(false, true)) {
            return;
        }
        logger.info(A.x1());
        logger.info(A.x2());
    }
}

