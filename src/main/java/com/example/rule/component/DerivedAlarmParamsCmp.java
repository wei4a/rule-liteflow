package com.example.rule.component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.rule.model.FmPolicyRules;
import com.example.rule.model.MSEvent;
import com.example.rule.model.SupplementaryConditions;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

@LiteflowComponent("derivedAlarmParamsCmp")
public class DerivedAlarmParamsCmp extends NodeComponent {
    @Override
    public void process() throws Exception {
        SupplementaryConditions spConditions = this.getContextBean(SupplementaryConditions.class);
        FmPolicyRules fmPolicyRules = spConditions.getFmPolicyRules();
        LambdaQueryWrapper<MSEvent> commonParams = spConditions.getCommonParams();
        LambdaQueryWrapper<MSEvent> derivedAlarmParams =commonParams.clone();
        derivedAlarmParams.eq(MSEvent::getEventId, fmPolicyRules.getPrimaryEventId());
        spConditions.setDerivedAlarmParams(derivedAlarmParams);
    }
}
