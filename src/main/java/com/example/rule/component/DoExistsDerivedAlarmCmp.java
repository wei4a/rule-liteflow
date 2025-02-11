package com.example.rule.component;

import com.example.rule.model.MSEvent;
import com.example.rule.model.SupplementaryConditions;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@LiteflowComponent("doExistsDerivedAlarmCmp")
public class DoExistsDerivedAlarmCmp extends NodeComponent {
    @Override
    public void process() throws Exception {
        SupplementaryConditions spConditions = this.getContextBean(SupplementaryConditions.class);
        List<MSEvent> parents = spConditions.getParents();
        MSEvent msEvent = spConditions.getMsEvent();
        Long recordId = msEvent.getRecordId();
        if(CollectionUtils.isNotEmpty(parents)){
            List<Long> childIds = new ArrayList<>();
            childIds.add(recordId);
            doExistsDerivedAlarm(parents.get(0), childIds);
        }
    }

    private void doExistsDerivedAlarm(MSEvent msEvent, List<Long> childIds) {

    }
}
