package dev.aaf.parkourArea.util;

import dev.aaf.parkourArea.zone.ZoneSpawn;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Locations.resolveYawPitch：朝向逐字段 fallback（orient spawn → pos spawn → fallback）。 */
class LocationsTest {

    @Test
    void orientSpawnWins() {
        ZoneSpawn pos = ZoneSpawn.of(null, null, null, 10.0, 20.0);
        ZoneSpawn orient = ZoneSpawn.of(null, null, null, 90.0, 45.0);
        float[] yp = Locations.resolveYawPitch(pos, orient, 0f, 0f);
        assertThat(yp[0]).isEqualTo(90f);
        assertThat(yp[1]).isEqualTo(45f);
    }

    @Test
    void posSpawnUsedWhenOrientMissing() {
        ZoneSpawn pos = ZoneSpawn.of(null, null, null, 10.0, 20.0);
        float[] yp = Locations.resolveYawPitch(pos, null, 0f, 0f);
        assertThat(yp[0]).isEqualTo(10f);
        assertThat(yp[1]).isEqualTo(20f);
    }

    @Test
    void fallbackUsedWhenNoSpawn() {
        float[] yp = Locations.resolveYawPitch(null, null, 123f, -45f);
        assertThat(yp[0]).isEqualTo(123f);
        assertThat(yp[1]).isEqualTo(-45f);
    }

    @Test
    void fieldsResolveIndependently() {
        // orient 只指定 yaw → yaw 用 orient，pitch 落 fallback
        ZoneSpawn orient = ZoneSpawn.of(null, null, null, 90.0, null);
        float[] yp = Locations.resolveYawPitch(null, orient, 5f, 6f);
        assertThat(yp[0]).isEqualTo(90f);
        assertThat(yp[1]).isEqualTo(6f);
    }

    @Test
    void orientYawWithPosPitch() {
        // orient 只有 yaw、pos 只有 pitch → yaw=orient、pitch=pos
        ZoneSpawn pos = ZoneSpawn.of(null, null, null, null, 30.0);
        ZoneSpawn orient = ZoneSpawn.of(null, null, null, 180.0, null);
        float[] yp = Locations.resolveYawPitch(pos, orient, 0f, 0f);
        assertThat(yp[0]).isEqualTo(180f);
        assertThat(yp[1]).isEqualTo(30f);
    }
}
