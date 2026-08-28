package software.pxel.learneasy.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import software.pxel.learneasy.api.dto.auth.RegisterRequest;
import software.pxel.learneasy.api.dto.user.UpdateProfileDTO;
import software.pxel.learneasy.api.dto.user.UpdateUserDTO;
import software.pxel.learneasy.api.dto.user.UserDTO;
import software.pxel.learneasy.model.User;
import software.pxel.learneasy.model.UserProfile;
import software.pxel.learneasy.model.enums.UserRole;

@Mapper(componentModel = "spring", imports = UserRole.class)
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "role", expression = "java(UserRole.USER)")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toUser(RegisterRequest registerRequest);

    /**
     * Профильные поля берутся из {@code profile}, который может отсутствовать:
     * строка в user_profiles создаётся лениво, при первом сохранении профиля.
     * MapStruct генерирует для этого null-безопасный доступ.
     */
    @Mapping(target = "id", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "role", source = "user.role")
    @Mapping(target = "createdAt", source = "user.createdAt")
    @Mapping(target = "firstName", source = "profile.firstName")
    @Mapping(target = "lastName", source = "profile.lastName")
    @Mapping(target = "gender", source = "profile.gender")
    @Mapping(target = "phone", source = "profile.phone")
    @Mapping(target = "address", source = "profile.address")
    @Mapping(target = "birthDate", source = "profile.birthDate")
    @Mapping(target = "country", source = "profile.country")
    @Mapping(target = "city", source = "profile.city")
    @Mapping(target = "avatarKey", source = "profile.avatarKey")
    UserDTO toDto(User user, UserProfile profile);

    /**
     * Для мест, где профиль не нужен или ещё не загружен.
     */
    default UserDTO toDto(User user) {
        return toDto(user, null);
    }

    /**
     * PATCH-семантика: null в DTO означает «не менять», а не «обнулить».
     * Без {@code NullValuePropertyMappingStrategy.IGNORE} частичное обновление
     * затирало бы незаданные поля - для username это ещё и нарушение NOT NULL.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "role", ignore = true)
    void updateUserFromDto(UpdateUserDTO dto, @MappingTarget User user);

    /**
     * Переносит в профиль только те поля, что относятся к user_profiles.
     * Email живёт в users и обновляется отдельно - у него своя проверка
     * уникальности.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateProfileFromDto(UpdateProfileDTO dto, @MappingTarget UserProfile profile);
}
