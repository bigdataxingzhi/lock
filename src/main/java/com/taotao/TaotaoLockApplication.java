package com.taotao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
public class TaotaoLockApplication {

	public static void main(String[] args) {
		SpringApplication.run(TaotaoLockApplication.class, args);
	}
}
