package com.linker.linker.mapper;

import com.linker.linker.dto.request.UserDtoRequest;
import com.linker.linker.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "role", constant = "ROLE_USER")
    User toEntity(UserDtoRequest request);
}
