package dev.aaf.parkourArea.util;

import dev.aaf.parkourArea.zone.ZoneSpawn;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Locations.resolveYawPitch：朝向逐字段 fallback（orient spawn → pos spawn → fallback）。 */
class LocationsTest {

    @Test
    void orientSpawnWins() {
        ZoneSpawn pos = ZoneSpawn.of(null, null, null, 10.0, 20.0);
        ZoneSpawn orient = ZoneSpawn.of(null, null, null, 90.0, 45.0);
        float[] yp = Locations.resolveYawPitch(pos, orient, 0f, 0f);
        assertEquals(90f, yp[0], 0.001);
        assertEquals(45f, yp[1], 0.001);
    }

    @Test
    void posSpawnUsedWhenOrientMissing() {
        ZoneSpawn pos = ZoneSpawn.of(null, null, null, 10.0, 20.0);
        float[] yp = Locations.resolveYawPitch(pos, null, 0f, 0f);
        assertEquals(10f, yp[0], 0.001);
        assertEquals(20f, yp[1], 0.001);
    }

    @Test
    void fallbackUsedWhenNoSpawn() {
        float[] yp = Locations.resolveYawPitch(null, null, 123f, -45f);
        assertEquals(123f, yp[0], 0.001);
        assertEquals(-45f, yp[1], 0.001);
    }

    @Test
    void fieldsResolveIndependently() {
        // orient 只指定 yaw → yaw 用 orient，pitch 落 fallback
        ZoneSpawn orient = ZoneSpawn.of(null, null, null, 90.0, null);
        float[] yp = Locations.resolveYawPitch(null, orient, 5f, 6f);
        assertEquals(90f, yp[0], 0.001);
        assertEquals(6f, yp[1], 0.001);
    }

    @Test
    void orientYawWithPosPitch() {
        // orient 只有 yaw、pos 只有 pitch → yaw=orient、pitch=pos
        ZoneSpawn pos = ZoneSpawn.of(null, null, null, null, 30.0);
        ZoneSpawn orient = ZoneSpawn.of(null, null, null, 180.0, null);
        float[] yp = Locations.resolveYawPitch(pos, orient, 0f, 0f);
        assertEquals(180f, yp[0], 0.001);
        assertEquals(30f, yp[1], 0.001);
    }
}
