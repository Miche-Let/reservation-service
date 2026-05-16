package com.michelet.reservation.config;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

@Configuration
public class DataSourceConfig {

    // spring.datasource.hikari.* 전체 바인딩 — application.yaml의 pool 설정 유지
    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariDataSource hikariDataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    // @Transactional 진입 시 커넥션 획득을 첫 SQL 실행 시점까지 지연.
    // getList() 캐시 히트 경로에서 SQL 없이 반환되면 HikariCP 커넥션을 획득하지 않는다.
    @Bean
    @Primary
    public DataSource dataSource(HikariDataSource hikariDataSource) {
        return new LazyConnectionDataSourceProxy(hikariDataSource);
    }
}
