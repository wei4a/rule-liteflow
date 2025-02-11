package com.example.rule.component.script;

import com.example.rule.model.MSEvent;
import com.example.rule.model.SupplementaryConditions;
import com.yomahub.liteflow.script.ScriptExecuteWrap;
import com.yomahub.liteflow.script.body.CommonScriptBody;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class WhetherDeriveScript implements CommonScriptBody {
    @Override
    public Void body(ScriptExecuteWrap scriptExecuteWrap) {
        SupplementaryConditions spConditions = ContextAwareHolder.loadContextAware().getBean(SupplementaryConditions.class);
        //示例 网元去重之后大于阈值衍生
        List<MSEvent> childrens = spConditions.getChildrens();
        //遍历childrens 获取网元名称并去重计数
        if (CollectionUtils.isNotEmpty(childrens)) {
            Set<String> netElementNames = new HashSet<>();
            for (MSEvent children : childrens) {
                netElementNames.add(children.getDeviceName());
            }
            if (netElementNames.size() > spConditions.getFmPolicyRules().getThreshold()) {
                log.info("当前事件子事件大于阈值，衍生");
                spConditions.setDoDerive(true);
            } else {
                log.info("当前事件子事件小于阈值，不进行判断");
            }
        }
        return null;
    }
}
