package com.example.rule.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@Slf4j
@RequestMapping("/ruleLiteflow")
@RestController
public class RuleNodeController {
    /**
     * 规则引擎节点
     * @return
     * {
     *   "nodes": [
     *     { "id": "A", "name": "节点A" },
     *     { "id": "B", "name": "节点B" },
     *     { "id": "C", "name": "节点C" }
     *   ]
     * }
     */
    @RequestMapping("/ruleNode")
    public String ruleNode(){
        log.info("ruleNode");

        return "ruleNode";
    }
}
