package com.linker.linker.mapper;

import com.linker.linker.dto.auth.RegisterRequestDto;
import com.linker.linker.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuthMapper {
    User toEntity(RegisterRequestDto request);
}
