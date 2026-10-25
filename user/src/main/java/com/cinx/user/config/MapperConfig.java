package com.cinx.user.config;

import com.cinx.user.policy.api.mapper.PolicyMapper;
import com.cinx.user.profile.api.mapper.UserMapper;
import org.mapstruct.factory.Mappers;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MapperConfig {
    @Bean
    @ConditionalOnMissingBean(UserMapper.class)
    public UserMapper userMapper() {
        return Mappers.getMapper(UserMapper.class);
    }

    @Bean
    @ConditionalOnMissingBean(PolicyMapper.class)
    public PolicyMapper policyMapper() {
        return Mappers.getMapper(PolicyMapper.class);
    }
}
