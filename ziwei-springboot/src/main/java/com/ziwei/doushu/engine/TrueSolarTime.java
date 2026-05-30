package com.ziwei.doushu.engine;

import com.ziwei.doushu.model.BirthInfo;

import java.time.LocalDate;

/**
 * 真太阳时与子时跨日处理。严格对齐原项目 lib/ziwei/share.ts。
 */
public final class TrueSolarTime {

    private TrueSolarTime() {
    }

    /**
     * 根据北京时间 + 经度计算真太阳时时辰支 (0-11)。
     * 对齐 share.ts: calcTrueSolarBranch
     */
    public static int calcTrueSolarBranch(int clockHour, int clockMinute, double longitude) {
        int clockMins = clockHour * 60 + clockMinute;
        double offset = (longitude - 120) * 4;
        double solar = (((clockMins + offset) % 1440) + 1440) % 1440;
        if (solar >= 1380 || solar < 60) return 0;
        return (int) Math.floor((solar - 60) / 120) + 1;
    }

    /**
     * 表单 → BirthInfo。对齐 share.ts: formToBirthInfo。
     *
     * 子时规则（倪海厦体系/三合派标准）：
     *  · 23:00-23:59 = 晚子时，按次日排盘（日期 +1）
     *  · 00:00-00:59 = 早子时，按本日排盘
     *
     * @param unknownTime 是否未知时辰（true → hour=0，且不做晚子时进位）
     * @param longitude   经度；若 province 为空则原项目不传 longitude，这里由调用方决定
     */
    public static BirthInfo build(int year, int month, int day,
                                  boolean unknownTime, int clockHour, int clockMinute,
                                  double longitude, String gender,
                                  String name, String province, String city) {
        int y = year, m = month, d = day;

        if (!unknownTime) {
            if (clockHour == 23 && y > 0 && m > 0 && d > 0) {
                LocalDate next = LocalDate.of(y, m, d).plusDays(1);
                y = next.getYear();
                m = next.getMonthValue();
                d = next.getDayOfMonth();
            }
        }

        int hour = unknownTime ? 0 : calcTrueSolarBranch(clockHour, clockMinute, longitude);

        BirthInfo info = new BirthInfo(y, m, d, hour, gender);
        info.name = (name != null && !name.isEmpty()) ? name : null;
        info.province = (province != null && !province.isEmpty()) ? province : null;
        info.city = (city != null && !city.isEmpty()) ? city : null;
        info.longitude = (province != null && !province.isEmpty()) ? longitude : null;
        return info;
    }
}
