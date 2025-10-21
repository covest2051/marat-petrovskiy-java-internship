package userservice.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import userservice.dto.UserResponse;
import userservice.entity.User;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toUserResponse(User user);

    List<UserResponse> toUserResponseList(List<User> users);
}

