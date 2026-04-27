package com.example.nutriuniv.domain.auth.client;

import com.example.nutriuniv.common.exception.CustomException;
import com.example.nutriuniv.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * OAuthClient 구현체들을 모아두고 provider 문자열로 라우팅하는 팩토리.
 * 새 프로바이더 추가 시 OAuthClient 구현체를 만들어 빈으로 등록하기만 하면 되며,
 * 이 클래스는 수정할 필요가 없다.
 */
@Component
public class OAuthClientFactory {

    private final Map<OAuthProvider, OAuthClient> clientMap;

    public OAuthClientFactory(List<OAuthClient> clients) {
        this.clientMap = clients.stream()
                .collect(Collectors.toMap(OAuthClient::getProvider, Function.identity()));
    }

    public OAuthClient getClient(String provider) {
        OAuthProvider oAuthProvider = OAuthProvider.from(provider);
        OAuthClient client = clientMap.get(oAuthProvider);
        if (client == null) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "지원하지 않는 OAuth 프로바이더입니다: " + provider);
        }
        return client;
    }

    /**
     * provider 문자열이 유효한지만 확인 (클라이언트 인스턴스 불필요할 때 사용)
     */
    public static void validateProvider(String provider) {
        OAuthProvider.from(provider); // 내부에서 잘못된 값이면 CustomException 던짐
    }
}