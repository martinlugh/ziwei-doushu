package com.ziwei.doushu.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * 格局。对齐原项目 patterns.ts: Pattern
 *   level: 'excellent' | 'good' | 'neutral' | 'caution'
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Pattern {
    public String name;
    public String level;
    public String description;
    public List<String> palaces;        // 涉及宫位
    public PatternCondition conditions;  // 成立条件分层
    public String source;                // 古籍出处

    public Pattern() {
    }
}
