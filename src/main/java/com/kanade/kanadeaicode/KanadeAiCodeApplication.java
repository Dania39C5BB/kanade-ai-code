package com.kanade.kanadeaicode;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.kanade.kanadeaicode.mapper")
public class KanadeAiCodeApplication {

    public static void main(String[] args) {
        SpringApplication.run(KanadeAiCodeApplication.class, args);
    }

}
