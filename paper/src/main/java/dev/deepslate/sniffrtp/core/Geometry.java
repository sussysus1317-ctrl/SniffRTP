/*
 * Decompiled with CFR 0.152.
 */
package dev.deepslate.sniffrtp.core;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

public final class Geometry {
    public static Point sample(Area a) {
        return Geometry.sample(a, ThreadLocalRandom.current());
    }

    public static Point column(Point origin, int index) {
        return new Point(origin.x() & 0xFFFFFFF0 | origin.x() + index * 5 & 0xF, origin.z() & 0xFFFFFFF0 | origin.z() + index * 7 & 0xF);
    }

    public static Point sample(Area a, RandomGenerator random) {
        double lowX = Math.max(a.centerX - a.radius, a.border.minX);
        double highX = Math.min(a.centerX + a.radius, a.border.maxX);
        double lowZ = Math.max(a.centerZ - a.radius, a.border.minZ);
        double highZ = Math.min(a.centerZ + a.radius, a.border.maxZ);
        if (highX <= lowX || highZ <= lowZ) {
            return null;
        }
        double areaCircle = Math.PI * (a.radius * a.radius - a.minRadius * a.minRadius);
        if ((highX - lowX) * (highZ - lowZ) < areaCircle * 0.65) {
            return new Point((int)Math.floor(random.nextDouble(lowX, highX)), (int)Math.floor(random.nextDouble(lowZ, highZ)));
        }
        double angle = random.nextDouble() * Math.PI * 2.0;
        double radius = Math.sqrt(a.minRadius * a.minRadius + random.nextDouble() * (a.radius * a.radius - a.minRadius * a.minRadius));
        return new Point((int)Math.floor(a.centerX + Math.cos(angle) * radius), (int)Math.floor(a.centerZ + Math.sin(angle) * radius));
    }

    public static boolean separated(Point p, List<Point> history, Area area, int attempts, int maximum) {
        double progress = (double)attempts / (double)Math.max(1, maximum);
        double relaxation = progress < 0.15 ? 1.0 : Math.max(0.015, Math.pow(1.0 - progress, 4.0));
        double previous = Math.min(area.radius * 0.7, area.span() * 0.6) * relaxation;
        for (int i = 0; i < history.size(); ++i) {
            Point old = history.get(i);
            int cx = Math.abs((p.x >> 4) - (old.x >> 4));
            int cz = Math.abs((p.z >> 4) - (old.z >> 4));
            if (cx <= 1 && cz <= 1) {
                return false;
            }
            double dx = (double)p.x - (double)old.x;
            double dz = (double)p.z - (double)old.z;
            double separation = previous * (i == 0 ? 1.0 : (i < 5 ? 0.5 : 0.12));
            if (!(dx * dx + dz * dz < separation * separation)) continue;
            return false;
        }
        return true;
    }

    public record Area(double centerX, double centerZ, double minRadius, double radius, Border border) {
        public boolean contains(Point p) {
            double dz;
            double dx = (double)p.x() + 0.5 - this.centerX;
            double d = dx * dx + (dz = (double)p.z() + 0.5 - this.centerZ) * dz;
            return d >= this.minRadius * this.minRadius && d <= this.radius * this.radius && this.border.contains(p);
        }

        public double span() {
            double x = Math.max(0.0, Math.min(this.centerX + this.radius, this.border.maxX) - Math.max(this.centerX - this.radius, this.border.minX));
            double z = Math.max(0.0, Math.min(this.centerZ + this.radius, this.border.maxZ) - Math.max(this.centerZ - this.radius, this.border.minZ));
            return Math.min(2.0 * this.radius, Math.hypot(x, z));
        }
    }

    public record Point(int x, int z) {
        public long chunk() {
            return (long)(this.x >> 4) << 32 ^ (long)(this.z >> 4) & 0xFFFFFFFFL;
        }
    }

    public record Border(double minX, double minZ, double maxX, double maxZ) {
        public boolean contains(Point p) {
            return (double)p.x() + 0.2 >= this.minX && (double)p.x() + 0.8 <= this.maxX && (double)p.z() + 0.2 >= this.minZ && (double)p.z() + 0.8 <= this.maxZ;
        }
    }
}

