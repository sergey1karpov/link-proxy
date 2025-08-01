package com.linker.linker.mapper;

import com.linker.linker.dto.auth.RegisterRequestDto;
import com.linker.linker.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuthMapper {
    @Mapping(target = "role", constant = "ROLE_USER")
    User toEntity(RegisterRequestDto request);
}
