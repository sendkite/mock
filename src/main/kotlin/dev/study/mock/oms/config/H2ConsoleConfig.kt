package dev.study.mock.oms.config

import org.h2.server.web.JakartaWebServlet
import org.springframework.boot.web.servlet.ServletRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class H2ConsoleConfig {

    @Bean
    fun h2ServletRegistration(): ServletRegistrationBean<JakartaWebServlet> {
        val servlet = JakartaWebServlet()
        return ServletRegistrationBean(servlet, "/h2-console/*").apply {
            addInitParameter("webAllowOthers", "true")
        }
    }
}