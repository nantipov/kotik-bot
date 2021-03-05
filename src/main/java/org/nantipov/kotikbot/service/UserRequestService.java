package org.nantipov.kotikbot.service;

import org.nantipov.kotikbot.domain.UserRequest;
import org.nantipov.kotikbot.domain.UserResponse;
import org.springframework.stereotype.Service;

@Service
public class UserRequestService {
    public UserResponse process(UserRequest userRequest) {
        return new UserResponse();
    }
}
