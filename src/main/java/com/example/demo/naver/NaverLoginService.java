package com.example.demo.naver;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class NaverLoginService {

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    private final RestClient restClient;

    public NaverLoginService(
            @Value("${naver.client-id}") String clientId,
            @Value("${naver.client-secret}") String clientSecret,
            @Value("${naver.redirect-uri}") String redirectUri) {

        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;

        this.restClient = RestClient.create();
    }

    // 네이버 로그인 페이지 주소 생성
    public String getLoginUrl(String state) {

        return "https://nid.naver.com/oauth2.0/authorize"
                + "?response_type=code"
                + "&client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&state=" + state;
    }

    // 네이버에서 Access Token 받기
    @SuppressWarnings("unchecked")
    public String getAccessToken(
            String code,
            String state) {

        Map<String, Object> response =
                restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .scheme("https")
                                .host("nid.naver.com")
                                .path("/oauth2.0/token")
                                .queryParam(
                                        "grant_type",
                                        "authorization_code"
                                )
                                .queryParam(
                                        "client_id",
                                        clientId
                                )
                                .queryParam(
                                        "client_secret",
                                        clientSecret
                                )
                                .queryParam(
                                        "code",
                                        code
                                )
                                .queryParam(
                                        "state",
                                        state
                                )
                                .build())
                        .retrieve()
                        .body(Map.class);

        if (response == null ||
                response.get("access_token") == null) {

            throw new RuntimeException(
                    "네이버 로그인 토큰 발급에 실패했습니다."
            );
        }

        return response.get("access_token").toString();
    }

    // Access Token으로 네이버 회원 정보 가져오기
    @SuppressWarnings("unchecked")
    public Map<String, Object> getUserInfo(
            String accessToken) {

        Map<String, Object> response =
                restClient.get()
                        .uri(
                                "https://openapi.naver.com/v1/nid/me"
                        )
                        .header(
                                "Authorization",
                                "Bearer " + accessToken
                        )
                        .retrieve()
                        .body(Map.class);

        if (response == null ||
                response.get("response") == null) {

            throw new RuntimeException(
                    "네이버 사용자 정보를 가져오지 못했습니다."
            );
        }

        return (Map<String, Object>)
                response.get("response");
    }
}