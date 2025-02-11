package com.example.rule.component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.rule.model.FmPolicyRules;
import com.example.rule.model.MSEvent;
import com.example.rule.model.SupplementaryConditions;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import java.util.List;

@LiteflowComponent("parentAndChildCmp")
public class ParentAndChildCmp extends NodeComponent {
    @Override
    public void process() throws Exception {
        SupplementaryConditions spConditions = this.getContextBean(SupplementaryConditions.class);
        List<MSEvent> parents = spConditions.getParents();
        List<MSEvent> childrens = spConditions.getChildrens();
        FmPolicyRules fmPolicyRules = spConditions.getFmPolicyRules();
        LambdaQueryWrapper<MSEvent> derivedAlarmParams = spConditions.getDerivedAlarmParams();
        LambdaQueryWrapper<MSEvent> triggeredParams = spConditions.getTriggeredParams();
        if(fmPolicyRules.getInheritType()==2){
            //衍生
            Page<MSEvent> page = new Page<>(1, 1000);
            Page<MSEvent> derivedAlarms = spConditions.getFmService().page(page, derivedAlarmParams);
            Page<MSEvent> childs = spConditions.getFmService().page(page, triggeredParams);
            parents.addAll(derivedAlarms.getRecords());
            childrens.addAll(childs.getRecords());
        }
    }
}
