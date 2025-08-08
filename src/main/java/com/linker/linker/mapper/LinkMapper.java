package com.linker.linker.mapper;

import com.linker.linker.dto.request.LinkDtoRequest;
import com.linker.linker.entity.Link;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LinkMapper {
    @Mapping(target = "status", defaultValue = "PUBLIC")
    Link toEntity(LinkDtoRequest request);
}
