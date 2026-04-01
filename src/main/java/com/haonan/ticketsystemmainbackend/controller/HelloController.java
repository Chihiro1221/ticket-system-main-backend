package com.haonan.ticketsystemmainbackend.controller;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hello")
@Slf4j
public class HelloController {

    @PostConstruct
    public void init() {
        System.out.println("HelloController 初始化完成！");
        //log.info("HelloController 已加载！");
    }
    @GetMapping("/")
    public String hello() {
        return "Hello World!";
    }
}
