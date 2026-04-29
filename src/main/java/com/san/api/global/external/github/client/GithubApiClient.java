package com.san.api.global.external.github.client;

import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.AuthErrorCode;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.external.github.dto.GithubAccessTokenResponse;
import com.san.api.global.external.github.dto.GithubUserProfile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * GitHub OAuth와 사용자 API 통신을 담당하는 클라이언트입니다.
 *
 * 외부 API 실패는 도메인 예외로 변환해 상위 계층이 GitHub 응답 구조에
 * 직접 의존하지 않도록 합니다.
 */
@Component
public class GithubApiClient {

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String scope;

    public GithubApiClient(
            @Value("${oauth.github.client-id}") String clientId,
            @Value("${oauth.github.client-secret}") String clientSecret,
            @Value("${oauth.github.redirect-uri}") String redirectUri,
            @Value("${oauth.github.scope}") String scope) {
        this.restClient = RestClient.builder().build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.scope = scope;
    }

    /**
     * 백엔드 callback URI와 CSRF 방어용 state를 포함한 GitHub OAuth authorize URL을 생성합니다.
     */
    public String createAuthorizationUrl(String state) {
        return UriComponentsBuilder.fromUriString("https://github.com/login/oauth/authorize")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", scope)
                .queryParam("state", state)
                .build()
                .toUriString();
    }

    /**
     * GitHub OAuth authorization code를 GitHub access token으로 교환합니다.
     */
    public GithubAccessTokenResponse requestAccessToken(String code) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("code", code);
        body.add("redirect_uri", redirectUri);

        try {
            GithubAccessTokenResponse response = restClient.post()
                    .uri("https://github.com/login/oauth/access_token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(GithubAccessTokenResponse.class);

            if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
                throw new BusinessException(AuthErrorCode.GITHUB_OAUTH_FAILED);
            }
            return response;
        } catch (RestClientException e) {
            throw new BusinessException(AuthErrorCode.GITHUB_OAUTH_FAILED);
        }
    }

    /**
     * GitHub access token으로 현재 GitHub 사용자 프로필을 조회합니다.
     */
    public GithubUserProfile findUserProfile(String accessToken) {
        try {
            GithubUserProfile profile = restClient.get()
                    .uri("https://api.github.com/user")
                    .headers(headers -> setGithubHeaders(headers, accessToken))
                    .retrieve()
                    .body(GithubUserProfile.class);

            if (profile == null || profile.id() == null || profile.login() == null || profile.login().isBlank()) {
                throw new BusinessException(AuthErrorCode.GITHUB_OAUTH_FAILED);
            }
            return profile;
        } catch (RestClientException e) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_API_ERROR);
        }
    }

    private void setGithubHeaders(HttpHeaders headers, String accessToken) {
        headers.setBearerAuth(accessToken);
        headers.set(HttpHeaders.ACCEPT, "application/vnd.github+json");
        headers.set("X-GitHub-Api-Version", "2022-11-28");
    }
}
