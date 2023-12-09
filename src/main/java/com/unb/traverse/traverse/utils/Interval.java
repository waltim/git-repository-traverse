package com.unb.traverse.traverse.utils;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import lombok.Builder;
import lombok.RequiredArgsConstructor;

@Builder
@RequiredArgsConstructor
public class Interval {

    public final Date begin;

    public final Date end;

    public enum Unit {
        Days(TimeUnit.DAYS);

        private final TimeUnit unit;

        Unit(TimeUnit u) {
            unit = u;
        }
    }

    /**
     * Do a diff on a given interval d-e
     *
     * @param d
     * @param e
     * @param u
     * @return A number representing the diff
     */
    public static long diff(Date d, Date e, Unit u) {
        long diff = Math.abs(d.getTime() - e.getTime());

        return u.unit.convert(diff, TimeUnit.MILLISECONDS);
    }
}

