package software.pxel.learneasy.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import software.pxel.learneasy.controller.api.UserApi;
import software.pxel.learneasy.api.dto.user.UpdateProfileDTO;
import software.pxel.learneasy.api.dto.user.UpdateUserDTO;
import software.pxel.learneasy.api.dto.user.UserDTO;
import software.pxel.learneasy.model.User;
import software.pxel.learneasy.service.UserService;

import static software.pxel.learneasy.constants.ApiRoutes.USER_URI;

@RestController
@RequestMapping(USER_URI)
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final UserService userService;

    // С /{id} не конфликтует: Spring выбирает маршрут по специфичности, и
    // литеральный сегмент выигрывает у шаблона с переменной.
    @GetMapping("/me")
    @PreAuthorize("hasAnyAuthority('USER','ADMIN')")
    public ResponseEntity<UserDTO> getCurrentUser(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(userService.getCurrentUserProfile(currentUser));
    }

    @PatchMapping("/me")
    @PreAuthorize("hasAnyAuthority('USER','ADMIN')")
    public ResponseEntity<UserDTO> updateCurrentUser(@Valid @RequestBody UpdateProfileDTO updateDTO,
                                                     @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(userService.updateCurrentUserProfile(currentUser, updateDTO));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Page<UserDTO>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "username,asc") String sort) {
        Sort sortOrder = Sort.by(Sort.Direction.fromString(sort.split(",")[1]), sort.split(",")[0]);
        Pageable pageable = PageRequest.of(page, size, sortOrder);
        return ResponseEntity.ok(userService.findAllUsers(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findUserById(id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id,
                                              @RequestBody UpdateUserDTO updateDTO,
                                              @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(userService.updateUser(id, updateDTO, currentUser));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
