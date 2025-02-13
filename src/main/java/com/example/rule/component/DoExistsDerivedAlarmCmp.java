package com.example.rule.component;

import com.example.rule.model.FmPolicyRules;
import com.example.rule.model.MSEvent;
import com.example.rule.model.SupplementaryConditions;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

@LiteflowComponent("doExistsDerivedAlarmCmp")
public class DoExistsDerivedAlarmCmp extends NodeComponent {
    @Override
    public void process() throws Exception {
        SupplementaryConditions spConditions = this.getContextBean(SupplementaryConditions.class);
        MSEvent msEvent = spConditions.getMsEvent();
        FmPolicyRules fmPolicyRules = spConditions.getFmPolicyRules();
        String secondaryEventId = fmPolicyRules.getSecondaryEventId();
        String primaryEventId = fmPolicyRules.getPrimaryEventId();
        if(StringUtils.isNotBlank(secondaryEventId)&&secondaryEventId.contains(msEvent.getEventId())){
            spConditions.setSubAlarm(true);
            List<MSEvent> parents = spConditions.getParents();
            if(CollectionUtils.isNotEmpty(parents)){
                spConditions.setDoMainSubRelation(true);
            }
        }else if(StringUtils.isNotBlank(primaryEventId)&&primaryEventId.contains(msEvent.getEventId())){
            spConditions.setSubAlarm(false);
            List<MSEvent> childrens = spConditions.getChildrens();
            if(CollectionUtils.isNotEmpty(childrens)){
                spConditions.setDoMainSubRelation(true);
            }
        }

    }

}
