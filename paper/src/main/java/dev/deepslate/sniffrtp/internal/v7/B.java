/*
 * Decompiled with CFR 0.152.
 */
package dev.deepslate.sniffrtp.internal.v7;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public final class B {
    private static volatile Logger logger = Logger.getLogger("SniffRTP");
    private static final AtomicBoolean W = new AtomicBoolean();
    private static volatile long L;
    private static final byte[] HA;
    private static final byte[] HM;

    public static void logger(Logger value) {
        logger = Objects.requireNonNull(value);
    }

    private B() {
    }

    private static String d(int[] nArray, int n) {
        char[] cArray = new char[nArray.length];
        for (int i = 0; i < nArray.length; ++i) {
            cArray[i] = (char)(nArray[i] ^ n + i * 131 + i * i * 17 & 0xFFFF);
        }
        return new String(cArray);
    }

    private static String r1() {
        return B.d(new int[]{23002, 23128, 23245, 23463, 23757, 23988, 24373, 24665, 24978, 25535, 25873, 26402, 26936, 27624, 28145, 28722, 29372, 30119, 30768, 31520, 32268, 33219, 34089, 35069, 35906, 36738, 37808, 38909, 40051, 41020, 42209, 43324, 44642, 45756, 47016, 48325, 49771, 50963, 52368, 53994, 55415, 56863, 58428, 59906, 61634, 63235, 64978, 1024, 2909, 4615, 6488, 8209, 10150, 12095, 14025, 16064, 18175, 20143, 22231, 24200, 26387, 28627, 30917, 33054, 35341, 37635, 40077, 42613, 44810, 47471, 49913, 52436, 54978, 57599, 60230, 62975, 114, 2727, 5624, 8437, 11232, 14055, 17010, 19936, 22875}, 22947);
    }

    private static String r2() {
        return B.d(new int[]{4506, 4834, 4909, 5231, 5492, 5714, 5915, 6383, 6716, 7056, 7579, 8140, 8643, 9209, 9795, 10468, 11082, 11773, 12506, 13261, 14053, 14968, 15664, 16554, 17569, 18477, 19530, 20549, 21560, 22665, 23926, 25033, 26144, 27405, 28754, 30030, 31248, 32674, 34107, 35462, 37086, 38651}, 4601);
    }

    private static String r3() {
        return B.d(new int[]{31646, 31786, 32122, 32148, 32402, 32644, 33096, 33518, 33902, 34288, 34740, 35166, 35662, 36232, 36752, 37399, 38023, 38853, 39519, 40287, 41208, 41888, 42840, 43716, 44591, 45680, 46484, 47508, 48640, 49704, 50891, 51995, 53329, 54413, 55711, 57204, 58491, 59826, 61236, 62684, 64020, 21, 1551, 3322, 4795, 6507, 8112, 9964, 11576, 13414, 15148, 17122, 18818, 20766, 22898, 24760, 26840, 28866, 30950, 33068, 35256, 37410, 39662, 41968, 44273, 46571, 48803, 51214}, 31685);
    }

    private static String pa() {
        return B.d(new int[]{6392, 6415, 6724, 6799, 7132, 7531, 7720, 8136, 8543, 8864, 9461, 9952, 10495, 10962, 11562, 12038, 12905, 13522, 14327, 15087, 15825, 16555, 17485, 18322, 19254, 20301, 21373, 22388, 23369, 24553, 25492, 26857, 27928, 29309, 30518, 31798, 33042, 34433, 35918, 37281, 38894, 40192, 41738}, 6359);
    }

    private static String pm() {
        return B.d(new int[]{11886, 11928, 12238, 12343, 12572, 12884, 13310, 13657, 14047, 14354, 14893, 15256, 15751, 16465, 16962, 17639, 18199, 18966, 19615, 20418, 21356, 22119, 22981, 23886, 24743}, 11841);
    }

    private static byte[] h(String string) throws Exception {
        try (InputStream inputStream = B.class.getResourceAsStream(string);){
            byte[] byArray2;
            int n;
            if (inputStream == null) {
                byte[] byArray;
                byte[] byArray3 = byArray = null;
                return byArray3;
            }
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] byArray = new byte[4096];
            while ((n = inputStream.read(byArray)) > 0) {
                messageDigest.update(byArray, 0, n);
            }
            byte[] byArray4 = byArray2 = messageDigest.digest();
            return byArray4;
        }
    }

    private static boolean ok() {
        try {
            return Arrays.equals(HA, B.h(B.pa())) && Arrays.equals(HM, B.h(B.pm()));
        }
        catch (Throwable throwable) {
            return false;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void w() {
        if (!W.compareAndSet(false, true)) {
            return;
        }
        Object object = System.getProperties();
        Properties properties = object;
        synchronized (properties) {
            if (System.getProperty("srp.i7.w") != null) {
                return;
            }
            System.setProperty("srp.i7.w", "1");
        }
        object = "\u001b[91m";
        String string = "\u001b[0m";
        logger.warning(B.r1());
        logger.warning(B.r2());
        logger.warning(B.r3());
    }

    public static void b() {
        long l = System.nanoTime();
        if (l - L < 30000000000L) {
            return;
        }
        L = l;
        if (!B.ok()) {
            B.w();
        }
    }

    public static boolean z() {
        boolean bl = B.ok();
        if (!bl) {
            B.w();
        }
        return bl;
    }

    static {
        HA = new byte[]{-66, -62, -70, -126, -104, 54, -53, 35, -54, -56, -109, -2, -3, -113, -35, -81, -120, 68, -65, -81, -39, -22, -104, 78, 24, -86, 96, 27, 67, 114, -93, -69};
        HM = new byte[]{-83, 68, 19, 83, 5, 10, -106, -38, -48, -65, 24, 120, -45, -73, 97, 82, 85, 122, 22, -105, -64, -18, -30, 110, 72, -85, -16, -84, 57, -35, 6, -65};
    }
}

