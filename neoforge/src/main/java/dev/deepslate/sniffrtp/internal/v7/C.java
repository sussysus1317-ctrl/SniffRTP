/*
 * Decompiled with CFR 0.152.
 */
package dev.deepslate.sniffrtp.internal.v7;

import dev.deepslate.sniffrtp.internal.v7.A;
import dev.deepslate.sniffrtp.internal.v7.B;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public final class C {
    private static volatile Logger logger = Logger.getLogger("SniffRTP");
    private static final AtomicBoolean W = new AtomicBoolean();
    private static volatile long L;
    private static final byte[] HA;
    private static final byte[] HB;
    private static final byte[] HM;

    public static void c(Logger value) {
        logger = Objects.requireNonNull(value);
        A.logger(value);
        B.logger(value);
        C.c();
    }

    private C() {
    }

    private static String d(int[] nArray, int n) {
        char[] cArray = new char[nArray.length];
        for (int i = 0; i < nArray.length; ++i) {
            cArray[i] = (char)(nArray[i] ^ n + i * 131 + i * i * 17 & 0xFFFF);
        }
        return new String(cArray);
    }

    private static String r1() {
        return C.d(new int[]{20456, 20554, 20731, 20945, 21215, 21414, 21803, 22087, 22400, 22957, 23335, 23892, 24362, 24858, 25503, 26204, 26798, 27477, 28198, 28982, 29758, 30705, 31447, 32259, 33392, 34224, 35238, 36331, 37249, 38446, 39567, 40786, 41872, 43182, 44510, 45811, 47225, 48385, 49806, 51444, 52837, 54285, 55882, 57396, 59088, 60721, 62460, 64046, 335, 2101, 3918, 5639, 7636, 9549, 11511, 13566, 15373, 17501, 19649, 21662, 23841, 26049, 28395, 30512, 32831, 35089, 37563, 39811, 42264, 44925, 47335, 49866, 52432, 55021, 57712, 60169, 63072, 213, 2838, 5659, 8690, 11413, 14436, 17398, 20329}, 20369);
    }

    private static String r2() {
        return C.d(new int[]{8398, 8494, 8601, 8891, 9128, 9350, 9815, 10147, 10600, 10956, 11343, 11832, 12303, 12973, 13503, 14136, 14782, 15537, 16142, 16953, 17753, 18636, 19580, 20454, 21269, 22161, 23230, 24209, 25460, 26493, 27562, 28725, 30068, 31169, 32422, 33690, 35148, 36598, 38007, 39370, 40714, 42279}, 8365);
    }

    private static String r3() {
        return C.d(new int[]{27244, 27320, 27636, 27690, 27936, 28182, 28638, 29048, 29436, 29698, 30154, 30672, 31196, 31802, 32262, 32929, 33557, 34423, 35025, 35777, 36714, 37458, 38382, 39282, 40157, 41186, 41994, 43034, 44210, 45242, 46461, 47501, 48867, 49951, 51217, 52490, 53897, 55328, 56738, 58186, 59526, 61095, 62641, 64372, 297, 1945, 3622, 5274, 7082, 8852, 10658, 12668, 14352, 16300, 18180, 20302, 22378, 24400, 26488, 28578, 30666, 32944, 35096, 37478, 39555, 42105, 44333, 46768}, 27191);
    }

    private static String pa() {
        return C.d(new int[]{6686, 6817, 6942, 7205, 7522, 7693, 8130, 8546, 8953, 9310, 9631, 10170, 10641, 11380, 11888, 12476, 13071, 13948, 14477, 15237, 16015, 16973, 17895, 18744, 19664, 20499, 21527, 22542, 23783, 24719, 25902, 27059, 28350, 29459, 30828, 32092, 33516, 34855, 36324, 37707, 39048, 40638, 42144}, 6705);
    }

    private static String pb() {
        return C.d(new int[]{27690, 27901, 27946, 28241, 28430, 28761, 28958, 29374, 29741, 30322, 30635, 31182, 31693, 32128, 32796, 33488, 34139, 34688, 35545, 36305, 37027, 38009, 38683, 39620, 40676, 41535, 42563, 43610, 44571, 45787, 46914, 48095, 49226, 50511, 51736, 53096, 54467, 55795, 57144, 58519, 60124, 61650, 63188}, 27653);
    }

    private static String pm() {
        return C.d(new int[]{15704, 15942, 16004, 16333, 16594, 16770, 17060, 17411, 17801, 18268, 18711, 19282, 19801, 20263, 20872, 21549, 22209, 22888, 23637, 24344, 25122, 25937, 26815, 27700, 28561}, 15735);
    }

    private static String cn() {
        return C.d(new int[]{24181, 24256, 24365, 24605, 24905, 25132, 25570, 25751, 26138, 26721, 27058, 27599, 28064, 28639, 29260, 29889, 30504, 31123, 31917, 32689, 33449, 34409, 35161, 35998, 37111, 37929, 38950, 39993, 40987, 42144, 43331, 44433, 45575, 46962, 48149, 49426}, 24081);
    }

    private static byte[] h(String string) throws Exception {
        try (InputStream inputStream = C.class.getResourceAsStream(string);){
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
            return Arrays.equals(HA, C.h(C.pa())) && Arrays.equals(HB, C.h(C.pb())) && Arrays.equals(HM, C.h(C.pm()));
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
        logger.warning(C.r1());
        logger.warning(C.r2());
        logger.warning(C.r3());
    }

    private static void banner() {
        try {
            Class<?> clazz = Class.forName(C.cn(), true, C.class.getClassLoader());
            Method method = clazz.getDeclaredMethod("a", new Class[0]);
            method.invoke(null, new Object[0]);
        }
        catch (Throwable throwable) {
            C.w();
        }
    }

    public static void c() {
        if (C.ok()) {
            C.banner();
        } else {
            C.w();
        }
    }

    public static void d() {
        long l = System.nanoTime();
        if (l - L < 30000000000L) {
            return;
        }
        L = l;
        if (!C.ok()) {
            C.w();
        }
    }

    static {
        HA = new byte[]{-66, -62, -70, -126, -104, 54, -53, 35, -54, -56, -109, -2, -3, -113, -35, -81, -120, 68, -65, -81, -39, -22, -104, 78, 24, -86, 96, 27, 67, 114, -93, -69};
        HB = new byte[]{-49, -31, 50, 42, 106, 115, -25, -80, 124, -67, 87, 120, -26, -8, 17, 29, 41, -11, -71, 15, -108, -127, -95, 84, -11, -118, -40, 107, -5, -82, 94, 8};
        HM = new byte[]{-83, 68, 19, 83, 5, 10, -106, -38, -48, -65, 24, 120, -45, -73, 97, 82, 85, 122, 22, -105, -64, -18, -30, 110, 72, -85, -16, -84, 57, -35, 6, -65};
    }
}

