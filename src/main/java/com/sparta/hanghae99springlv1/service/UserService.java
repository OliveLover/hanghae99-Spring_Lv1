package com.sparta.hanghae99springlv1.service;

import com.sparta.hanghae99springlv1.dto.LoginRequestDto;
import com.sparta.hanghae99springlv1.dto.SignupRequestDto;
import com.sparta.hanghae99springlv1.entity.User;
import com.sparta.hanghae99springlv1.entity.UserRoleEnum;
import com.sparta.hanghae99springlv1.jwt.JwtUtil;
import com.sparta.hanghae99springlv1.message.Message;
import com.sparta.hanghae99springlv1.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserService {

    final private UserRepository userRepository;
    final private JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    // ADMIN_TOKEN
    private static final String ADMIN_TOKEN = "AAABnvxRVklrnYxKZ0aHgTBcXukeZygoC";

    @Transactional
    public Message signup(SignupRequestDto signupRequestDto) {
        String username = signupRequestDto.getUsername();
        String password = signupRequestDto.getPassword();

        // 입력 형식 확인
        // username 불일치, password 일치
        if (!Pattern.matches("^[a-z0-9]{4,10}$", username) && Pattern.matches("^[a-zA-Z0-9!@#$%^&*()_+]{8,15}$", password)) {
             return new Message("아이디는 최소 4자 이상, 10자 이하이며 알파벳 소문자(a-z), 숫자(0-9)로 구성되어야 합니다.", 400);
        }
        // username 일치, password 불일치
        else if (Pattern.matches("^[a-z0-9]{4,10}$", username) && !Pattern.matches("^[a-zA-Z0-9!@#$%^&*()_+]{8,15}$", password)) {
            return new Message("비밀번호는 최소 8자 이상, 15자 이하이며 알파벳 대소문자(a-z, A-Z), 숫자(0-9) 및 특수문자로 구성되어야 합니다.", 400);
        }
        // username 불일치, password 불일치
        else if (!Pattern.matches("^[a-z0-9]{4,10}$", username) && !Pattern.matches("^[a-zA-Z0-9!@#$%^&*()_+]{8,15}$", password)) {
            return new Message("아이디는 최소 4자 이상, 10자 이하이며 알파벳 소문자(a-z), 숫자(0-9)로 구성되어야 하며,\n" +
                                    "비밀번호는 최소 8자 이상, 15자 이하이며 알파벳 대소문자(a-z, A-Z), 숫자(0-9) 및 특수문자로 구성되어야 합니다.",
                                400);
        }

        // 회원 중복 확인
        Optional<User> found = userRepository.findByUsername(username);
        if (found.isPresent()) {
            throw new IllegalArgumentException("사용중인 아이디입니다.");
        }

        // 사용자 ROLE 확인
        UserRoleEnum role = UserRoleEnum.USER;
        if (signupRequestDto.isAdmin()) {
            if (!signupRequestDto.getAdminToken().equals(ADMIN_TOKEN)) {
                throw new IllegalArgumentException("관리자 암호가 틀려 등록이 불가능합니다.");
            }
            role = UserRoleEnum.ADMIN;
        }

        User user = new User(username, passwordEncoder.encode(password), role);
        userRepository.save(user);
        return new Message("회원가입 성공", 200);
    }

    // Security + Jwt 를 사용한 로그인
    @Transactional(readOnly = true)
    public Message login(LoginRequestDto loginRequestDto, HttpServletResponse response) {
        String username = loginRequestDto.getUsername();
        String password = loginRequestDto.getPassword();

        // 사용자 확인
        User user = userRepository.findByUsername(username).orElseThrow(
                () -> new IllegalArgumentException("등록된 사용자가 없습니다.")
        );
        // 비밀번호 확인
        if(!passwordEncoder.matches(password, user.getPassword())){
            throw  new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        response.addHeader(JwtUtil.AUTHORIZATION_HEADER, jwtUtil.createToken(user.getUsername(), user.getRole()));
        return new Message("로그인 성공", 200);
    }
}
