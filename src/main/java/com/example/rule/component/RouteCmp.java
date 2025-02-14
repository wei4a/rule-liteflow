package com.example.rule.component;

import com.example.rule.model.FmPolicyRules;
import com.example.rule.model.SupplementaryConditions;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeBooleanComponent;
@LiteflowComponent("routeCmp")
public class RouteCmp extends NodeBooleanComponent {
    @Override
    public boolean processBoolean() throws Exception {
        SupplementaryConditions spConditions = this.getContextBean(SupplementaryConditions.class);
        FmPolicyRules fmPolicyRules = spConditions.getFmPolicyRules();
        return true;
    }
}
