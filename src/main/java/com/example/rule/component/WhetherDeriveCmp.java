package com.example.rule.component;

import com.example.rule.model.FmPolicyRules;
import com.example.rule.model.MSEvent;
import com.example.rule.model.SupplementaryConditions;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

@LiteflowComponent("whetherDeriveCmp")
@Slf4j
public class WhetherDeriveCmp extends NodeComponent {
    @Override
    public void process() throws Exception {
        SupplementaryConditions spConditions = this.getContextBean(SupplementaryConditions.class);
        List<MSEvent> childrens = spConditions.getChildrens();
        FmPolicyRules fmPolicyRules = spConditions.getFmPolicyRules();
        if (fmPolicyRules.getInheritType() == 2) {
            //衍生
            if (CollectionUtils.isEmpty(childrens)) {
                log.info("当前事件没有子事件，不需要进行判断");
                this.setIsEnd(true);
            }
            if (childrens.size() < fmPolicyRules.getThreshold()) {
                log.info("当前事件子事件小于阈值，不需要进行判断");
                this.setIsEnd(true);
            }
        }
    }
}
