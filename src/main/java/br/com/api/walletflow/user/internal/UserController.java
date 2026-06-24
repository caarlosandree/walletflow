package br.com.api.walletflow.user.internal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
class UserController {

    private final UserService userService;

    @PostMapping
    ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        RegisterResult result = userService.register(request);
        UserResponse body = new UserResponse(result.id(), result.fullName(), result.document(),
                result.email(), result.type());
        return ResponseEntity.created(URI.create("/users/" + result.id())).body(body);
    }
}
