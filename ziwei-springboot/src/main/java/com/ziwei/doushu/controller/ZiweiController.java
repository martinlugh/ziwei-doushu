package com.ziwei.doushu.controller;

import com.ziwei.doushu.engine.TrueSolarTime;
import com.ziwei.doushu.engine.ZiweiEngine;
import com.ziwei.doushu.model.BirthInfo;
import com.ziwei.doushu.model.Pattern;
import com.ziwei.doushu.model.ZiweiChart;
import com.ziwei.doushu.service.PatternsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 紫微斗数排盘 REST 接口。
 */
@RestController
public class ZiweiController {

    private final ZiweiEngine engine;
    private final PatternsService patternsService;

    public ZiweiController(ZiweiEngine engine, PatternsService patternsService) {
        this.engine = engine;
        this.patternsService = patternsService;
    }

    /**
     * 直接用 BirthInfo（已是时辰地支索引 0~11）排盘。
     */
    @PostMapping("/api/chart")
    public ZiweiChart chart(@RequestBody BirthInfo birthInfo) {
        return engine.generateChart(birthInfo);
    }

    /**
     * 用钟表时间 + 经度排盘（真太阳时校正、晚子时进位，对齐 share.ts）。
     */
    @GetMapping("/api/chart")
    public Map<String, Object> chartByClock(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam int day,
            @RequestParam(defaultValue = "false") boolean unknownTime,
            @RequestParam(defaultValue = "8") int clockHour,
            @RequestParam(defaultValue = "0") int clockMinute,
            @RequestParam(defaultValue = "120") double longitude,
            @RequestParam(defaultValue = "male") String gender,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city) {
        BirthInfo info = TrueSolarTime.build(year, month, day, unknownTime, clockHour, clockMinute,
                longitude, gender, name, province, city);
        ZiweiChart chart = engine.generateChart(info);
        List<Pattern> patterns = patternsService.detectPatterns(chart);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("chart", chart);
        result.put("patterns", patterns);
        result.put("mingGongSummary", patternsService.getMingGongSummary(chart));
        return result;
    }

    /**
     * 仅返回格局识别结果。
     */
    @PostMapping("/api/patterns")
    public List<Pattern> patterns(@RequestBody BirthInfo birthInfo) {
        ZiweiChart chart = engine.generateChart(birthInfo);
        return patternsService.detectPatterns(chart);
    }
}
