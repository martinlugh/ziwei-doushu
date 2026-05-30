package com.ziwei.doushu.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * 格局成立条件分层。对齐原项目 patterns.ts: PatternCondition
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PatternCondition {
    public List<String> required;   // 必须满足条件（已通过的）
    public List<String> bonus;      // 加分项（已触发）
    public List<String> breaking;   // 破格警示（已触发）

    public PatternCondition() {
    }

    public PatternCondition(List<String> required, List<String> bonus, List<String> breaking) {
        this.required = required;
        this.bonus = bonus;
        this.breaking = breaking;
    }
}
