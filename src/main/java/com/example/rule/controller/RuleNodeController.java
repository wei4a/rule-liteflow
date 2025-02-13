package com.example.rule.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.rule.model.FmPolicyRulesScript;
import com.example.rule.service.FmPolicyRulesScriptService;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequestMapping("/ruleLiteflow")
@RestController
public class RuleNodeController {
    private static final String ID_KEY = "id";
    private static final String NAME_KEY = "name";
    private static final String NODES_KEY = "nodes";
    @Resource
    private FmPolicyRulesScriptService fmPolicyRulesScriptService;

    /**
     * 规则引擎节点
     *
     * @return {
     * "nodes": [
     * { "id": "A", "name": "节点A" },
     * { "id": "B", "name": "节点B" },
     * { "id": "C", "name": "节点C" }
     * ]
     * }
     */
    @RequestMapping("/ruleNode")
    public String ruleNode() {
        try {
            log.info("开始获取规则引擎节点数据");

            List<FmPolicyRulesScript> list = fmPolicyRulesScriptService.list(
                    Wrappers.lambdaQuery(FmPolicyRulesScript.class)
                            .in(FmPolicyRulesScript::getEnable, Lists.newArrayList(1, -1))
            );

            JSONArray nodes = list.stream()
                    .map(fmPolicyRulesScript -> {
                        JSONObject node = new JSONObject();
                        node.put(ID_KEY, fmPolicyRulesScript.getScriptKey());
                        node.put(NAME_KEY, fmPolicyRulesScript.getScriptName());
                        return node;
                    })
                    .collect(Collectors.toCollection(JSONArray::new));
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(NODES_KEY, nodes);

            log.info("成功获取规则引擎节点数据");
            return JSON.toJSONString(jsonObject);
        } catch (Exception e) {
            log.error("获取规则引擎节点数据失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取规则引擎节点数据失败", e);
        }
    }
}
